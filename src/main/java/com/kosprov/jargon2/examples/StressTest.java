package com.kosprov.jargon2.examples;

import org.apache.commons.cli.*;

import java.util.Arrays;
import java.util.LongSummaryStatistics;
import java.util.Random;

import static com.kosprov.jargon2.api.Jargon2.*;

/**
 * Warms up and runs a stress test with the selected settings.
 */
public class StressTest {

    private static Random R = new Random();

    public static void main(String[] args) throws Exception {
        int iterations = 100;
        boolean rawHash = false;
        int saltLength = 16;
        int passwordLength = 32;
        int secretLength = 0;
        int adLength = 0;
        int hashLength = 16;
        Type type = Type.ARGON2id;
        Version version = Version.V13;
        int memoryCost = 4 * 1024;
        int timeCost = 2;
        int parallelism = 2;

        //<editor-fold desc="Read command-line options" defaultstate="collapsed">
        {
            Options options = new Options();

            options.addOption(Option.builder("it").longOpt("iterations").hasArg(true).argName("N").desc("Run N iterations on stress test (default: " + iterations + ")").build());
            options.addOption(Option.builder("ot").longOpt("outputType").hasArg(true).argName("raw|encoded").desc("Output hash type (default: " + (rawHash ? "raw" : "encoded") + ")").build());
            options.addOption(Option.builder("sl").longOpt("saltLength").hasArg(true).argName("N").desc("Number of salt bytes (default: " + saltLength + ")").build());
            options.addOption(Option.builder("pl").longOpt("passwordLength").hasArg(true).argName("N").desc("Number of password bytes (default: " + passwordLength + ")").build());
            options.addOption(Option.builder("kl").longOpt("secretLength").hasArg(true).argName("N").desc("Number of secret bytes (default: " + secretLength + ")").build());
            options.addOption(Option.builder("al").longOpt("adLength").hasArg(true).argName("N").desc("Number of ad bytes (default: " + adLength + ")").build());
            options.addOption(Option.builder("hl").longOpt("hashLength").hasArg(true).argName("N").desc("Number of output hash bytes (default: " + hashLength + ")").build());
            options.addOption(Option.builder("t").longOpt("type").hasArg(true).argName("i|d|id").desc("Argon2 type (default: " + type.getValue().replace("argon2", "") + ")").build());
            options.addOption(Option.builder("v").longOpt("version").hasArg(true).argName("10|13").desc("Argon2 version (default: " + version.name().replace("V", "") + ")").build());
            options.addOption(Option.builder("mc").longOpt("memoryCost").hasArg(true).argName("N").desc("Number of KB of memory used for hash calculation (default: " + memoryCost + ")").build());
            options.addOption(Option.builder("tc").longOpt("timeCost").hasArg(true).argName("N").desc("Number of passes through memory during hash calculation (default: " + timeCost + ")").build());
            options.addOption(Option.builder("p").longOpt("parallelism").hasArg(true).argName("N").desc("Number of lanes/threads used for hash calculation (default: " + parallelism + ")").build());
            options.addOption("h", "Prints this help");

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                System.out.println("Warms up and runs a stress test with the selected settings.");
                HelpFormatter formatter = new HelpFormatter();
                formatter.setOptionComparator(null);
                formatter.printHelp("java -cp path_to_jar " + StressTest.class.getName() + " [options]", options);
                System.exit(0);
            }

            if (cmd.hasOption("it")) {
                iterations = Integer.parseInt(cmd.getOptionValue("it"));
            }

            if (cmd.hasOption("ot")) {
                String val = cmd.getOptionValue("ot");
                if ("raw".equals(val)) {
                    rawHash = true;
                } else if (!"encoded".equals(val)) {
                    throw new IllegalArgumentException("Wrong output type " + val);
                }
            }

            if (cmd.hasOption("sl")) {
                saltLength = Integer.parseInt(cmd.getOptionValue("sl"));
            }

            if (cmd.hasOption("pl")) {
                passwordLength = Integer.parseInt(cmd.getOptionValue("pl"));
            }

            if (cmd.hasOption("kl")) {
                secretLength = Integer.parseInt(cmd.getOptionValue("kl"));
            }

            if (cmd.hasOption("al")) {
                adLength = Integer.parseInt(cmd.getOptionValue("al"));
            }

            if (cmd.hasOption("hl")) {
                hashLength = Integer.parseInt(cmd.getOptionValue("hl"));
            }

            if (cmd.hasOption("t")) {
                String val = cmd.getOptionValue("t");
                if ("i".equals(val)) {
                    type = Type.ARGON2i;
                } else if ("d".equals(val)) {
                    type = Type.ARGON2d;
                } else if (!"id".equals(val)) {
                    throw new IllegalArgumentException("wrong type " + val);
                }
            }

            if (cmd.hasOption("v")) {
                String val = cmd.getOptionValue("v");
                if ("10".equals(val)) {
                    version = Version.V10;
                } else if (!"13".equals(val)) {
                    throw new IllegalArgumentException("wrong version " + val);
                }
            }

            if (cmd.hasOption("mc")) {
                memoryCost = Integer.parseInt(cmd.getOptionValue("mc"));
            }

            if (cmd.hasOption("tc")) {
                timeCost = Integer.parseInt(cmd.getOptionValue("tc"));
            }

            if (cmd.hasOption("p")) {
                parallelism = Integer.parseInt(cmd.getOptionValue("p"));
            }
        }
        //</editor-fold>

        System.out.println("--------------------------------------------------");
        System.out.println("Configuration");
        System.out.println("--------------------------------------------------");
        System.out.printf("Iterations:\t\t%d\n", iterations);
        System.out.printf("Output type:\t\t%s\n", (rawHash ? "raw" : "encoded"));
        System.out.printf("Salt length:\t\t%d bytes\n", saltLength);
        System.out.printf("Password length:\t%d bytes\n", passwordLength);
        System.out.printf("Secret length:\t\t%d bytes\n", secretLength);
        System.out.printf("AD length:\t\t%d bytes\n", adLength);
        System.out.printf("Hash length:\t\t%d bytes\n", hashLength);
        System.out.printf("Type:\t\t\t%s\n", type.getValueCapitalized());
        System.out.printf("Version:\t\t%s\n", version.name().toLowerCase());
        System.out.printf("Memory cost:\t\t%d KB\n", memoryCost);
        System.out.printf("Time cost:\t\t%d passes\n", timeCost);
        System.out.printf("Parallelism:\t\t%d lanes/threads\n", parallelism);
        System.out.println("--------------------------------------------------");

        warmUp(
                rawHash,
                jargon2Hasher().type(type).version(version).memoryCost(8).timeCost(1).parallelism(1),
                jargon2Verifier().type(type).version(version).memoryCost(8).timeCost(1).parallelism(1)
        );

        Hasher hasher = jargon2Hasher()
                .type(type)
                .version(version)
                .hashLength(hashLength)
                .memoryCost(memoryCost)
                .timeCost(timeCost)
                .parallelism(parallelism);

        Verifier verifier = jargon2Verifier()
                .type(type)
                .version(version)
                .memoryCost(memoryCost)
                .timeCost(timeCost)
                .parallelism(parallelism);

        if (secretLength != 0) {
            byte[] secret = new byte[secretLength];
            R.nextBytes(secret);

            hasher = hasher.secret(secret);
            verifier = verifier.secret(secret);
        }

        testHash(iterations, rawHash, adLength, saltLength, passwordLength, hasher, verifier);
    }

    private static void warmUp(boolean raw, Hasher hasher, Verifier verifier) throws Exception {
        System.out.println("Warming up...");
        byte[] password = "a password value".getBytes("UTF-8");
        byte[] salt = "a salt value".getBytes("UTF-8");
        for (int i = 0; i < 5000; i++) {
            boolean match;
            if (raw) {
                match = verifier.hash(hasher.salt(salt).password(password).rawHash()).salt(salt).password(password).verifyRaw();
            } else {
                match = verifier.hash(hasher.password(password).encodedHash()).password(password).verifyEncoded();
            }
            if (!match) {
                throw new IllegalStateException("Could not verify");
            }
        }
        gc();
    }

    private static void testHash(int iterations, boolean raw, int adLength, int saltLength, int passwordLength , Hasher hasher, Verifier verifier) throws Exception {
        System.out.println("Running stress test...");
        System.out.printf("Iterations: %5d, Output type: %s, AD length: %d, Salt length: %d, Password length: %d%n", iterations, (raw ? "raw" : "encoded"), adLength, saltLength, passwordLength);
        System.out.println(hasher.toString());
        System.out.println(verifier.toString());
        System.out.printf("        [%10s    %15s      %14s ]%n", "avg", "min", "max (95%)");

        long[] hashElapsed = new long[iterations];
        long[] verifyElapsed = new long[iterations];
        for (int i = 0; i < iterations; i++) {

            long start;
            Object hash;
            byte[] ad = randomByteArray(adLength);
            byte[] salt = randomByteArray(saltLength);
            byte[] password = randomByteArray(passwordLength);
            {
                start = System.nanoTime();
                hasher = hasher.ad(ad).salt(salt).password(password);
                if (raw) {
                    hash = hasher.rawHash();
                } else {
                    hash = hasher.encodedHash();
                }
                hashElapsed[i] = (System.nanoTime() - start);
            }

            {
                start = System.nanoTime();
                verifier = verifier.ad(ad).password(password);
                boolean match;
                if (raw) {
                    match = verifier.hash((byte[]) hash).salt(salt).verifyRaw();
                } else {
                    match = verifier.hash((String) hash).verifyEncoded();
                }
                verifyElapsed[i] = (System.nanoTime() - start);

                if (!match) {
                    throw new IllegalStateException("Could not verify");
                }
            }
        }

        LongSummaryStatistics hashStats = Arrays.stream(hashElapsed).sorted().limit((long) (hashElapsed.length * 0.95)).summaryStatistics();
        LongSummaryStatistics verifyStats = Arrays.stream(verifyElapsed).sorted().limit((long) (verifyElapsed.length * 0.95)).summaryStatistics();

        System.out.printf("Hash  : [%10.2fms, %15dns, %15dns]%n", toMillis(hashStats.getAverage()), hashStats.getMin(), hashStats.getMax());
        System.out.printf("Verify: [%10.2fms, %15dns, %15dns]%n", toMillis(verifyStats.getAverage()), verifyStats.getMin(), verifyStats.getMax());
        System.out.printf("Total : %dms%n", toMillis(hashStats.getSum() + verifyStats.getSum()));
        System.out.println();
    }

    private static double toMillis(double nanos) {
        return nanos / (1000 * 1000);
    }

    private static long toMillis(long nanos) {
        return nanos / (1000 * 1000);
    }

    private static byte[] randomByteArray(int length) {
        if (length > 0) {
            byte[] bytes = new byte[length];
            R.nextBytes(bytes);
            return bytes;
        } else {
            return null;
        }
    }

    private static void gc() throws Exception {
        System.gc();
        Thread.sleep(1);
    }
}
