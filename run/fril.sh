#!/bin/bash
java -Xmx400M -Xdock:name="FRIL" -Xdock:icon=icons/icon-app-large.png -cp jars/xercesImpl.jar:jars/xml-apis.jar:jars/join.jar:jars/emory-util-all.jar:jars/opencsv-1.8.jar:jars/jtds-1.2.2.jar:jars/poi-3.1-FINAL-20080629.jar:jars/poi-contrib-3.1-FINAL-20080629.jar:poi-scratchpad-3.1-FINAL-20080629.jar cdc.impl.MainGUI
