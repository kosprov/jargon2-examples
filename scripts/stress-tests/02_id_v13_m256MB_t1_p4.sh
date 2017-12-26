#!/usr/bin/env bash

mvn -f ../../pom.xml clean package;
java -server -Xms512m -Xmx512m -cp ../../target/jargon2-examples-1.0.0.jar com.kosprov.jargon2.examples.StressTest \
    --iterations 100 \
    --outputType encoded \
    --saltLength 16 \
    --passwordLength 64 \
    --secretLength 32 \
    --adLength 64 \
    --hashLength 16 \
    --type id \
    --version 13 \
    --memoryCost $(bc <<< "256 * 1024") \
    --timeCost 1 \
    --parallelism 4 \
    ;

