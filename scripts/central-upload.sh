#!/bin/sh
# Uploads a Maven Central bundle via the Central Publisher API and waits for validation.
#
# Usage: central-upload.sh <bundle.zip>
# Env:   CENTRAL_TOKEN_USERNAME, CENTRAL_TOKEN_PASSWORD - Central Portal user token
#        CENTRAL_PUBLISHING_TYPE - USER_MANAGED (default) or AUTOMATIC
#        CENTRAL_TIMEOUT_SECONDS - how long to wait for validation (default 1200)
set -eu

API="https://central.sonatype.com/api/v1/publisher"
BUNDLE="${1:?usage: central-upload.sh <bundle.zip>}"
PUBLISHING_TYPE="${CENTRAL_PUBLISHING_TYPE:-USER_MANAGED}"
TIMEOUT="${CENTRAL_TIMEOUT_SECONDS:-1200}"
POLL_INTERVAL=10

: "${CENTRAL_TOKEN_USERNAME:?CENTRAL_TOKEN_USERNAME is not set}"
: "${CENTRAL_TOKEN_PASSWORD:?CENTRAL_TOKEN_PASSWORD is not set}"
[ -f "$BUNDLE" ] || { echo "Bundle not found: $BUNDLE" >&2; exit 1; }

AUTH=$(printf '%s:%s' "$CENTRAL_TOKEN_USERNAME" "$CENTRAL_TOKEN_PASSWORD" | base64 | tr -d '\n')
NAME=$(basename "$BUNDLE" .zip)

echo "Uploading $BUNDLE as '$NAME' (publishingType=$PUBLISHING_TYPE)"
DEPLOYMENT_ID=$(curl --silent --show-error --fail-with-body \
    -X POST "$API/upload?name=$NAME&publishingType=$PUBLISHING_TYPE" \
    -H "Authorization: Bearer $AUTH" \
    -F "bundle=@$BUNDLE") || { echo "Upload failed: $DEPLOYMENT_ID" >&2; exit 1; }
echo "Deployment id: $DEPLOYMENT_ID"

elapsed=0
while :; do
    STATUS=$(curl --silent --show-error --fail-with-body \
        -X POST "$API/status?id=$DEPLOYMENT_ID" \
        -H "Authorization: Bearer $AUTH") || { echo "Status request failed: $STATUS" >&2; exit 1; }
    STATE=$(printf '%s' "$STATUS" | tr -d '\n' | sed -n 's/.*"deploymentState"[[:space:]]*:[[:space:]]*"\([A-Z_]*\)".*/\1/p')
    echo "State: ${STATE:-unknown}"

    case "$STATE" in
        FAILED)
            echo "Deployment failed:" >&2
            printf '%s\n' "$STATUS" >&2
            exit 1
            ;;
        VALIDATED)
            if [ "$PUBLISHING_TYPE" = "USER_MANAGED" ]; then
                echo "Validated. Review and publish at https://central.sonatype.com/publishing/deployments"
                exit 0
            fi
            ;;
        PUBLISHED)
            echo "Published."
            exit 0
            ;;
    esac

    if [ "$elapsed" -ge "$TIMEOUT" ]; then
        echo "Timed out after ${TIMEOUT}s waiting for deployment $DEPLOYMENT_ID" >&2
        printf '%s\n' "$STATUS" >&2
        exit 1
    fi
    sleep "$POLL_INTERVAL"
    elapsed=$((elapsed + POLL_INTERVAL))
done
