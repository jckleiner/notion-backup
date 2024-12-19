package com.greydev.notionbackup.cloudstorage.googledrive;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.greydev.notionbackup.cloudstorage.CloudStorageClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDate;
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

        try {
            fileMetadata.setName(fileToUpload.getName());
            fileMetadata.setParents(Collections.singletonList(createDateBasedFolders()));
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

    String createDateBasedFolders() throws IOException {
        LocalDate now = LocalDate.now();

        String yearFolderId = getYearFolder(now.getYear());

        return getMonthFolder(yearFolderId, now.getMonthValue());
    }

    String createFolder(String name, String parentId) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(name);
        fileMetadata.setMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER);
        fileMetadata.setParents(List.of(parentId));

        return driveService.files().create(fileMetadata)
                .setFields("id")
                .execute()
                .getId();
    }

    String getMonthFolder(final String yearFolder, final int month) throws IOException {
        List<File> folders = findFolders(yearFolder, month);

        if (folders.isEmpty()) {
            return createFolder(String.valueOf(month), yearFolder);
        }

        return folders.getFirst().getId();
    }

    String getYearFolder(final int year) throws IOException {
        List<File> folders = findFolders(googleDriveRootFolderId, year);

        if (folders.isEmpty()) {
            return createFolder(String.valueOf(year), googleDriveRootFolderId);
        }

        return folders.getFirst().getId();
    }

    List<File> findFolders(final String parentId, final int folderName) {
        List<File> files = new ArrayList<>();

        String pageToken = null;
        final String query = String.format("mimeType='application/vnd.google-apps.folder' and name='%d' and '%s' in parents", folderName, parentId);
        try {
            do {
                final FileList result = driveService.files().list()
                        .setQ(query)
                        .setSpaces("drive")
                        .setFields("nextPageToken,files(id,name)")
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
        } catch (IOException e) {
            log.warn("Google Drive list: IOException ", e);
        }

        return files;
    }
}
