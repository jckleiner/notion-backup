### Find Your notion-space-id

1. Login to your [notion profile](https://www.notion.so/login)
2. Open your developer console of your browser and go to the "Network" tab
3. Click on "Quick Find" on the Notion menu (should be at the upper left corner) and type something in the search bar
4. Typing will trigger a new request with the name `search` which should be visible under the network tab. Open that
   request and copy the value of `spaceId`

![testImage](../images/notion-search-request.png)

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

### Dropbox

TODO: GIF

1. Create a new app on developer console (https://www.dropbox.com/developers/apps/create)
2. Go to permissions tab > enable `files.content.write` & `files.content.read` and click "Submit" to save your
   changes. Make sure you saved these changes **before** you generate your access token
3. Go to Settings > OAuth 2 > Generate access token > generate > Access token expiration: "no expiration" and copy the
   generated token

> Long-lived tokens are less secure and will be deprecated in the future.