#!/bin/bash
./mvnw clean install -DskipTests=false -U

# Update docs directory
rm -rf docs/apidocs/
cp -r target/apidocs/ docs/apidocs
