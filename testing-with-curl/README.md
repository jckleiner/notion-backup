# Testing the Notion API with cURL

```bash
# TODO - example with curl on how trigger an export

# This will return a long list of notifications, also some from the past
# Search for 'export-completed' to find the export URL
# Make sure the timestamp is the one you are looking for
curl -X POST https://www.notion.so/api/v3/getNotificationLogV2 \
    -H "Content-Type: application/json" \
    -H "Cookie: token_v2=$NOTION_TOKEN_V2" \
    -d '{
          "spaceId": "'"$NOTION_SPACE_ID"'",
          "size": 20,
          "type": "unread_and_read",
          "variant": "no_grouping"
        }'

# TODO - example with curl on how to download the file, once you have the export URL


```