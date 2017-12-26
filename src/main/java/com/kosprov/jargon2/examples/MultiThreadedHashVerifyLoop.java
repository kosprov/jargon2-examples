package com.kosprov.jargon2.examples;

import com.kosprov.jargon2.api.Jargon2Exception;
import com.kosprov.jargon2.spi.Jargon2Backend;
import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static com.kosprov.jargon2.api.Jargon2.*;

/**
 * Runs a multi-threaded Java hash / verify loop with random ad, salt and password on every iteration.
 *
 * <p>
 *     On Unix systems, it is able to collect process and JVM memory statistics (semi-accurately) to spot
 *     possible memory leaks. Folder <tt>scripts/long-running</tt> has examples of various executions.
 * </p>
 */
public class MultiThreadedHashVerifyLoop {

    public static void main(String[] args) throws Exception {

        Class<? extends Jargon2Backend> backend = null; // discovered
        long runtime = 300 * 1000;
        boolean collectStats = false;
        long statsSamplingPeriod = 0;
        int javaThreads = 4;
        int saltLength = 16;
        int passwordLength = 32;
        int secretLength = 16;
        int adLength = 32;
        int hashLength = 16;
        Type type = Type.ARGON2id;
        Version version = Version.V13;
        int memoryCost = 4 * 1024;
        int timeCost = 2;
        int parallelism = 2;

        //<editor-fold desc="Read command-line options" defaultstate="collapsed">
        {
            Options options = new Options();

            options.addOption(Option.builder("b").longOpt("backend").hasArg(true).argName("class").desc("Class name of the Argon2 backend (default: automatic)").build());
            options.addOption(Option.builder("rt").longOpt("runtime").hasArg(true).argName("N").desc("Run for N seconds (default: " + (runtime / 1000) + ")").build());
            options.addOption(Option.builder("cs").longOpt("collectStats").hasArg(true).argName("N").desc("Sample statistics every N seconds (no stats by default - works only on UNIX systems)").build());
            options.addOption(Option.builder("jt").longOpt("javaThreads").hasArg(true).argName("N").desc("Number of Java threads (default: " + javaThreads + ")").build());
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
                System.out.println("Runs a multi-threaded Java hash / verify loop with random ad, salt and password on every iteration.");
                HelpFormatter formatter = new HelpFormatter();
                formatter.setOptionComparator(null);
                formatter.printHelp("java -cp path_to_jar " + MultiThreadedHashVerifyLoop.class.getName() + " [options]", options);
                System.exit(0);
            }

            if (cmd.hasOption("b")) {
                backend = Class.forName(cmd.getOptionValue("b")).asSubclass(Jargon2Backend.class);
            }

            if (cmd.hasOption("rt")) {
                runtime = Long.parseLong(cmd.getOptionValue("rt")) * 1000;
            }

            if (cmd.hasOption("cs")) {
                collectStats = true;
                statsSamplingPeriod = Long.parseLong(cmd.getOptionValue("cs")) * 1000;
            }

            if (cmd.hasOption("jt")) {
                javaThreads = Integer.parseInt(cmd.getOptionValue("jt"));
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

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");

        System.out.println("--------------------------------------------------");
        System.out.println("Configuration");
        System.out.println("--------------------------------------------------");
        System.out.printf("Backend:\t\t%s\n", (backend != null ? backend.getName() : "automatic"));
        System.out.printf("Runtime:\t\t%d seconds\n", runtime / 1000);
        System.out.printf("Collect stats:\t\t%s\n", (collectStats ? "every " + (statsSamplingPeriod / 1000) + " seconds" : "no"));
        System.out.printf("Java threads:\t\t%d\n", javaThreads);
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

        Date completion = new Date(System.currentTimeMillis() + runtime + statsSamplingPeriod);
        System.out.printf("[%s] Pid: %s. Estimated completion before %s%n%n", sdf.format(new Date()), getProcessId(), sdf.format(completion));

        AtomicLong hashCounter = new AtomicLong();

        Random r = new Random();

        byte[] secret = null;
        if (secretLength > 0) {
            secret = new byte[secretLength];
            r.nextBytes(secret);
        }

        CountDownLatch firstStatLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(javaThreads + 1);

        StatsCollector statsCollector = new StatsCollector(hashCounter, firstStatLatch, completionLatch, statsSamplingPeriod);

        if (collectStats) {
            Thread statsThread = new Thread(statsCollector);
            statsThread.setName("StatsCollector");
            statsThread.start();
        } else {
            firstStatLatch.countDown();
            completionLatch.countDown();
        }

        firstStatLatch.await(); // wait for first stat to be measured before other threads execute

        HashVerifyLoop[] loops = new HashVerifyLoop[javaThreads];

        Hasher hasher = (backend != null ? jargon2Hasher().backend(backend) : jargon2Hasher())
                .type(type)
                .version(version)
                .memoryCost(memoryCost)
                .timeCost(timeCost)
                .parallelism(parallelism)
                .hashLength(hashLength)
                .secret(secret);

        Verifier verifier = (backend != null ? jargon2Verifier().backend(backend) : jargon2Verifier())
                .secret(secret);

        for (int i = 0; i < javaThreads; i++) {
            loops[i] = new HashVerifyLoop(
                    hashCounter,
                    completionLatch,
                    adLength,
                    saltLength,
                    passwordLength,
                    (ad, salt, password) -> hasher.ad(ad).salt(salt).password(password).encodedHash(),
                    (hash, ad, password) -> verifier.hash(hash).ad(ad).password(password).verifyEncoded()
            );
            Thread hashVerifyThread = new Thread(loops[i]);
            hashVerifyThread.setName("HashVerifyLoop-" + i);
            hashVerifyThread.start();
        }

        Thread.sleep(runtime);

        for (HashVerifyLoop runnable : loops) {
            runnable.stop();
        }

        statsCollector.stop();

        completionLatch.await();

        long total = hashCounter.get();

        System.out.printf("%n[%s] Executed %d hash/verify in %ds.", sdf.format(new Date()), total,  (runtime / 1000));
    }

    @FunctionalInterface
    interface Hash {
        String hash(byte[] ad, byte[] salt, ByteArray password);
    }

    @FunctionalInterface
    interface Verify {
        boolean verify(String hash, byte[] ad, ByteArray password);
    }

    static class HashVerifyLoop implements Runnable {
        private AtomicLong hashCounter;
        private CountDownLatch latch;
        private int adLength;
        private int saltLength;
        private int passwordLength;
        private Hash hasher;
        private Verify verifier;
        private volatile boolean active = true;
        private Random r = new Random();

        HashVerifyLoop(AtomicLong hashCounter, CountDownLatch latch, int adLength, int saltLength, int passwordLength, Hash hasher, Verify verifier) {
            this.hashCounter = hashCounter;
            this.latch = latch;
            this.adLength = adLength;
            this.saltLength = saltLength;
            this.passwordLength = passwordLength;
            this.hasher = hasher;
            this.verifier = verifier;
        }

        @Override
        public void run() {
            while (active) {
                byte[] ad = null;
                if (adLength > 0) {
                    ad = new byte[adLength];
                    r.nextBytes(ad);
                }

                byte[] salt = new byte[saltLength];
                r.nextBytes(salt);

                byte[] password = new byte[passwordLength];
                r.nextBytes(password);

                boolean match;

                try (ByteArray passwordByteArray = toByteArray(password).clearSource()) {

                    String encodedHash = hasher.hash(ad, salt, passwordByteArray);
                    match = verifier.verify(encodedHash, ad, passwordByteArray);

                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }

                if (!match) {
                    throw new IllegalStateException("Not matched");
                }

                hashCounter.addAndGet(2);
            }
            latch.countDown();
        }

        void stop() {
            active = false;
        }
    }

    static class StatsCollector implements Runnable {
        private AtomicLong hashCounter;
        private CountDownLatch firstStatLatch;
        private CountDownLatch completionLatch;
        private volatile boolean active = true;
        private long samplingPeriod;

        StatsCollector(AtomicLong hashCounter, CountDownLatch firstStatLatch, CountDownLatch completionLatch, long samplingPeriod) {
            this.hashCounter = hashCounter;
            this.firstStatLatch = firstStatLatch;
            this.completionLatch = completionLatch;
            this.samplingPeriod = samplingPeriod;
        }

        @Override
        public void run() {
            long start = System.currentTimeMillis();

            sample(true, 0);
            firstStatLatch.countDown();
            sleep(samplingPeriod);

            while (active) {
                sample(false, System.currentTimeMillis() - start);
                sleep(samplingPeriod);
            }

            System.gc(); sleep(10); System.gc(); sleep(10); System.gc(); sleep(10);

            sample(false, System.currentTimeMillis() - start);

            completionLatch.countDown();
        }

        void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        void sample(boolean showHeader, long time) {
            try {
                String pid = getProcessId();

                Runtime runtime = Runtime.getRuntime();

                Process jstat = runtime.exec(new String[] { "jstat", "-gc", pid });
                jstat.waitFor();
                BufferedReader jstatReader = new BufferedReader(new InputStreamReader(jstat.getInputStream()));
                String jstatHeader = jstatReader.readLine();
                String jstatValues = jstatReader.readLine();

                Process ps = runtime.exec(new String[] { "ps", "-p", pid, "-o", "%cpu,rss" });
                ps.waitFor();
                BufferedReader psReader = new BufferedReader(new InputStreamReader(ps.getInputStream()));
                String psHeader = psReader.readLine();
                String psValues = psReader.readLine();

                if (showHeader) {
                    String header = "T C " + psHeader + " " + jstatHeader;
                    System.out.println(header.trim().replaceAll(" +", ","));
                }

                String values = time + " " + hashCounter.get() + " " + psValues + " " + jstatValues;
                System.out.println(values.trim().replaceAll(" +", ","));

                jstatReader.close();
                psReader.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void stop() {
            active = false;
        }

    }

    static String getProcessId() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String[] parts = name.split("@");

        long pid;
        if (parts.length < 2 || (pid = Long.parseLong(parts[0])) <= 1) {
            throw new IllegalStateException("Could not get process id");
        }

        return String.valueOf(pid);
    }

    @SuppressWarnings("unused")
    public static class DummyBackend implements Jargon2Backend {
        @Override
        public byte[] rawHash(Type type, Version version, int memoryCost, int timeCost, int lanes, int threads, int hashLength, byte[] secret, byte[] ad, byte[] salt, byte[] password, Map<String, Object> options) throws Jargon2Exception {
            return new byte[0];
        }

        @Override
        public String encodedHash(Type type, Version version, int memoryCost, int timeCost, int lanes, int threads, int hashLength, byte[] secret, byte[] ad, byte[] salt, byte[] password, Map<String, Object> options) throws Jargon2Exception {
            return "";
        }

        @Override
        public boolean verifyRaw(Type type, Version version, int memoryCost, int timeCost, int lanes, int threads, byte[] rawHash, byte[] secret, byte[] ad, byte[] salt, byte[] password, Map<String, Object> options) throws Jargon2Exception {
            return true;
        }

        @Override
        public boolean verifyEncoded(String encodedHash, int threads, byte[] secret, byte[] ad, byte[] password, Map<String, Object> options) throws Jargon2Exception {
            return true;
        }
    }
}
