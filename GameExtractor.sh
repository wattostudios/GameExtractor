#!/bin/bash
export CLASSPATH=".;$CLASSPATH"
echo "==== Running Game Extractor ===="
java -Xmx1024m -jar GameExtractor.jar
