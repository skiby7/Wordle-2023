#!/bin/bash

java -cp "./lib/jackson-annotations-2.13.3.jar:./lib/jackson-core-2.13.3.jar:./lib/jackson-databind-2.13.3.jar:./build/" Client.ClientMain $1
