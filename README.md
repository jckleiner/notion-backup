# notion-backup

![example workflow name](https://github.com/jckleiner/notion-backup/workflows/notion-backup-workflow/badge.svg?branch=master)

TODO:

1. Gist: .env, apache http client
2. upload file to GDrive/Nextcloud
3. Finish README

### 1. Find Your notion-space-id

1. [Login](https://www.notion.so/login) to your notion profile
2. Open the network tab in your developer console
3. Click on "Quick Find" on the menu and type something
4. A new request will be sent to `search`. Open that request and copy the value of `spaceId`

![testImage](notion-search-request.png)

### 2. Set Credentials

Create a `.env` file in the root directory of the project with the following properties:

    NOTION_SPACE_ID=1234-56789-abcdef
    NOTION_EMAIL=notion@example.com
    NOTION_PASSWORD=password
    GDRIVE_ROOT_FOLDER_ID=<get-the-folder-id-from-gdrive>
    GDRIVE_SERVICE_ACCOUNT=<get-the-service-account-info-from-1password>

### 3. Start the Export

Run the application with ``

see also https://github.com/openownership/notion-backup
