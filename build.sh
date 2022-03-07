#!/bin/bash
./mvnw clean install -DskipTests=false -U

# Update docs directory
rm -rf docs
mkdir docs
cp -r target/apidocs/ docs/
