#!/usr/bin/env bash

mvn -f ../../pom.xml clean package;

java -Xms2048m -Xmx2048m -cp ../../target/jargon2-examples-1.1.1.jar com.kosprov.jargon2.examples.MultiThreadedHashVerifyLoop \
    --runtime 7200 \
    --collectStats 60 \
    --javaThreads 4 \
    --saltLength 4194304 \
    --passwordLength 4194304 \
    --secretLength 4194304 \
    --adLength 4194304 \
    --hashLength 4194304 \
    --type id \
    --version 13 \
    --memoryCost 4096 \
    --timeCost 2 \
    --parallelism 2 \
    | tee large_values.out

