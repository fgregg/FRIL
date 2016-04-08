#!/bin/bash
cd "`dirname "$0"`"
echo "Working in directory: `pwd`"

if [ "$JAVA_HOME" ]
then
        echo "JAVA_HOME was found to be set"
        JAVA_CMD="$JAVA_HOME/bin/java"
else
        echo "No JAVA_HOME, using default java"
        JAVA_CMD="java"
fi
echo "Using java command: $JAVA_CMD"

$JAVA_CMD -Xmx600M -Xdock:name="FRIL" -Xdock:icon=icons/icon-app-large.png -cp jars/janino.jar:jars/rsyntaxtextarea.jar:jars/libsvm.jar:jars/xercesImpl.jar:jars/xml-apis.jar:jars/join.jar:jars/emory-util-all.jar:jars/opencsv-2.0.jar:jars/poi-3.1-FINAL-20080629.jar:jars/poi-contrib-3.1-FINAL-20080629.jar:poi-scratchpad-3.1-FINAL-20080629.jar:jdbc/* cdc.impl.MainGUI
