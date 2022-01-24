package com.greydev.notionbackup;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.dropbox.core.v2.DbxClientV2;
import com.google.api.services.drive.Drive;
import com.greydev.notionbackup.cloudstorage.dropbox.DropboxClient;
import com.greydev.notionbackup.cloudstorage.dropbox.DropboxServiceFactory;
import com.greydev.notionbackup.cloudstorage.googledrive.GoogleDriveClient;
import com.greydev.notionbackup.cloudstorage.googledrive.GoogleDriveServiceFactory;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class NotionBackup {

	// TODO add another github action: build the docker container, push it to my repo and then run
	// TODO add those actions to gists
	// TODO add Nextcloud upload- https://docs.nextcloud.com/server/latest/developer_manual/client_apis/WebDAV/basic.html#uploading-files
	// TODO add tests, mockito

	public static final String KEY_DROPBOX_ACCESS_TOKEN = "DROPBOX_ACCESS_TOKEN";

	private static final String KEY_GOOGLE_DRIVE_ROOT_FOLDER_ID = "GOOGLE_DRIVE_ROOT_FOLDER_ID";
	private static final String KEY_GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_JSON = "GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_JSON";
	private static final String KEY_GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_FILE_PATH = "GOOGLE_DRIVE_SERVICE_ACCOUNT_SECRET_FILE_PATH";

	private static final Dotenv dotenv;

	static {
		dotenv = initDotenv();
	}

	public static void main(String[] args) {
		NotionClient notionClient = new NotionClient(dotenv);

		final File exportedFile = notionClient.export()
				.orElseThrow(() -> new IllegalStateException("Could not export notion file"));

		// use a local file to skip the notion export step
		// final File exportedFile = new File("notion-export-markdown_2022-01-17_22-39.zip");

		CompletableFuture<Void> futureGoogleDrive = CompletableFuture.runAsync(() -> NotionBackup.startGoogleDriveBackup(exportedFile));
		CompletableFuture<Void> futureDropbox = CompletableFuture.runAsync(() -> NotionBackup.startDropboxBackup(exportedFile));

		CompletableFuture.allOf(futureGoogleDrive, futureDropbox).join();
	}


	public static void startGoogleDriveBackup(File fileToUpload) {
		Optional<String> serviceAccountSecretOptional = extractGoogleServiceAccountSecret();
		if (serviceAccountSecretOptional.isEmpty()) {
			log.info("No secret provided for Google Drive. Skipping Google Drive upload.");
			return;
		}
		Optional<Drive> googleServiceOptional = GoogleDriveServiceFactory.create(serviceAccountSecretOptional.get());
		if (googleServiceOptional.isEmpty()) {
			log.warn("Could not create Google Drive service. Skipping Google Drive upload.");
			return;
		}
		String googleDriveRootFolderId = dotenv.get(KEY_GOOGLE_DRIVE_ROOT_FOLDER_ID);
		if (StringUtils.isBlank(googleDriveRootFolderId)) {
			log.info("{} is blank. Skipping Google Drive upload.", KEY_GOOGLE_DRIVE_ROOT_FOLDER_ID);
			return;
		}
		GoogleDriveClient GoogleDriveClient = new GoogleDriveClient(googleServiceOptional.get(), googleDriveRootFolderId);
		GoogleDriveClient.upload(fileToUpload);
	}


	public static void startDropboxBackup(File fileToUpload) {
		String dropboxAccessToken = dotenv.get(KEY_DROPBOX_ACCESS_TOKEN);

		if (StringUtils.isBlank(dropboxAccessToken)) {
			log.info("{} is blank. Skipping Dropbox upload.", KEY_DROPBOX_ACCESS_TOKEN);
			return;
		}
		Optional<DbxClientV2> dropboxServiceOptional = DropboxServiceFactory.create(dropboxAccessToken);
		if (dropboxServiceOptional.isEmpty()) {
			log.warn("Could not create Dropbox service. Skipping Dropbox upload");
			return;
		}
		DropboxClient dropboxClient = new DropboxClient(dropboxServiceOptional.get());
		dropboxClient.upload(fileToUpload);
	}


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