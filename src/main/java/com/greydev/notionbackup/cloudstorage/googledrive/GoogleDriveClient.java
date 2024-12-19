package com.greydev.notionbackup.cloudstorage.googledrive;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.greydev.notionbackup.cloudstorage.CloudStorageClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
        fileMetadata.setParents(Collections.singletonList(getParent()));

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

    String getParent() {
        String dateBasedFolderIds = createDateBasedFolders();
        if (dateBasedFolderIds != null) {
            return dateBasedFolderIds;
        }

        return googleDriveRootFolderId;
    }

    String createDateBasedFolders() {
        String folderName = "Test Folder";
        String folderId = createFolder(folderName, googleDriveRootFolderId);

        return folderId != null ? folderId : googleDriveRootFolderId;
    }

    String createFolder(String name, String parentId) {
        File fileMetadata = new File();
        fileMetadata.setName(name);
        fileMetadata.setMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER);
        fileMetadata.setParents(List.of(parentId));

        try {
            findFiles();
            return driveService.files().create(fileMetadata)
                    .setFields("id")
                    .execute()
                    .getId();
        } catch (IOException e) {
            log.warn("Google Drive: IOException ", e);
        }

        return null;
    }

    List<File> findFiles() throws IOException {
        List<File> files = new ArrayList<>();

        String pageToken = null;
        do {
            String query = "mimeType = 'application/vnd.google-apps.folder' and '" + googleDriveRootFolderId + "' in parents";
            FileList result = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("nextPageToken, items(id, title)")
                    .setPageToken(pageToken)
                    .execute();

            if (result == null) continue;

            for (File file : result.getFiles()) {
                System.out.printf("Found file: %s (%s)\n",
                        file.getName(), file.getId());
            }

            files.addAll(result.getFiles());

            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        return files;
    }
}
