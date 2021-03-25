# Jargon2 Examples: Putting Jargon2 to work 

This repository contains a command line utility that uses the [default Jargon2 backend implementation](https://github.com/kosprov/jargon2-backends "Jargon2 Backends repository") with generic binaries.

Class `com.kosprov.jargon2.examples.CommandLineUtility` mimics the command line utility implemented in [Argon2 reference implementation](https://github.com/P-H-C/phc-winner-argon2 "Argon2 reference implementation repository").

This class is the `Main-Class` of the (fat) jar produced, so it can be executed as:

```bash
mvn clean package
echo -n "password" | java -jar target/jargon2-examples-1.1.1.jar somesalt -t 2 -m 16 -p 4 -l 24
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
