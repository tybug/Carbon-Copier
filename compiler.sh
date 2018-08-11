#!/bin/bash
git pull
javac -cp "dependencies/*:src/main/java" src/main/java/com/tybug/carboncopier/CarbonCopier.java -d sources/ # Compile to class files
java -cp "./sources:./dependencies/*" com.tybug.carboncopier.CarbonCopier &>> log.txt # Run the main class, redirect stdout and stderr to log.txt