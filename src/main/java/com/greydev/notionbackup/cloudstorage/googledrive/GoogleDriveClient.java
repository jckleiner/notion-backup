package com.greydev.notionbackup.cloudstorage.googledrive;

import java.io.IOException;
import java.util.Collections;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class GoogleDriveClient {

	private final Drive driveService;
	private final String googleDriveRootFolderId;


	// TODO inject
	public GoogleDriveClient(Drive driveService, String googleDriveRootFolderId) {
		this.driveService = driveService;
		this.googleDriveRootFolderId = googleDriveRootFolderId;
	}


	public void upload(java.io.File fileToUpload) {

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

		log.info("Google Drive: uploading file '{}' ...", fileToUpload.getName());
		if (!(fileToUpload.exists() && fileToUpload.isFile())) {
			log.error("Google Drive: could not find {} in project root directory", fileToUpload.getName());
			return;
		}

		//		FileContent notionExportFile = new FileContent("application/zip", filePath);
		FileContent notionExportFile = new FileContent("application/zip", fileToUpload);
		File fileMetadata = new File();
		fileMetadata.setName(fileToUpload.getName());
		fileMetadata.setParents(Collections.singletonList(googleDriveRootFolderId));
		try {
			File file = driveService.files().create(fileMetadata, notionExportFile)
					.setFields("id, parents")
					.execute();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Google Drive: Successfully uploaded '{}'", fileToUpload.getName());

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
	//				.createScoped(Collections.singletonList(OAUTH_SCOPE_GOOGLE_DRIVE));
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
