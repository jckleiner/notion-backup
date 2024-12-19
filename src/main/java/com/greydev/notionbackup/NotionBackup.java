package com.greydev.notionbackup;

import com.google.api.services.drive.Drive;
import com.greydev.notionbackup.cloudstorage.googledrive.GoogleDriveClient;
import com.greydev.notionbackup.cloudstorage.googledrive.GoogleDriveServiceFactory;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
public class NotionBackup {

    public static final String KEY_DROPBOX_ACCESS_TOKEN = "DROPBOX_ACCESS_TOKEN";
    public static final String KEY_DROPBOX_APP_KEY = "DROPBOX_APP_KEY";
    public static final String KEY_DROPBOX_APP_SECRET = "DROPBOX_APP_SECRET";
    public static final String KEY_DROPBOX_REFRESH_TOKEN = "DROPBOX_REFRESH_TOKEN";

    public static final String KEY_NEXTCLOUD_EMAIL = "NEXTCLOUD_EMAIL";
    public static final String KEY_NEXTCLOUD_PASSWORD = "NEXTCLOUD_PASSWORD";
    public static final String KEY_NEXTCLOUD_WEBDAV_URL = "NEXTCLOUD_WEBDAV_URL";

    private static final String KEY_GOOGLE_DRIVE_ROOT_FOLDER_ID = "GOOGLE_DRIVE_ROOT_FOLDER_ID";
    private static final String KEY_GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_JSON = "GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_JSON";
    private static final String KEY_GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_FILE_PATH = "GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_FILE_PATH";

    private static final String KEY_PCLOUD_ACCESS_TOKEN = "PCLOUD_ACCESS_TOKEN";
    private static final String KEY_PCLOUD_API_HOST = "PCLOUD_API_HOST";
    private static final String KEY_PCLOUD_FOLDER_ID = "PCLOUD_FOLDER_ID";

    private static final Dotenv dotenv;

    static {
        dotenv = initDotenv();
    }

    public static void main(String[] args) {
        log.info("---------------- Starting Notion Backup ----------------");

        NotionClient notionClient = new NotionClient(dotenv);

        final File exportedFile = notionClient.export()
                .orElseThrow(() -> new IllegalStateException("Could not export notion file"));

        // use a local file to skip the notion export step
        // final File exportedFile = new File("notion-export-markdown_2022-01-18_23-17-13.zip");

        AtomicBoolean hasErrorOccurred = new AtomicBoolean(false);

        CompletableFuture
                .runAsync(() -> NotionBackup.startGoogleDriveBackup(exportedFile))
                .handle((result, ex) -> {
                    if (ex != null) {
                        log.error("Exception while GoogleDrive upload", ex);
                        hasErrorOccurred.set(true);
                    }
                    return null;
                }).join();

//		CompletableFuture<Void> futureDropbox = CompletableFuture
//				.runAsync(() -> NotionBackup.startDropboxBackup(exportedFile))
//				.handle((result, ex) -> {
//					if (ex != null) {
//						hasErrorOccurred.set(true);
//						log.error("Exception while Dropbox upload", ex);
//					}
//					return null;
//				});
//
//		CompletableFuture<Void> futureNextcloud = CompletableFuture
//				.runAsync(() -> NotionBackup.startNextcloudBackup(exportedFile))
//				.handle((result, ex) -> {
//					if (ex != null) {
//						hasErrorOccurred.set(true);
//						log.error("Exception while Nextcloud upload", ex);
//					}
//					return null;
//				});
//
//		CompletableFuture<Void> futurePCloud = CompletableFuture
//				.runAsync(() -> NotionBackup.startPCloudBackup(exportedFile))
//				.handle((result, ex) -> {
//					if (ex != null) {
//						hasErrorOccurred.set(true);
//						log.error("Exception while Pcloud upload", ex);
//					}
//					return null;
//				});

//		CompletableFuture.allOf(futureGoogleDrive, futureDropbox, futureNextcloud, futurePCloud).join();

        if (hasErrorOccurred.get()) {
            log.error("Not all backups were completed successfully. See the logs above to get more information about the errors.");
            System.exit(1);
        }
    }

    public static void startGoogleDriveBackup(File fileToUpload) {
        Optional<String> serviceAccountSecretOptional = extractGoogleServiceAccountSecret();
        if (serviceAccountSecretOptional.isEmpty()) {
            log.info("Skipping Google Drive upload. No secret provided for Google Drive.");
            return;
        }
        Optional<Drive> googleServiceOptional = GoogleDriveServiceFactory.create(serviceAccountSecretOptional.get());
        if (googleServiceOptional.isEmpty()) {
            log.warn("Skipping Google Drive upload. Could not create Google Drive service.");
            return;
        }
        String googleDriveRootFolderId = dotenv.get(KEY_GOOGLE_DRIVE_ROOT_FOLDER_ID);
        if (StringUtils.isBlank(googleDriveRootFolderId)) {
            log.info("Skipping Google Drive upload. {} is blank.", KEY_GOOGLE_DRIVE_ROOT_FOLDER_ID);
            return;
        }
        GoogleDriveClient GoogleDriveClient = new GoogleDriveClient(googleServiceOptional.get(), googleDriveRootFolderId);
        boolean isSuccess = GoogleDriveClient.upload(fileToUpload);

        if (!isSuccess) {
            throw new IllegalStateException("Backup was not successful");
        }
    }

//	public static void startDropboxBackup(File fileToUpload) {
//		Optional<String> dropboxAccessToken = getDropboxAccessToken();
//		if (dropboxAccessToken.isEmpty()) {
//			log.info("No Dropbox access token available. Skipping Dropbox upload.");
//			return;
//		}
//
//		Optional<DbxClientV2> dropboxServiceOptional = DropboxServiceFactory.create(dropboxAccessToken.get());
//		if (dropboxServiceOptional.isEmpty()) {
//			log.warn("Could not create Dropbox service. Skipping Dropbox upload");
//			return;
//		}
//		DropboxClient dropboxClient = new DropboxClient(dropboxServiceOptional.get());
//		boolean isSuccess =  dropboxClient.upload(fileToUpload);
//
//		if (!isSuccess) {
//			throw new IllegalStateException("Backup was not successful");
//		}
//	}
//
//	private static Optional<String> getDropboxAccessToken() {
//		String dropboxAccessToken = dotenv.get(KEY_DROPBOX_ACCESS_TOKEN);
//
//		if (StringUtils.isBlank(dropboxAccessToken)) {
//			log.info("{} is blank. Trying to fetch an access token with the refresh token...", KEY_DROPBOX_ACCESS_TOKEN);
//
//			String dropboxAppKey = dotenv.get(KEY_DROPBOX_APP_KEY);
//			String dropboxAppSecret = dotenv.get(KEY_DROPBOX_APP_SECRET);
//			String dropboxRefreshToken = dotenv.get(KEY_DROPBOX_REFRESH_TOKEN);
//			if (StringUtils.isAnyBlank(dropboxAppKey, dropboxAppSecret, dropboxRefreshToken)) {
//				log.info("Failed to fetch an access token. Either {}, {} or {} is blank.", KEY_DROPBOX_REFRESH_TOKEN, KEY_DROPBOX_APP_KEY, KEY_DROPBOX_APP_SECRET);
//				return Optional.empty();
//			}
//
//			DbxCredential dbxCredential = new DbxCredential("", 14400L, dropboxRefreshToken, dropboxAppKey, dropboxAppSecret);
//			DbxRefreshResult refreshResult;
//			try {
//				refreshResult = dbxCredential.refresh(new DbxRequestConfig("NotionBackup"));
//			} catch (DbxException e) {
//				log.info("Token refresh call to Dropbox API failed.");
//				return Optional.empty();
//			}
//
//			dropboxAccessToken = refreshResult.getAccessToken();
//			log.info("Successfully fetched an access token.");
//		}
//
//		return Optional.of(dropboxAccessToken);
//	}
//
//
//	public static void startNextcloudBackup(File fileToUpload) {
//		String email = dotenv.get(KEY_NEXTCLOUD_EMAIL);
//		String password = dotenv.get(KEY_NEXTCLOUD_PASSWORD);
//		String webdavUrl = dotenv.get(KEY_NEXTCLOUD_WEBDAV_URL);
//
//		if (StringUtils.isAnyBlank(email, password, webdavUrl)) {
//			log.info("Skipping Nextcloud upload. {}, {} or {} is blank.", KEY_NEXTCLOUD_EMAIL, KEY_NEXTCLOUD_PASSWORD, KEY_NEXTCLOUD_WEBDAV_URL);
//			return;
//		}
//
//		boolean isSuccess = new NextcloudClient(email, password, webdavUrl).upload(fileToUpload);
//
//		if (!isSuccess) {
//			throw new IllegalStateException("Backup was not successful");
//		}
//	}
//
//	public static void startPCloudBackup(File fileToUpload) {
//		String pCloudAccessToken = dotenv.get(KEY_PCLOUD_ACCESS_TOKEN);
//		String pCloudApiHost = dotenv.get(KEY_PCLOUD_API_HOST);
//
//		if (StringUtils.isAnyBlank(pCloudAccessToken, pCloudApiHost)) {
//			log.info("Skipping pCloud upload. {} or {} is blank.", KEY_PCLOUD_ACCESS_TOKEN, KEY_PCLOUD_API_HOST);
//			return;
//		}
//
//		Optional<ApiClient> pCloudApiClient = PCloudApiClientFactory.create(pCloudAccessToken, pCloudApiHost);
//		if (pCloudApiClient.isEmpty()) {
//			log.info("Could not create pCloud API client. Skipping pCloud upload.");
//			return;
//		}
//
//		String pCloudFolderIdString = dotenv.get(KEY_PCLOUD_FOLDER_ID);
//		long pCloudFolderId = RemoteFolder.ROOT_FOLDER_ID;
//		if (StringUtils.isNotBlank(pCloudFolderIdString)) {
//			try {
//				pCloudFolderId = Long.parseLong(pCloudFolderIdString);
//			} catch (NumberFormatException e) {
//				log.warn("The given pCloud folder ID {} is not a valid number. Skipping pCloud upload.", pCloudFolderIdString);
//				return;
//			}
//		}
//		PCloudClient pCloudClient = new PCloudClient(pCloudApiClient.get(), pCloudFolderId);
//		boolean isSuccess = pCloudClient.upload(fileToUpload);
//
//		if (!isSuccess) {
//			throw new IllegalStateException("Backup was not successful");
//		}
//	}

    private static Optional<String> extractGoogleServiceAccountSecret() {
        String serviceAccountSecret = dotenv.get(KEY_GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_JSON);
        String serviceAccountSecretFilePath = dotenv.get(KEY_GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_FILE_PATH);

        // Use the secret value if provided.
        if (StringUtils.isNotBlank(serviceAccountSecret)) {
            return Optional.of(serviceAccountSecret);
        }
        // If not then read the value from the given path to the file which contains the secret
        if (StringUtils.isNotBlank(serviceAccountSecretFilePath)) {
            return readFileContentAsString(serviceAccountSecretFilePath);
        }
        log.info("Both {} and {} are empty", KEY_GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_JSON, KEY_GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_FILE_PATH);
        return Optional.empty();
    }

    private static Optional<String> readFileContentAsString(String filePath) {
        String fileContent = null;
        try {
            fileContent = FileUtils.readFileToString(new File(filePath), CharEncoding.UTF_8);
        } catch (IOException e) {
            log.warn("IOException while reading file in path: '{}'", filePath, e);
            return Optional.empty();
        }
        if (StringUtils.isBlank(fileContent)) {
            log.warn("File '{}' is empty", filePath);
            return Optional.empty();
        }
        return Optional.of(fileContent);
    }

    private static Dotenv initDotenv() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();
        if (dotenv == null) {
            throw new IllegalStateException("Could not load dotenv!");
        }
        return dotenv;
    }
}