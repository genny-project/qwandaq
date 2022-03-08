#!/bin/bash

# Merge latest
git merge $1

# Build docs
./mvnw clean install -DskipTests=false -U

# Update docs directory
rm -rf docs/javadocs
mkdir docs/javadocs
cp -r target/apidocs/ docs/javadocs
