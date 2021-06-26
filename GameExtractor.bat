echo off
SET CLASSPATH=.;%CLASSPATH%
cls

echo ==== Running Game Extractor ====
java -Xmx1024m -jar GameExtractor.jar