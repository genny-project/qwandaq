#!/bin/bash

# check the number of arguments
if [ "$#" -ne 1 ]; then
	echo -e "Branch name for merging must be supplied!\n\nUsage: $0 <branch>" >&2
	exit 1
fi

# Merge latest
git merge $1

# Build docs
./mvnw clean install -DskipTests=false -U

# Update docs directory
rm -rf docs/javadocs
mkdir docs/javadocs
cp -r target/apidocs/ docs/javadocs
