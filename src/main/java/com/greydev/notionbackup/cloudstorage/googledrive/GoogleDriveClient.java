package com.greydev.notionbackup.cloudstorage.googledrive;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.greydev.notionbackup.cloudstorage.CloudStorageClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Slf4j
public class GoogleDriveClient implements CloudStorageClient {

    public static final String APPLICATION_VND_GOOGLE_APPS_FOLDER = "application/vnd.google-apps.folder";

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
        fileMetadata.setParents(getParent());

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

    private List<String> getParent() {
        ArrayList<String> ids = new ArrayList<>();
        ids.add(googleDriveRootFolderId);

        List<String> dateBasedFolderIds = createDateBasedFolders();
        if (!dateBasedFolderIds.isEmpty()) {
            ids.addAll(dateBasedFolderIds);
        }

        return ids;
    }

    private List<String> createDateBasedFolders() {
        String folderName = "Test Folder";
        File fileMetadata = new File();
        fileMetadata.setName(folderName);
        fileMetadata.setMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER);
        fileMetadata.setParents(List.of(googleDriveRootFolderId));

        try {
            File folder = driveService.files().create(fileMetadata)
                    .setFields("id")
                    .execute();

            if (folder != null) {
                return List.of(folder.getId());
            }
        } catch (IOException e) {
            log.warn("Google Drive: IOException ", e);
        }

        log.info("Google Drive: successfully created folder '{}'", folderName);
        return List.of();
    }
}
