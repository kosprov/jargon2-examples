package com.kosprov.jargon2.examples;

import org.apache.commons.cli.*;

import javax.xml.bind.DatatypeConverter;

import static com.kosprov.jargon2.api.Jargon2.*;

/**
 * This the main class of the jar.
 *
 * <p>
 *     It mimics the functionality of the Argon2 RI command-line utility (https://github.com/P-H-C/phc-winner-argon2/blob/master/README.md).
 * </p>
 *
 * <p>
 *     Usage:
 * </p>
 * <pre>
 *     $ mvn clean package
 *     $ echo -n "password" | java -jar target/jargon2-examples-1.0.1.jar somesalt -t 2 -m 16 -p 4 -l 24
 * </pre>
 * <p>
 *     Outputs:
 * </p>
 * <pre>
 *     Type:           Argon2i
 *     Iterations:     2
 *     Memory:         65536 KiB
 *     Parallelism:    4
 *     Hash:           45d7ac72e76f242b20b77b9bf9bf9d5915894e669a24e6c6
 *     Encoded:        $argon2i$v=19$m=65536,t=2,p=4$c29tZXNhbHQ$RdescudvJCsgt3ub+b+dWRWJTmaaJObG
 *     0.180 seconds
 *     Verification ok
 * </pre>
 */
public class CommandLineUtility {
    public static void main(String[] args) throws Exception {

        // Defaults
        Type type = Type.ARGON2i;
        Version version = Version.V13;
        int timeCost = 3;
        int memoryCost = 1 << 12;
        int parallelism = 1;
        int hashLength = 32;
        boolean encodedOnly = false;
        boolean rawOnly = false;

        //<editor-fold desc="Read command-line options" defaultstate="collapsed">
        {
            Options options = new Options();

            options.addOption("i", "Use Argon2i (this is the default)");
            options.addOption("d", "Use Argon2d instead of Argon2i");
            options.addOption("id", "Use Argon2id instead of Argon2i");
            options.addOption(Option.builder("t").hasArg(true).argName("N").desc("Sets the number of iterations to N (default = 3)").build());
            options.addOption(Option.builder("m").hasArg(true).argName("N").desc("Sets the memory usage of 2^N KiB (default 12)").build());
            options.addOption(Option.builder("p").hasArg(true).argName("N").desc("Sets parallelism to N threads (default 1)").build());
            options.addOption(Option.builder("l").hasArg(true).argName("N").desc("Sets hash output length to N bytes (default 32)").build());
            options.addOption("e", "Output only encoded hash");
            options.addOption("r", "Output only the raw bytes of the hash");
            options.addOption(Option.builder("v").hasArg(true).argName("10|13").desc("Argon2 version (defaults to the most recent version, currently 13)").build());
            options.addOption("h", "Print argon2 usage");

            if (args.length == 0) { // Salt is required as first program argument
                exit(options);
            }

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                exit(options);
            }

            if (cmd.hasOption("id")) {
                type = Type.ARGON2id;
            } else if (cmd.hasOption("d")) {
                type = Type.ARGON2d;
            }

            if (cmd.hasOption("v")) {
                String versionStr = cmd.getOptionValue("v");
                if ("10".equals(versionStr)) {
                    version = Version.V10;
                } else if (!"13".equals(versionStr)) {
                    exit(options, "Wrong version: " + versionStr);
                }
            }

            if (cmd.hasOption("t")) {
                String timeCostStr = cmd.getOptionValue("t");
                try {
                    timeCost = Integer.parseInt(timeCostStr);
                } catch (NumberFormatException e) {
                    exit(options, "Wrong time cost: " + timeCostStr);
                }
            }

            if (cmd.hasOption("m")) {
                String memoryCostStr = cmd.getOptionValue("m");
                try {
                    memoryCost = 1 << Integer.parseInt(memoryCostStr);
                } catch (NumberFormatException e) {
                    exit(options, "Wrong memory cost: " + memoryCostStr);
                }
            }

            if (cmd.hasOption("p")) {
                String parallelismStr = cmd.getOptionValue("p");
                try {
                    parallelism = Integer.parseInt(parallelismStr);
                } catch (NumberFormatException e) {
                    exit(options, "Wrong parallelism: " + parallelismStr);
                }
            }

            if (cmd.hasOption("l")) {
                String hashLengthStr = cmd.getOptionValue("l");
                try {
                    hashLength = Integer.parseInt(hashLengthStr);
                } catch (NumberFormatException e) {
                    exit(options, "Wrong hash length: " + hashLengthStr);
                }
            }

            if (cmd.hasOption("e")) {
                encodedOnly = true;
            } else if (cmd.hasOption("r")) {
                rawOnly = true;
            }
        }
        //</editor-fold>

        boolean verbose = !encodedOnly && !rawOnly;

        if (verbose) {
            System.out.printf("Type:\t\t%s\n", type.getValueCapitalized());
            System.out.printf("Iterations:\t%d\n", timeCost);
            System.out.printf("Memory:\t\t%d KiB\n", memoryCost);
            System.out.printf("Parallelism:\t%d\n", parallelism);
        }

        long start = System.currentTimeMillis();

        Hasher hasher = jargon2Hasher()
                .type(type)
                .version(version)
                .timeCost(timeCost)
                .memoryCost(memoryCost)
                .parallelism(parallelism)
                .hashLength(hashLength);

        Verifier verifier = jargon2Verifier();

        String rawHashHex = null;
        String encodedHash = null;

        try (ByteArray salt = toByteArray(args[0]);
             ByteArray password = toByteArray(System.in)) {

            if (verbose || rawOnly) {
                byte[] rawHash = hasher.salt(salt).password(password).rawHash();
                rawHashHex = DatatypeConverter.printHexBinary(rawHash).toLowerCase();
            }

            if (verbose || encodedOnly) {
                encodedHash = hasher.salt(salt).password(password).encodedHash();
            }

            if (rawHashHex != null && rawOnly) {
                System.out.println(rawHashHex);
            } else if (rawHashHex != null) {
                System.out.println("Hash:\t\t" + rawHashHex);
            }

            if (encodedHash != null && encodedOnly) {
                System.out.println(encodedHash);
            } else if (encodedHash != null) {
                System.out.println("Encoded:\t" + encodedHash);
            }

            if (verbose) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.printf("%2.3f seconds\n", ((double) elapsed / 1000));

                boolean verificationOk = verifier.hash(encodedHash).password(password).verifyEncoded();
                if (verificationOk) {
                    System.out.println("Verification ok");
                } else {
                    throw new RuntimeException("Verification failed.");
                }
            }
        }
    }

    private static void exit(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);
        formatter.printHelp( "java -jar path_to_jar salt [options]", options);
        System.exit(-1);
    }

    private static void exit(Options options, String message) {
        if (message != null) {
            System.out.println(message);
        }
        exit(options);
    }
}
