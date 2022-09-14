### Find Your notion-space-id

1. Login to your [notion profile](https://www.notion.so/login)
2. Open your developer console of your browser and go to the "Network" tab
3. Click on "Quick Find" on the Notion menu (should be at the upper left corner) and type something in the search bar
4. Typing will trigger a new request with the name `search` which should be visible under the network tab. Open that
   request and copy the value of `spaceId`

![testImage](../images/notion-search-request.png)

### Dropbox

1. Create a new app on developer console (https://www.dropbox.com/developers/apps/create)
2. Go to the Permissions tab > enable `files.content.write` & `files.content.read` and click "Submit" to save your changes.
Make sure you saved these changes **before** you generate your access token.
3. Go to the Settings tab > OAuth 2 > Generate access token > Generate. Note that these tokens are short-lived and expire after a few hours.
   Due to security reason long-lived tokens have been deprecated.
   
For an automated setup, for example with [GitHub Action](../README.md#fork-github-actions), short-lived access tokens will not work because they will expire.
An alternative solution would be to use a refresh token that does not expire which you need to retrieve manually once.
The refresh token will then be used to fetch an access token on-the-fly everytime the application runs.
Do the following steps to set up the refresh token flow.

1. Go to the Settings tab and store the App Key in the `DROPBOX_APP_KEY` environment variable
2. Go to the Settings tab and store the App Secret in the `DROPBOX_APP_SECRET` environment variable
3. Open a browser and enter the following URL and replace the `<APP_KEY>` placeholder

        https://www.dropbox.com/oauth2/authorize?client_id=<APP_KEY>&token_access_type=offline&response_type=code

4. Click on continue and allow the app to access your files
5. Copy the authorization code (referred to as `AUTH_CODE` in the following steps) shown in the following screen
6. Send the following HTTP POST request with the actual values replacing the placeholders

```
curl --request POST \
  --url https://api.dropboxapi.com/oauth2/token \
  --header 'Authorization: Basic <BASE64 ENCODING OF <APP_KEY>:<APP_SECRET>>' \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data code=<AUTH_CODE> \
  --data grant_type=authorization_code
```

7. Extract the `refresh_token` from the response and store it in the `DROPBOX_REFRESH_TOKEN` environment variable.
   **DO NOT SHARE THIS TOKEN WITH ANYONE!**

8. To ensure it works try sending the following HTTP POST request with the actual values replacing the placeholders

```
curl --request POST \
  --url https://api.dropbox.com/oauth2/token \
  --header 'Authorization: Basic <BASE64 ENCODING OF <APP_KEY>:<APP_SECRET>>' \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data grant_type=refresh_token \
  --data refresh_token=<REFRESH_TOKEN>
```

9. Leave the `DROPBOX_ACCESS_TOKEN` environment variable empty.
   Otherwise, the refresh token flow will be skipped and no new access token will be fetched.

### Nextcloud

All you need is to provide your Email, password and the WebDAV URL.

On your main Nextcloud page, click Settings at the bottom left. This should show you a WebDAV URL
like `https://my.nextcloud.tld/remote.php/dav/files/EMAIL/path/to/directory/`

* If the WebDAV URL ends with a `/`, for instance `https://my.nextcloud.tld/remote.php/dav/files/EMAIL/Documents/`: this
  indicates the uploaded file will be placed in the `Documents` folder.
* If the WebDAV URL **does not end** with a `/`, for
  instance `https://my.nextcloud.tld/remote.php/dav/files/EMAIL/Documents/somefile.txt`: this indicates the uploaded
  file will be named `somefile.txt` and it will be placed in the `Documents` folder. If a file with the same name 
  exists, it will be overwritten.
* All the folders must be present

### Google Drive

> This is a pretty tedious task. If someone knows a better way to do this, please let me know.

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
12. Rename the downloaded file to `credentials.json` and move it to the project root directory. The path to this 
    file is the value for your `GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_FILE_PATH` environment variable.
    Alternatively, if you don't want to keep a `credentials.json` file, you could copy the contents of 
    `credentials.json` file and provide it as a value to your `GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_JSON` environment variable.
13. Login to your [Google Drive account](https://drive.google.com/drive/) and select the folder you want your notion
    backups to be saved in. You need to share that folder with the service account you've just created. Right click on
    the folder -> Share -> enter the E-Mail address of your service account. (The email of your service account will 
    probably look something like `XXX@XXX.iam.gserviceaccount.com`. This is the value for your 
    `GOOGLE_DRIVE_SERVICE_ACCOUNT` environment variable.)
14. Open that folder by double-clicking on it. You will now be able to see the id of that folder in the URL. It 
    should look something like `https://drive.google.com/drive/folders/62F2faJbVasSGsYGyQzBeGSc2-k7GOZg2`. The ID 
    (only the last part `62F2faJbVasSGsYGyQzBeGSc2-k7GOZg2` of the URL) is the value for 
    your `GOOGLE_DRIVE_ROOT_FOLDER_ID` environment variable.
