#!/bin/bash

find . -name "*.java" > ./files.txt
javac -d "./build" -cp "./lib/jackson-core-2.13.3.jar:./lib/jackson-annotations-2.13.3.jar:./lib/jackson-databind-2.13.3.jar:./lib/sqlite-jdbc-3.40.0.0.jar" @files.txt
