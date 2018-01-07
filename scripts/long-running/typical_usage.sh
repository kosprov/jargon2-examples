#!/usr/bin/env bash

mvn -f ../../pom.xml clean package;

java -Xms128m -Xmx128m -cp ../../target/jargon2-examples-1.1.0.jar com.kosprov.jargon2.examples.MultiThreadedHashVerifyLoop \
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
    --parallelism 1 \
    | tee typical_usage.out

