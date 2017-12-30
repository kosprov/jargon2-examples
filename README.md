# Jargon2 Examples: Putting Jargon2 to work 

This repository contains `main` classes with the following examples:

- A command line utility
- Simple stress test to measure the performance of any given configuration
- A multi-threaded test that monitors memory consumption for any given configuration (Linux only)

All examples use the [default Jargon2 backend implementation](https://github.com/kosprov/jargon2-backends "Jargon2 Backends repository") with generic binaries.

## Command-line utility

Class `com.kosprov.jargon2.examples.CommandLineUtility` mimics the command line utility implemented in [Argon2 reference implementation](https://github.com/P-H-C/phc-winner-argon2 "Argon2 reference implementation repository").

This class is the `Main-Class` of the (fat) jar produced, so it can be executed as:

```bash
mvn clean package;
echo -n "password" | java -jar target/jargon2-examples-1.0.1.jar somesalt -t 2 -m 16 -p 4 -l 24
```  

The output is:

```
Type:           Argon2i
Iterations:     2
Memory:         65536 KiB
Parallelism:    4
Hash:           45d7ac72e76f242b20b77b9bf9bf9d5915894e669a24e6c6
Encoded:        $argon2i$v=19$m=65536,t=2,p=4$c29tZXNhbHQ$RdescudvJCsgt3ub+b+dWRWJTmaaJObG
0.134 seconds
Verification ok
```

## Stress test

Class `com.kosprov.jargon2.examples.StressTest` implements a microbenchmark of Jargon2.

```bash
mvn clean package;
java -server -Xms512m -Xmx512m -cp target/jargon2-examples-1.0.1.jar com.kosprov.jargon2.examples.StressTest \
    --iterations 100 \
    --outputType encoded \
    --saltLength 16 \
    --passwordLength 64 \
    --secretLength 32 \
    --adLength 64 \
    --hashLength 16 \
    --type id \
    --version 13 \
    --memoryCost $(bc <<< "32 * 1024") \
    --timeCost 3 \
    --parallelism 1 \
    ;
```

The output is:

```
Output type:            encoded
Salt length:            16 bytes
Password length:        64 bytes
Secret length:          32 bytes
AD length:              64 bytes
Hash length:            16 bytes
Type:                   Argon2id
Version:                v13
Memory cost:            32768 KB
Time cost:              3 passes
Parallelism:            1 lanes/threads
--------------------------------------------------
Warming up...
Running stress test...
Iterations:   100, Output type: encoded, AD length: 64, Salt length: 16, Password length: 64
Hasher{backend=com.kosprov.jargon2.backend.NativeRiJargon2Backend, options=none, type=ARGON2id, version=V13, timeCost=3, memoryCost=32768, lanes=1, threads=1, hashLength=16, saltLength=16}
Verifier{backend=com.kosprov.jargon2.backend.NativeRiJargon2Backend, options=none, type=ARGON2id, version=V13, timeCost=3, memoryCost=32768, lanes=1, threads=1}
        [       avg                min           max (95%) ]
Hash  : [     66.47ms,        64884716ns,        68156945ns]
Verify: [     66.34ms,        65167428ns,        67423742ns]
Total : 12617ms
```

Folder `scripts/stress-tests` contains a few shell scripts for different configurations.

### Long-running tests

Class `com.kosprov.jargon2.examples.MultiThreadedHashVerifyLoop` implements a multi-threaded hash/verify loop while a separate thread measures heap and process memory consumption.

The idea is to be able to run for a significant number of hash/verify operations with different memory requirements (both the memory cost and the data passed) in order to spot any memory leaks or stability issues.

> Memory consumption at the process level is measured with `ps` so it's just an indication, not an accurate measurement.

For example, 4 Java threads executing a hash/verify loop for 2 hours with moderate settings for values and Argon2 configuration would look like:

```bash
mvn clean package;

java -Xms128m -Xmx128m -cp target/jargon2-examples-1.0.1.jar com.kosprov.jargon2.examples.MultiThreadedHashVerifyLoop \
    --runtime 7200 \
    --collectStats 60 \
    --javaThreads 4 \
    --saltLength 16 \
    --passwordLength 12 \
    --secretLength 0 \
    --adLength 0 \
    --hashLength 32 \
    --type id \
    --version 13 \
    --memoryCost 32768 \
    --timeCost 1 \
    --parallelism 1
```

The output is:

```
    --------------------------------------------------
    Configuration
    --------------------------------------------------
    Backend:            automatic
    Runtime:            7200 seconds
    Collect stats:      every 60 seconds
    Java threads:       4
    Salt length:        16 bytes
    Password length:    12 bytes
    Secret length:      0 bytes
    AD length:          0 bytes
    Hash length:        32 bytes
    Type:               Argon2id
    Version:            v13
    Memory cost:        32768 KB
    Time cost:          1 passes
    Parallelism:        1 lanes/threads
    --------------------------------------------------
    [10:49:21] Pid: 21958. Estimated completion before 12:50:21
    
    T,C,%CPU,RSS,S0C,S1C,S0U,S1U,EC,EU,OC,OU,MC,MU,CCSC,CCSU,YGC,YGCT,FGC,FGCT,GCT
    0,0,0.0,32860,5120.0,5120.0,0.0,0.0,33280.0,3994.8,87552.0,0.0,4480.0,773.8,384.0,75.8,0,0.000,0,0.000,0.000
    60284,2822,358,147788,5120.0,5120.0,0.0,0.0,33280.0,31286.6,87552.0,0.0,4480.0,773.8,384.0,75.8,0,0.000,0,0.000,0.000
    120513,5730,361,171928,5120.0,5120.0,0.0,2559.1,33280.0,20782.9,87552.0,8.0,7040.0,6758.5,896.0,745.7,1,0.018,0,0.000,0.018
    180723,8592,362,122136,5120.0,5120.0,2464.0,0.0,33280.0,8997.7,87552.0,16.0,7040.0,6768.4,896.0,745.7,2,0.043,0,0.000,0.043
    ... 2 hours later ...
    7166095,331780,361,182484,2048.0,2048.0,0.0,1664.0,39424.0,32582.2,87552.0,2200.1,7296.0,6869.6,896.0,746.7,63,0.779,0,0.000,0.779
    7226401,333328,360,107384,2560.0,2560.0,0.0,0.0,38400.0,28.2,87552.0,1039.6,7296.0,6864.7,896.0,744.0,67,0.803,3,0.043,0.846
    
    [12:49:47] Executed 333328 hash/verify in 7200s.
```

A plot of some of these metrics is:

![Typical usage](/scripts/long-running/typical_usage.png?raw=true)

Folder `scripts/long-running` contains a few shell scripts for different configurations that could potential expose a stability issue. Also, there are spreadsheets where you can paste the CSV data and reproduce the plot.

