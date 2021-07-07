#!/bin/csh

if (! ${?CLASSPATH} ) then 
  setenv CLASSPATH "."
else 
  setenv CLASSPATH ".;$CLASSPATH"
endif

echo "==== Running Game Extractor ===="
java -Xmx1024m -jar GameExtractor.jar
