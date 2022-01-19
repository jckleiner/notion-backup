# notion-backup

![example workflow name](https://github.com/jckleiner/notion-backup/workflows/notion-backup-workflow/badge.svg?branch=master)

### Warning: this repo is far from done, it's still work-in-progress!

see also https://github.com/openownership/notion-backup


### Set Credentials

Create a `.env` file with the following properties ([how to find all these values](./documentation/setup)):

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



### Docker

Once you created you `.env` file, you can run the following command to start your backup:

```bash
docker run \
    --rm=true \
    --env-file=.env \
    jckleiner/notion-backup
```

The exported files will be saved to the `/downloads` folder in the Docker container and the container will be 
removed when the backup is done (because of the `--rm=true` flag).

If you want to keep the downloaded files locally, you could mount the `/downloads` folder from the container 
somewhere on your machine:

```bash
docker run \
    --rm=true \
    --env-file=.env \
    -v <backup-dir-absolute-path-on-your-machine>:/downloads \
    jckleiner/notion-backup
```

You could also set a cronjob if you want trigger backups in regular intervals.

### Fork (GitHub Actions)

Another way ...

1. Create repository secrets for the following variables:

2. Fork this repository. That's it!


## Troubleshoot

If you got an Exception like this: `com.dropbox.core.BadResponseException: Bad JSON: expected object value.`, then try
to re-generate your access token and run the application again.



## TODO:

1. create Dockerfile so ci-cd will pull it from dockerhub
2. Create gifs for readme, update readme, document also how github actions work in this project
3. upload file to Nextcloud
4. GitClient
5. NextcloudClient
6. With cron to local folder