#!/bin/bash
# Bump the Kafka version to .2 patch release on each active Quarkus branch.
# Creates a topic branch per Quarkus branch with the version change committed.
#
# Usage: ./bump-kafka-versions.sh [remote]   (default: upstream)

set -euo pipefail

REMOTE="${1:-upstream}"
BRANCHES=("main" "3.34" "3.33" "3.27" "3.20")
BOM_FILE="bom/application/pom.xml"
PATCH="2"

# Bail out if the working tree is dirty
if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "ERROR: Working tree has uncommitted changes. Commit or stash them first."
    exit 1
fi

ORIGINAL_REF=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || git rev-parse HEAD)

# Make sure we have latest refs
git fetch "$REMOTE"

for branch in "${BRANCHES[@]}"; do
    echo "=== Processing $branch ==="

    bom=$(git show "${REMOTE}/${branch}:${BOM_FILE}")

    # Detect property name and current version
    prop_name=""
    current_version=""
    for prop in "kafka.version" "kafka3.version"; do
        current_version=$(echo "$bom" | sed -n "s/.*<${prop}>\([^<]*\)<\/${prop}>.*/\1/p")
        if [ -n "$current_version" ]; then
            prop_name="$prop"
            break
        fi
    done

    if [ -z "$current_version" ]; then
        echo "  SKIP: Could not find Kafka version property on ${branch}"
        continue
    fi

    # Compute .2 patch version (replace last segment)
    new_version=$(echo "$current_version" | sed "s/\.[0-9]*$/\.${PATCH}/")

    if [ "$current_version" = "$new_version" ]; then
        echo "  SKIP: Already at $current_version"
        continue
    fi

    echo "  $prop_name: $current_version -> $new_version"

    topic_branch="kafka-${new_version}-${branch}"

    # Create topic branch from the upstream branch
    git checkout -B "$topic_branch" "${REMOTE}/${branch}"

    # Update the version in the BOM
    sed -i.bak "s|<${prop_name}>${current_version}</${prop_name}>|<${prop_name}>${new_version}</${prop_name}>|" "$BOM_FILE"
    rm -f "${BOM_FILE}.bak"

    # Commit
    git add "$BOM_FILE"
    git commit -m "Bump Kafka to ${new_version}"

    echo "  Created branch: $topic_branch"
    echo ""
done

# Return to original branch
git checkout "$ORIGINAL_REF"

echo "Done."
