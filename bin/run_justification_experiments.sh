#!/bin/sh

MAIN_CLASS=org.liveontologies.pinpointing.RunJustificationExperiments

POM="$(dirname "$(cd "$(dirname "$0")" && pwd)")/pom.xml"

mvn -f $POM exec:java -Dexec.mainClass=$MAIN_CLASS -Dexec.args="$*"
