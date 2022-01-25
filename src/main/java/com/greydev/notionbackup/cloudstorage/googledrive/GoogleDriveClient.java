package com.greydev.notionbackup.cloudstorage.googledrive;

import java.io.IOException;
import java.util.Collections;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.greydev.notionbackup.cloudstorage.CloudStorageClient;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class GoogleDriveClient implements CloudStorageClient {

	private final Drive driveService;
	private final String googleDriveRootFolderId;


	public GoogleDriveClient(Drive driveService, String googleDriveRootFolderId) {
		this.driveService = driveService;
		this.googleDriveRootFolderId = googleDriveRootFolderId;
	}


	public boolean upload(java.io.File fileToUpload) {

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
			return false;
		}

		FileContent notionExportFileContent = new FileContent("application/zip", fileToUpload);
		File fileMetadata = new File();
		fileMetadata.setName(fileToUpload.getName());
		fileMetadata.setParents(Collections.singletonList(googleDriveRootFolderId));
		try {
			driveService.files().create(fileMetadata, notionExportFileContent)
					.setFields("id, parents")
					.execute();
		} catch (IOException e) {
			log.warn("Google Drive: IOException ", e);
			return false;
		}
		log.info("Google Drive: successfully uploaded '{}'", fileToUpload.getName());
		return true;
	}

}
