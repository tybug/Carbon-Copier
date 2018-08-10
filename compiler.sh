#!/bin/bash
javac -cp "dependencies/*:src/main/java" src/main/java/com/tybug/carboncopier/CarbonCopier.java -d sources/ # Compile to class files
java -cp "./sources:./dependencies/*" com.tybug.carboncopier.CarbonCopier # Run the main class