#!/bin/bash

echo
echo
echo "This script will copy and prepare src of FRIL for release"

echo "Removing current src repository from local directory..."
rm -r src

echo "Copying new source repository..."
cp -r ~/Documents/workspace/CDC/src .

echo "Removing source files that should not be in the release..."
rm -r src/linkage
rm -r src/cdc/impl/schemamatching

echo "Removing MacOS specific files (.DS_Store)..."
find ./src -name .DS_Store -exec rm {} \;

echo "All done. Ready to build."
echo
