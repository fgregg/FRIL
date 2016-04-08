echo off
IF DEFINED JAVA_HOME GOTO :HOME_SET 
IF NOT DEFINED JAVA_HOME GOTO :NO_HOME_SET



:HOME_SET
echo JAVA_HOME variable set
SET JAVA_CMD="%JAVA_HOME%\bin\java"
GOTO :START


:NO_HOME_SET
echo JAVA_HOME variable not set
SET JAVA_CMD=java
GOTO :START



:START
echo on
echo Using java commad: %JAVA_CMD%
%JAVA_CMD% -Xmx600M -cp jars\janino.jar;jars\rsyntaxtextarea.jar;jars\libsvm.jar;jars\xercesImpl.jar;jars\xml-apis.jar;jars\join.jar;jars\emory-util-all.jar;jars\opencsv-2.0.jar;jars\poi-3.1-FINAL-20080629.jar;jars\poi-contrib-3.1-FINAL-20080629.jar;poi-scratchpad-3.1-FINAL-20080629.jar;jdbc\* cdc.impl.MainGUI
