#!/usr/bin/env bash

mvn -f ../../pom.xml clean package;

java -Xms16m -Xmx16m -cp ../../target/jargon2-examples-1.1.1.jar com.kosprov.jargon2.examples.MultiThreadedHashVerifyLoop \
    --backend com.kosprov.jargon2.examples.MultiThreadedHashVerifyLoop\$DummyBackend \
    --runtime 7200 \
    --collectStats 60 \
    --javaThreads 4 \
    --saltLength 8 \
    --passwordLength 8 \
    --secretLength 8 \
    --adLength 8 \
    --hashLength 8 \
    --type id \
    --version 13 \
    --memoryCost 16 \
    --timeCost 2 \
    --parallelism 2 \
    | tee may_dummy_hashes.out

