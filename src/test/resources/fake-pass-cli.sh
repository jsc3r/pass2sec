#!/bin/sh
# Deliberately fake credentials; never use this fixture with a real account.
printf '%s %s\n' "$1" "$2" >> "$0.calls"
case "$1 $2" in
  'login '|'logout ')
    exit 0
    ;;
  'item list')
    printf '%s\n' '{"items":[{"id":"item","title":"Example","item_type":"login"}]}'
    ;;
  'item view')
    printf '%s\n' '{"item":{"id":"item","share_id":"vault","content":{"title":"Example","content":{"Login":{"username":"user","email":"old@example.org","password":"fake-old-secret","urls":["https://example.org"]}}}}}'
    ;;
  'item update')
    printf '%s\n' 'Updated item successfully'
    ;;
  *)
    printf '%s\n' 'sensitive-provider-error' >&2
    exit 7
    ;;
esac
