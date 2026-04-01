#!/bin/sh
# Load SpiceDB schema and seed user-to-role relationships for integration tests.
set -e

SPICEDB_HTTP_URL="${SPICEDB_HTTP_URL:-http://spicedb:8090}"
SPICEDB_TOKEN="${SPICEDB_TOKEN:-test}"
SCHEMA_FILE="${SCHEMA_FILE:-/scripts/schema.zed}"

echo "Waiting for SpiceDB at $SPICEDB_HTTP_URL..."
i=0
while [ "$i" -lt 60 ]; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
        -H "Authorization: Bearer $SPICEDB_TOKEN" \
        -H "Content-Type: application/json" \
        -d '{}' "$SPICEDB_HTTP_URL/v1/schema/read" 2>/dev/null || echo "000")
    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "404" ]; then
        echo "SpiceDB is ready"
        break
    fi
    i=$((i + 1))
    [ "$i" -eq 60 ] && echo "SpiceDB not ready after 60s" && exit 1
    sleep 1
done

# Load schema — escape the .zed file content for JSON
echo "Loading schema..."
# Use sed to escape backslashes, quotes, and newlines for JSON string embedding
SCHEMA_JSON=$(sed 's/\\/\\\\/g; s/"/\\"/g' "$SCHEMA_FILE" | tr '\n' '\\' | sed 's/\\/\\n/g')
curl -sf -X POST "$SPICEDB_HTTP_URL/v1/schema/write" \
    -H "Authorization: Bearer $SPICEDB_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"schema\": \"$SCHEMA_JSON\"}" > /dev/null
echo "Schema loaded"

# Load relationships
echo "Loading relationships..."

write_rel() {
    rt=$1; rid=$2; rel=$3; st=$4; sid=$5; srel=${6:-}; caveat=${7:-}
    subj="\"object\":{\"objectType\":\"$st\",\"objectId\":\"$sid\"}"
    if [ -n "$srel" ]; then
        subj="$subj,\"optionalRelation\":\"$srel\""
    fi
    r="{\"resource\":{\"objectType\":\"$rt\",\"objectId\":\"$rid\"},\"relation\":\"$rel\",\"subject\":{$subj}"
    if [ -n "$caveat" ]; then
        r="$r,\"optionalCaveat\":{\"caveatName\":\"$caveat\"}"
    fi
    r="$r}"
    curl -sf -X POST "$SPICEDB_HTTP_URL/v1/relationships/write" \
        -H "Authorization: Bearer $SPICEDB_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"updates\":[{\"operation\":\"OPERATION_TOUCH\",\"relationship\":$r}]}" > /dev/null
}

# Users -> Roles (must match Keycloak applicationRolesPerZaaktype)
for user in behandelaar1newiam behandelaar2newiam; do
    write_rel role behandelaar member user "$user"
    write_rel role raadpleger member user "$user"
done
for user in raadpleger1newiam raadpleger2newiam; do
    write_rel role raadpleger member user "$user"
done
for user in coordinator1newiam coordinator2newiam; do
    for role in coordinator behandelaar raadpleger; do
        write_rel role "$role" member user "$user"
    done
done
# beheerder has all roles
for role in beheerder behandelaar coordinator recordmanager raadpleger; do
    write_rel role "$role" member user beheerder1newiam
done
for user in recordmanager1newiam recordmanager2newiam; do
    for role in recordmanager raadpleger coordinator behandelaar; do
        write_rel role "$role" member user "$user"
    done
done
# raadplegerenbehandelaar1newiam has both roles
write_rel role raadpleger member user raadplegerenbehandelaar1newiam
write_rel role behandelaar member user raadplegerenbehandelaar1newiam

# Resource→Role relationships on fixed "_" resource IDs.
# The adapter checks against "_" for all resource types, so these
# relationships apply to all resources of each type.
echo "Creating resource-role relationships..."

# zaak: all 5 base roles + conditional relations
for rel in raadpleger behandelaar coordinator recordmanager beheerder; do
    write_rel zaak _ "$rel" role "$rel" member
done
# Conditional relations (with caveats) for zaak
write_rel zaak _ behandelaar_when_open role behandelaar member zaak_is_open
write_rel zaak _ behandelaar_can_verlengen role behandelaar member zaak_can_verlengen
write_rel zaak _ behandelaar_can_opschorten role behandelaar member zaak_can_opschorten
write_rel zaak _ behandelaar_can_vastleggen_besluit role behandelaar member zaak_can_vastleggen_besluit

# taak
for rel in raadpleger behandelaar; do
    write_rel taak _ "$rel" role "$rel" member
done
write_rel taak _ behandelaar_when_open role behandelaar member taak_is_open

# document
for rel in raadpleger behandelaar recordmanager; do
    write_rel document _ "$rel" role "$rel" member
done
write_rel document _ behandelaar_zaak_open role behandelaar member doc_zaak_open
write_rel document _ behandelaar_can_edit role behandelaar member doc_can_edit
write_rel document _ behandelaar_can_delete role behandelaar member doc_can_delete
write_rel document _ rm_not_locked role recordmanager member doc_rm_can_delete
write_rel document _ behandelaar_unlock_or_own role behandelaar member doc_unlocked_or_own_lock
write_rel document _ behandelaar_own_lock role behandelaar member doc_own_lock
write_rel document _ behandelaar_definitief role behandelaar member doc_is_definitief

# zaak_notitie
for rel in raadpleger behandelaar; do
    write_rel zaak_notitie _ "$rel" role "$rel" member
done

# application
for rel in behandelaar beheerder raadpleger; do
    write_rel application _ "$rel" role "$rel" member
done

# werklijst
for rel in raadpleger coordinator recordmanager beheerder; do
    write_rel werklijst _ "$rel" role "$rel" member
done

echo "SpiceDB setup complete"
