#!/bin/bash

java -cp "./lib/jackson-annotations-2.13.3.jar:./lib/jackson-core-2.13.3.jar:./lib/jackson-databind-2.13.3.jar:./lib/sqlite-jdbc-3.40.0.0.jar:./build/" Server.ServerMain $1