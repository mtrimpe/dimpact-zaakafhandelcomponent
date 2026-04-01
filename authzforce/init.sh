#!/bin/sh
# Initialize AuthzForce CE: create domain, upload XACML policies, enable MDP, set root policy.
set -e

AZ_URL="${AUTHZFORCE_URL:-http://authzforce:8080/authzforce-ce}"
POLICY_FILE="${POLICY_FILE:-/policies/zac-policies.xml}"

echo "Waiting for AuthzForce at $AZ_URL..."
i=0
while [ "$i" -lt 60 ]; do
    if curl -sf "$AZ_URL/domains" -H "Accept: application/xml" > /dev/null 2>&1; then
        echo "AuthzForce is ready"
        break
    fi
    i=$((i + 1))
    [ "$i" -eq 60 ] && echo "AuthzForce not ready after 60s" && exit 1
    sleep 1
done

# Create domain
echo "Creating domain..."
DOMAIN_RESP=$(curl -sf -X POST -H "Accept: application/xml" -H "Content-Type: application/xml" \
  -d '<?xml version="1.0" encoding="UTF-8"?><taz:domainProperties xmlns:taz="http://authzforce.github.io/rest-api-model/xmlns/authz/5" />' \
  "$AZ_URL/domains")
DOMAIN_ID=$(echo "$DOMAIN_RESP" | sed -n 's/.*href="\([^"]*\)".*/\1/p')
echo "Domain ID: $DOMAIN_ID"

# Enable MDP (Multiple Decision Profile) request preprocessor
echo "Enabling MDP..."
# Get current PDP properties, enable the MDP preprocessor
curl -sf -X PUT -H "Content-Type: application/xml" -H "Accept: application/xml" \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<pdpPropertiesUpdate xmlns="http://authzforce.github.io/rest-api-model/xmlns/authz/5">
  <feature type="urn:ow2:authzforce:feature-type:pdp:request-preproc" enabled="true">urn:ow2:authzforce:feature:pdp:request-preproc:xacml-json:multiple:repeated-attribute-categories-lax</feature>
  <rootPolicyRefExpression>root</rootPolicyRefExpression>
</pdpPropertiesUpdate>' \
  "$AZ_URL/domains/$DOMAIN_ID/pap/pdp.properties" > /dev/null

# Upload policies
echo "Uploading policies from $POLICY_FILE..."
curl -sf -X POST -H "Content-Type: application/xml" -H "Accept: application/xml" \
  -d @"$POLICY_FILE" \
  "$AZ_URL/domains/$DOMAIN_ID/pap/policies" > /dev/null

# Set as root policy
echo "Setting root policy..."
curl -sf -X PUT -H "Content-Type: application/xml" -H "Accept: application/xml" \
  -d '<?xml version="1.0" encoding="UTF-8"?>
<pdpPropertiesUpdate xmlns="http://authzforce.github.io/rest-api-model/xmlns/authz/5">
  <feature type="urn:ow2:authzforce:feature-type:pdp:request-preproc" enabled="true">urn:ow2:authzforce:feature:pdp:request-preproc:xacml-json:multiple:repeated-attribute-categories-lax</feature>
  <rootPolicyRefExpression>zac-policies</rootPolicyRefExpression>
</pdpPropertiesUpdate>' \
  "$AZ_URL/domains/$DOMAIN_ID/pap/pdp.properties" > /dev/null

# Write domain ID to a file for the PDP URL
echo "$DOMAIN_ID" > /tmp/authzforce-domain-id
echo "AuthzForce setup complete. Domain: $DOMAIN_ID, PDP: $AZ_URL/domains/$DOMAIN_ID/pdp"
