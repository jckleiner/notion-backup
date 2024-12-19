package com.greydev.notionbackup.cloudstorage.googledrive;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleDriveClientTest {

    public static final String APPLICATION_VND_GOOGLE_APPS_FOLDER = "application/vnd.google-apps.folder";
    public static final String TEST_FILE = "testFileToUpload.txt";
    public static final String TEST_FOLDER = "testFolder";
    public static final String PARENT_FOLDER_ID = "parentFolderId";
    public static final String TEST_FOLDER_ID = "testFolderId";
    public static final String ROOT_FOLDER_ID = "parentFolderId";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Drive googleDriveService;

    private com.google.api.services.drive.model.File responseFile;
    private com.google.api.services.drive.model.File responseFolder;

    @BeforeEach
    void setup() {
        responseFile = new com.google.api.services.drive.model.File();
        responseFile.setName(TEST_FILE);
        responseFile.setParents(Collections.singletonList(PARENT_FOLDER_ID));

        responseFolder = new com.google.api.services.drive.model.File();
        responseFolder.setName(TEST_FOLDER);
        responseFolder.setParents(Collections.singletonList(PARENT_FOLDER_ID));
        responseFolder.setMimeType(APPLICATION_VND_GOOGLE_APPS_FOLDER);
        responseFolder.setId(TEST_FOLDER_ID);
    }

    @Test
    public void testUpload() throws IOException {
        // given
        File fileToUpload = new File("src/test/resources/testFileToUpload.txt");
        GoogleDriveClient googleService = new GoogleDriveClient(googleDriveService, ROOT_FOLDER_ID);

        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        fileMetadata.setName(TEST_FILE);
        fileMetadata.setParents(Collections.singletonList(TEST_FOLDER_ID));

        FileContent notionExportFileContent = new FileContent("application/zip", fileToUpload);

        when(googleDriveService.files().create(any(), any()).setFields(anyString()).execute()).thenReturn(responseFile);
        when(googleDriveService.files().create(any()).setFields(anyString()).execute()).thenReturn(responseFolder);
        clearInvocations(googleDriveService);

        // when
        boolean result = googleService.upload(fileToUpload);

        // then
        assertTrue(result);
        verify(googleDriveService, times(5)).files();
        // eq(notionExportFileContent) does not work I assume because FileContent doesn't override the equals method?
        // com.google.api.client.http.FileContent@66908383 is not the same as com.google.api.client.http.FileContent@736ac09a
        // but eq() works for com.google.api.services.drive.model.File -> the toString {"name" = "testFileToUpload.txt", "parents" = [parentFolderId]}

        verify(googleDriveService.files(), times(2)).create(any(com.google.api.services.drive.model.File.class));
        verify(googleDriveService.files().create(any(com.google.api.services.drive.model.File.class)), times(2)).setFields("id");
        verify(googleDriveService.files().create(any(com.google.api.services.drive.model.File.class)).setFields("id"), times(2)).execute();
        verify(googleDriveService.files()).create(eq(fileMetadata), any(FileContent.class));
        verify(googleDriveService.files().create(eq(fileMetadata), any(FileContent.class))).setFields("id, parents");
        verify(googleDriveService.files().create(eq(fileMetadata), any(FileContent.class)).setFields("id, parents")).execute();
    }

    @Test
    public void testUpload_IOException() throws IOException {
        // given
        File fileToUpload = new File("src/test/resources/testFileToUpload.txt");
        GoogleDriveClient googleService = new GoogleDriveClient(googleDriveService, ROOT_FOLDER_ID);

        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        fileMetadata.setName(TEST_FILE);
        fileMetadata.setParents(Collections.singletonList(TEST_FOLDER_ID));

        when(googleDriveService.files().create(any(), any())).thenThrow(IOException.class);
        when(googleDriveService.files().create(any()).setFields(anyString()).execute()).thenReturn(responseFolder);
        clearInvocations(googleDriveService);

        // when
        boolean result = googleService.upload(fileToUpload);

        // then
        assertFalse(result);
        verify(googleDriveService, times(5)).files();
        verify(googleDriveService.files(), times(2)).create(any(com.google.api.services.drive.model.File.class));
        verify(googleDriveService.files()).create(eq(fileMetadata), any(FileContent.class));
    }

    @Test
    public void testUpload_invalidFile() {
        // given
        File fileToUpload = new File("thisFileDoesNotExist.txt");
        GoogleDriveClient googleService = new GoogleDriveClient(googleDriveService, PARENT_FOLDER_ID);

        // when
        boolean result = googleService.upload(fileToUpload);

        // then
        assertFalse(result);
        verifyNoInteractions(googleDriveService);
    }
}