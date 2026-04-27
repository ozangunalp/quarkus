#!/bin/bash
# Find the Kafka version for each active Quarkus branch
# The property name changed from kafka3.version to kafka.version in newer branches

REMOTE="${1:-upstream}"
BRANCHES=("main" "3.33" "3.27" "3.20")
BOM_FILE="bom/application/pom.xml"

printf "%-15s %s\n" "Branch" "Kafka Version"
printf "%-15s %s\n" "------" "-------------"

for branch in "${BRANCHES[@]}"; do
    bom=$(git show "${REMOTE}/${branch}:${BOM_FILE}" 2>/dev/null)
    kafka_version=$(echo "$bom" | sed -n 's/.*<kafka\.version>\([^<]*\)<\/kafka\.version>.*/\1/p')
    if [ -z "$kafka_version" ]; then
        kafka_version=$(echo "$bom" | sed -n 's/.*<kafka3\.version>\([^<]*\)<\/kafka3\.version>.*/\1/p')
    fi
    if [ -z "$kafka_version" ]; then
        kafka_version="not found"
    fi
    printf "%-15s %s\n" "$branch" "$kafka_version"
done
