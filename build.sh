#!/bin/bash
./mvnw clean install -DskipTests=false -U

# Update docs directory
rm -rf docs/javadocs
mkdir docs/javadocs
cp -r target/apidocs/ docs/javadocs
