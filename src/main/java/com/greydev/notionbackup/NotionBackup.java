package com.greydev.notionbackup;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

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

	// TODO DI
	// TODO if export limit exceeded, print response
	// TODO publish github package with actions when new merge in master
	// TODO GitClient
	// TODO NexcloudClient
	// TODO With cron to local folder
	// TODO Docker
	// TODO give file name as param?
	// TODO add tests, mockito
	// TODO testing with okhttp's mock server?
	// TODO test where the download will go if the path is "exportFolder/", will the jar create it in the pwd?
	// TODO make a common interface, make a list, loop and call upload on each, make upload async
	// TODO best way to run async code in java?
	// TODO // method call or code to be async.
	// TODO where and how to handle errors
	// 	if no key is given, we don't want to stop the whole app, just ignore the upload call with logs

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
		//		final File exportedFile = notionClient.export()
		//				.orElseThrow(() -> new IllegalStateException("Could not export notion file"));
		final File exportedFile = new File("notion-export-markdown_2021-02-06_23-37.zip");

		CompletableFuture<Void> futureGoogleDrive = CompletableFuture.runAsync(() -> NotionBackup.startGoogleDriveBackup(exportedFile));
		CompletableFuture<Void> futureDropbox = CompletableFuture.runAsync(() -> NotionBackup.startDropboxBackup(exportedFile));

		CompletableFuture.allOf(futureGoogleDrive, futureDropbox).join();
	}


	public static void startGoogleDriveBackup(File fileToUpload) {
		Optional<String> serviceAccountSecretOptional = extractGoogleServiceAccountSecret();
		if (serviceAccountSecretOptional.isEmpty()) {
			log.info("No secret provided for GoogleDrive. Skipping GoogleDrive.");
			return;
		}
		Optional<Drive> googleServiceOptional = GoogleDriveServiceFactory.create(serviceAccountSecretOptional.get());
		if (googleServiceOptional.isEmpty()) {
			log.warn("Could not create GoogleDrive service. Skipping GoogleDrive.");
			return;
		}
		String googleDriveRootFolderId = dotenv.get(KEY_GOOGLE_DRIVE_ROOT_FOLDER_ID);
		if (StringUtils.isBlank(googleDriveRootFolderId)) {
			log.info("{} is blank, skipping GoogleDrive...", KEY_GOOGLE_DRIVE_ROOT_FOLDER_ID);
			return;
		}
		GoogleDriveClient GoogleDriveClient = new GoogleDriveClient(googleServiceOptional.get(), googleDriveRootFolderId);
		GoogleDriveClient.upload(fileToUpload);
	}


	public static void startDropboxBackup(File fileToUpload) {
		String dropboxAccessToken = dotenv.get(KEY_DROPBOX_ACCESS_TOKEN);
		if (StringUtils.isBlank(dropboxAccessToken)) {
			log.info("{} is blank, skipping Dropbox...", KEY_DROPBOX_ACCESS_TOKEN);
			return;
		}
		Optional<DbxClientV2> dropboxServiceOptional = DropboxServiceFactory.create(dropboxAccessToken);
		if (dropboxServiceOptional.isEmpty()) {
			log.warn("Dropbox service is empty. Skipping...");
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

	//		CookieStore cookieStore = new BasicCookieStore();
	//		CloseableHttpClient httpClient = initHttpClient(cookieStore);


	public static CloseableHttpClient initHttpClient(CookieStore cookieStore) {
		// Prevent warning 'Invalid cookie header' warning by setting a cookie spec
		// Also adding a cookie store to be able to access the cookies
		return HttpClients.custom()
				.setDefaultCookieStore(cookieStore)
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
				.build();
	}

}