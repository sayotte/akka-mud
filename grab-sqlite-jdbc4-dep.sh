#!/bin/bash
wget https://bitbucket.org/xerial/sqlite-jdbc/downloads/sqlite-jdbc4-3.8.2-SNAPSHOT.jar
mvn install:install-file -Dfile=sqlite-jdbc4-3.8.2-SNAPSHOT.jar -DgroupId=org.xerial -DartifactId=sqlite-jdbc4 -Dversion=3.8.2-SNAPSHOT -Dpackaging=jar
