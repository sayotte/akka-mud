#!/bin/bash
set -x
set -e
for file in *.java; do
	javac -cp 'scala-2.11.2/lib/scala-library.jar:akka-2.3.4/lib/akka/*:sqlite-jdbc-3.7.15-M1.jar:.' $file
done
