#!/usr/bin/env bash

mvn -f ../../pom.xml clean package;
java -server -Xms512m -Xmx512m -cp ../../target/jargon2-examples-1.0.1.jar com.kosprov.jargon2.examples.StressTest \
    --iterations 100 \
    --outputType encoded \
    --saltLength 16 \
    --passwordLength 64 \
    --secretLength 0 \
    --adLength 0 \
    --hashLength 16 \
    --type id \
    --version 13 \
    --memoryCost $(bc <<< "4 * 1024 * 1024") \
    --timeCost 1 \
    --parallelism 8 \
    ;

