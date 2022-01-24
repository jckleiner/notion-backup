# notion-backup

![example workflow name](https://github.com/jckleiner/notion-backup/workflows/notion-backup-docker-workflow/badge.svg?branch=master)

### Warning: this repo is far from done, it's still work-in-progress!

see also https://github.com/openownership/notion-backup


### Set Credentials

Create a `.env` file with the following properties ([How do I find all these values?](./documentation/setup.md)):

    # Make sure not to use any quotes around these environment variables
    
    # Notion (Required)
    NOTION_SPACE_ID=
    NOTION_EMAIL=
    NOTION_PASSWORD=

    # Google (Optional)
    GOOGLE_DRIVE_ROOT_FOLDER_ID=
    GOOGLE_DRIVE_SERVICE_ACCOUNT=
    GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_JSON=
    GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_FILE_PATH=

    # Dropbox (Optional)
    DROPBOX_ACCESS_TOKEN=



### Docker

Once you created your `.env` file, you can run the following command to start your backup:

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

### Dropbox

If you get the exception: `com.dropbox.core.BadResponseException: Bad JSON: expected object value.`, then try
to re-generate your access token and run the application again.


## TODO:

1. Create gifs for readme, update readme, document also how github actions work in this project
2. Upload files to Nextcloud
3. Upload files to a Git Repo