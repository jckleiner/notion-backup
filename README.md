# notion-backup

![example workflow name](https://github.com/jckleiner/notion-backup/workflows/notion-backup-build-run/badge.svg?branch=master)

Automatically backup your Notion workspace to Google Drive, Dropbox, pCloud, Nextcloud or to your local machine.

### Set Credentials

Create a `.env` file with the following properties ([How do I find all these values?](./documentation/setup.md)):

    # Make sure not to use any quotes around these environment variables
    
    # Notion (Required)
    NOTION_SPACE_ID=
    NOTION_EMAIL=
    NOTION_PASSWORD=
    # Options: markdown, html (default is markdown)
    NOTION_EXPORT_TYPE=markdown
    # Create folders for nested pages? Options: true, false (default is false)
    NOTION_FLATTEN_EXPORT_FILETREE=false

    # Google Drive (Optional)
    GOOGLE_DRIVE_ROOT_FOLDER_ID=
    GOOGLE_DRIVE_SERVICE_ACCOUNT=
    # Provide either secret json or the path to the secret file
    GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_JSON=
    GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_FILE_PATH=

    # Dropbox (Optional)
    DROPBOX_ACCESS_TOKEN=

    # Nextcloud (Optional)
    NEXTCLOUD_EMAIL=
    NEXTCLOUD_PASSWORD=
    NEXTCLOUD_WEBDAV_URL=

    # pCloud (Optional)
    PCLOUD_ACCESS_TOKEN=
    PCLOUD_API_HOST=
    PCLOUD_FOLDER_ID=

### Backup to Cloud With Docker

Once you created your `.env` file, you can run the following command to start your backup:

```bash
docker run \
    --rm=true \
    --env-file=.env \
    ghcr.io/jckleiner/notion-backup
```

The downloaded Notion export file will be saved to the `/downloads` folder **within the Docker container** and the container
will be removed after the backup is done (because of the `--rm=true` flag).

If you want automatic backups in regular intervals, you could either set up a cronjob on your local machine or
[fork this repo](#fork-github-actions) and let GitHub Actions do the job.

### Local Backup With Docker

If you want to keep the downloaded files locally, you could mount the `/downloads` folder from the container somewhere
on your machine:

```bash
docker run \
    --rm=true \
    --env-file=.env \
    -v <backup-dir-absolute-path-on-your-machine>:/downloads \
    ghcr.io/jckleiner/notion-backup
```

If you want automatic backups in regular intervals, you could either set up a cronjob on your local machine or 
[fork this repo](#fork-github-actions) and let GitHub Actions do the job.

### Fork (GitHub Actions)

Another way to do automated backups is using GitHub Actions. You can simply:

1. Fork this repository.
2. Create repository secrets: Go to `notion-backup (your forked repo) > Settings > Secrets > Actions` and create all
   the [necessary environment variables](#set-credentials).
3. Go to `notion-backup (your forked repo) > Actions` to see the workflows and make sure the 
   `notion-backup-build-run` workflow is enabled. This is the workflow which will periodically build and run the 
   application.
4. You can adjust when the action will be triggered by editing the `schedule > cron` property in your 
   [notion-backup/.github/workflows/build-run.yml](.github/workflows/build-run.yml)
   workflow file (to convert time values into cron expressions: [crontab.guru](https://crontab.guru/)).

That's it. GitHub Actions will now run your workflow regularly at your defined time interval.

## Troubleshooting

### Dropbox

If you get the exception: `com.dropbox.core.BadResponseException: Bad JSON: expected object value.`, then try to
re-generate your Dropbox access token and run the application again.

### pCloud

If you get the exception: `com.pcloud.sdk.ApiError: 2094 - Invalid 'access_token' provided.`,
please also make sure that the `PCLOUD_API_HOST` environment variable is correct. There are currently two API hosts:
one for Europe (`eapi.pcloud.com`) and one for the rest (`api.pcloud.com`).
If you still get the error, please try and regenerate the access token as described in the [pCloud section](./documentation/setup.md#pcloud)
of the documentation.
