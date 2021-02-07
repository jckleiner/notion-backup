package com.greydev.notionbackup;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class GoogleClient {

	private static final String OAUTH_SCOPE_GDRIVE = "https://www.googleapis.com/auth/drive";
	private static final String APPLICATION_NAME = "NOTION-BACKUP";
	private static final String KEY_GDRIVE_ROOT_FOLDER_ID = "GDRIVE_ROOT_FOLDER_ID";
	private static final String KEY_GDRIVE_SERVICE_ACCOUNT_SECRET_JSON = "GDRIVE_SERVICE_ACCOUNT_SECRET_JSON";
	private static final String KEY_GDRIVE_SERVICE_ACCOUNT_SECRET_FILE_PATH = "GDRIVE_SERVICE_ACCOUNT_SECRET_FILE_PATH";

	private final Drive driveService;
	private final String gdriveRootFolderId;


	// TODO inject
	GoogleClient(Dotenv dotenv) {
		gdriveRootFolderId = dotenv.get(KEY_GDRIVE_ROOT_FOLDER_ID);
		// TODO add check
		driveService = initGDriveService(extractServiceAccountSecret(dotenv));
	}


	private String extractServiceAccountSecret(Dotenv dotenv) {
		String serviceAccountSecret = dotenv.get(KEY_GDRIVE_SERVICE_ACCOUNT_SECRET_JSON);
		String serviceAccountSecretFilePath = dotenv.get(KEY_GDRIVE_SERVICE_ACCOUNT_SECRET_FILE_PATH);
		// Use the secret value if provided. If not then read the value from the given secret file path
		if (StringUtils.isBlank(serviceAccountSecret)) {
			if (StringUtils.isBlank(serviceAccountSecretFilePath)) {
				exit("Both " + KEY_GDRIVE_SERVICE_ACCOUNT_SECRET_JSON + " and " + KEY_GDRIVE_SERVICE_ACCOUNT_SECRET_FILE_PATH + " are empty");
			}
			return readFileContentAsString(serviceAccountSecretFilePath);
		}
		return serviceAccountSecret;
	}


	private String readFileContentAsString(String filePath) {
		try {
			String fileContent = FileUtils.readFileToString(new java.io.File(filePath), CharEncoding.UTF_8);
			if (StringUtils.isBlank(fileContent)) {
				exit("File '" + filePath + "' is empty");
			}
			return fileContent;
		} catch (IOException e) {
			// TODO
			log.error("", e);
			exit("IOException: Could not read file at " + filePath);
		}
		// TODO refactor
		return null; // should never be reached
	}


	private void exit(String message) {
		log.error(message);
		System.exit(1);
	}


	private Drive initGDriveService(String gdriveServiceAccountSecret) {
		//Build service account credential
		// We need to set a scope?
		// https://developers.google.com/identity/protocols/oauth2/scopes
		try {
			GoogleCredentials googleCredentials = GoogleCredentials
					.fromStream(new ByteArrayInputStream(gdriveServiceAccountSecret.getBytes()))
					.createScoped(Collections.singletonList(OAUTH_SCOPE_GDRIVE));
			return new Drive
					.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), new HttpCredentialsAdapter(googleCredentials))
					.setApplicationName(APPLICATION_NAME)
					.build();
		} catch (IOException | GeneralSecurityException e) {
			// TODO
			log.error("TODO", e);
		}
		// TODO optional
		return null;
	}


	public void upload(java.io.File fileToUpload) throws IOException, GeneralSecurityException {

		/* TODO how people normally you would process XML:
			TODO Create examples for every one of them
			1. Like I did with classes and annotations
			2. Something like swagger
			3. XSLD?
		 */
		// TODO how to get parent folder id from the api?
		// TODO refactor EXPORT_FILE_NAME date and extension
		// TODO Create a new directory each month?

		// create a file
		/*
			Service accounts also have their own Google Drive space. If we would create a new folder or file, it would be created in that space.
			But the problem is that the drive space won't be accessible from a GUI since the "real" user (who created the service account) doesn't have access
			to the drive space of the service account and there is no way to login with a service account to access the GUI.
			So the only way to see the files is through API calls.
		 */

		log.info("uploading file '{}' to Google Drive...", fileToUpload.getName());
		if (!(fileToUpload.exists() && fileToUpload.isFile())) {
			log.error("Could not find {} in project root directory", fileToUpload.getName());
			return;
		}

		//		FileContent notionExportFile = new FileContent("application/zip", filePath);
		FileContent notionExportFile = new FileContent("application/zip", fileToUpload);
		File fileMetadata = new File();
		fileMetadata.setName(fileToUpload.getName());
		fileMetadata.setParents(Collections.singletonList(gdriveRootFolderId));
		File file = driveService.files().create(fileMetadata, notionExportFile)
				.setFields("id, parents")
				.execute();
		log.info("Successfully uploaded '{}' to Google Drive", fileToUpload.getName());

	}

	//	private String upload() throws FileNotFoundException {
	//		HttpPost post = new HttpPost("http://echo.200please.com");
	//		InputStream inputStream = new FileInputStream("zipFileName");
	//		File imageFile = new File("imageFileName");
	//		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
	//		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
	//		builder.addBinaryBody
	//				("upfile", imageFile, ContentType.DEFAULT_BINARY, "imageFileName");
	//		builder.addBinaryBody
	//				("upstream", inputStream, ContentType.create("application/zip"), "zipFileName");
	//		builder.addTextBody("text", "This is a multipart post", ContentType.TEXT_PLAIN);
	//		//
	//		HttpEntity entity = builder.build();
	//		post.setEntity(entity);
	//		HttpResponse response = client.execute(post);
	//	}

	//	public void deleteLater() throws GeneralSecurityException, IOException {
	//
	//		GoogleCredentials googleCredentials = GoogleCredentials
	//				.fromStream(new FileInputStream(CREDENTIALS_FILE_NAME))
	//				.createScoped(Collections.singletonList(OAUTH_SCOPE_GDRIVE));
	//
	//		Drive driveService = new Drive
	//				.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), new HttpCredentialsAdapter(googleCredentials))
	//				.setApplicationName(APPLICATION_NAME)
	//				.build();
	//
	//		// Print the names and IDs for up to 10 files.
	//		FileList result = driveService.files().list()
	//				.setPageSize(10)
	//				.setFields("nextPageToken, files(id, name)")
	//				.execute();
	//
	//		log.info("Id of the shared drive to search: {}", driveService.files().list().getDriveId());
	//
	//		About about = driveService.about().get().setFields("*").execute();
	//		System.out.println("Drive user: " + about.getUser().getEmailAddress());
	//		System.out.println("getStorageQuota (Bytes): " + about.getStorageQuota());
	//		System.out.println("getKind: " + about.getKind());
	//
	//		List<File> files = result.getFiles();
	//		if (files == null || files.isEmpty()) {
	//			System.out.println("No files found");
	//		} else {
	//			System.out.println("Files:");
	//			for (File file : files) {
	//				System.out.printf("%s (%s)\n", file.getName(), file.getId());
	//			}
	//		}
	//	}

}
