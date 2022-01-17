# notion-backup

![example workflow name](https://github.com/jckleiner/notion-backup/workflows/notion-backup-workflow/badge.svg?branch=master)

### Warning: this repo is far from done, it's still work-in-progress!

### Find Your notion-space-id

You need to find ... TODO

1. Login to your [notion profile](https://www.notion.so/login)
2. Open your developer console of your browser and go to the "Network" tab
3. Click on "Quick Find" on the Notion menu (should be at the upper left corner) and type something in the search bar
4. Typing will trigger a new request with the name `search` which should be visible under the network tab. Open that
   request and copy the value of `spaceId`

![testImage](images/notion-search-request.png)

## Backup Options

### Google Drive

1. Login to your [google developer console](https://console.developers.google.com/)
2. Open [cloud-resource-manager](https://console.cloud.google.com/cloud-resource-manager) and create a project.
3. Go to the [Google Drive API](https://console.cloud.google.com/apis/library/drive.googleapis.com) page
4. Make sure in the top left corner you've selected your newly created project
5. Click "Enable" (if Google Driver API is already enabled for your project, you will see a "Manage" button. In that
   case you can continue with the next step)
6. Open the [credentials page](https://console.cloud.google.com/apis/credentials)
7. Click "Create credentials" and select "Service Account"
8. Give it a name and click "DONE" (You can ignore the other steps)
9. You will see your newly created service account E-Mail address under the "Service Accounts" section. Click on that
   account.
10. Copy the E-Mail address of your service account since you will need it later.
11. Keys -> Add Key -> Create new key -> JSON and download that file
12. Rename the downloaded file to `credentials.json` and move it to the project root directory.
13. Login to your [Google Drive account](https://drive.google.com/drive/) and select the folder you want your notion
    backups to be saved in. You need to share that folder with the service account you've just created. Right click on
    the folder -> Share -> enter the E-Mail address of your service account. (Your service account's address probably
    looks like XXX@XXX.iam.gserviceaccount.com)
14. TODO - Copy the id of that folder, from the URL
15. You are now ready to setup the application. Go to TODO - link to setup...

Now, we have obtained our credentials, move to the [First run](#first-run) section to use those credentials:

[Go back to oauth credentials setup](#generating-oauth-credentials)

### Dropbox

TODO: GIF

1. Create app on developer console (https://www.dropbox.com/developers/apps/create)
2. (Important to change the permissions first before generating a token)
   Go to permissions tab > enable `files.content.write` & `files.content.read` and click "Submit" to save your changes
3. Go to "Settings > OAuth 2 > Generate access token > generate > Access token expiration: "no expiration and copy the
   token

> Long-lived tokens are less secure and will be deprecated in the future.

If you got an Exception like this: `com.dropbox.core.BadResponseException: Bad JSON: expected object value.`, then try
to re-generate your access token and run the application again.

TODO: If/when the no-expiration-date tokens are removed then update the authentication method.

### Folder on your local machine

TODO

## Setup

### 3 ways

* Fork
* Docker?
* Locally?

### Fork

1. Create repository secrets for the following variables:

        # Required
        NOTION_SPACE_ID=1234-56789-abcdef
        NOTION_EMAIL=notion@example.com
        NOTION_PASSWORD=password

        # Optional
        GOOGLE_DRIVE_ROOT_FOLDER_ID=<get-the-folder-id-from-google-drive>
        GOOGLE_DRIVE_SERVICE_ACCOUNT=<get-the-service-account-info-from-1password>
        GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_JSON=
        GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_FILE_PATH=<file-path-to-json-credentials-file>
        
        DROPBOX_ACCESS_TOKEN=<generate-token-from-dropbox-developer-console>

2. Fork this repository. That's it!

## Running it locally

### Set Credentials

Create a `.env` file in the root directory of the project with the following properties:

    NOTION_SPACE_ID=1234-56789-abcdef
    NOTION_EMAIL=notion@example.com
    NOTION_PASSWORD=password
    GOOGLE_DRIVE_ROOT_FOLDER_ID=<get-the-folder-id-from-google-drive>
    GOOGLE_DRIVE_SERVICE_ACCOUNT=<get-the-service-account-info-from-1password>
    GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_JSON=
    GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_FILE_PATH=<file-path-to-json-credentials-file>
    DROPBOX_ACCESS_TOKEN=<generate-token-from-dropbox-developer-console>

### 4. Start the Export

Docker?

Run the application with ``

see also https://github.com/openownership/notion-backup

TODO:

0. Create gifs for readme, update readme, document also how github actions work in this project
1. Gist: GoogleCredentials-Service Account, apache http client, okhttp
2. upload file to Nextcloud
3. create Dockerfile so ci-cd will pull it from dockerhub
4. GitClient
5. NextcloudClient
6. With cron to local folder
7. Docker