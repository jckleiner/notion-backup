package com.greydev.notionbackup.cloudstorage.googledrive;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
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

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Drive googleDriveService;

    @Test
    public void testUpload() throws IOException {
        // given
        File fileToUpload = new File("src/test/resources/testFileToUpload.txt");
        String googleDriveRootFolderId = "parentFolderId";
        GoogleDriveClient googleService = new GoogleDriveClient(googleDriveService, googleDriveRootFolderId);

        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        fileMetadata.setName("testFileToUpload.txt");
        fileMetadata.setParents(Collections.singletonList("parentFolderId"));

        FileContent notionExportFileContent = new FileContent("application/zip", fileToUpload);

        when(googleDriveService.files().create(any(), any()).setFields(anyString()).execute()).thenReturn(null);
        when(googleDriveService.files().create(any()).setFields(anyString()).execute()).thenReturn(null);
        clearInvocations(googleDriveService);

        // when
        boolean result = googleService.upload(fileToUpload);

        // then
        assertTrue(result);
        verify(googleDriveService, times(2)).files();
        // eq(notionExportFileContent) does not work I assume because FileContent doesn't override the equals method?
        // com.google.api.client.http.FileContent@66908383 is not the same as com.google.api.client.http.FileContent@736ac09a
        // but eq() works for com.google.api.services.drive.model.File -> the toString {"name" = "testFileToUpload.txt", "parents" = [parentFolderId]}
        verify(googleDriveService.files()).create(eq(fileMetadata), any(FileContent.class));
        verify(googleDriveService.files().create(eq(fileMetadata), any(FileContent.class))).setFields("id, parents");
        verify(googleDriveService.files().create(eq(fileMetadata), any(FileContent.class)).setFields("id, parents")).execute();
    }

    @Test
    public void testUpload_IOException() throws IOException {
        // given
        File fileToUpload = new File("src/test/resources/testFileToUpload.txt");
        String googleDriveRootFolderId = "parentFolderId";
        GoogleDriveClient googleService = new GoogleDriveClient(googleDriveService, googleDriveRootFolderId);

        com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
        fileMetadata.setName("testFileToUpload.txt");
        fileMetadata.setParents(Collections.singletonList("parentFolderId"));

        when(googleDriveService.files().create(any(), any())).thenThrow(IOException.class);
        when(googleDriveService.files().create(any()).setFields(anyString()).execute()).thenReturn(null);
        clearInvocations(googleDriveService);

        // when
        boolean result = googleService.upload(fileToUpload);

        // then
        assertFalse(result);
        verify(googleDriveService, times(2)).files();
        verify(googleDriveService.files()).create(eq(fileMetadata), any(FileContent.class));
    }

    @Test
    public void testUpload_invalidFile() {
        // given
        File fileToUpload = new File("thisFileDoesNotExist.txt");
        String googleDriveRootFolderId = "parentFolderId";
        GoogleDriveClient googleService = new GoogleDriveClient(googleDriveService, googleDriveRootFolderId);

        // when
        boolean result = googleService.upload(fileToUpload);

        // then
        assertFalse(result);
        verifyNoInteractions(googleDriveService);
    }
}