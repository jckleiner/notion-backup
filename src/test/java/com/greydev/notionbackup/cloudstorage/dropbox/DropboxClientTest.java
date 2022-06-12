package com.greydev.notionbackup.cloudstorage.dropbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@Slf4j
@ExtendWith(MockitoExtension.class)
public class DropboxClientTest {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private DbxClientV2 dropboxService;


	@Test
	public void testUpload_success() throws DbxException, IOException {
		// given
		DropboxClient testee = spy(new DropboxClient(dropboxService));

		// Mocking a file is not a good idea
		File testFileToUpload = new File("src/test/resources/testFileToUpload.txt");
		if (!testFileToUpload.exists()) {
			System.out.println("Test file does not exist...");
		}
		when(dropboxService.files().uploadBuilder(anyString()).uploadAndFinish(any())).thenReturn(null);
		doReturn(true).when(testee).doesFileExist(anyString());

		// when
		boolean result = testee.upload(testFileToUpload);

		// then
		verify(dropboxService.files().uploadBuilder("/testFileToUpload.txt")).uploadAndFinish(any(FileInputStream.class));
		verify(testee).doesFileExist("testFileToUpload.txt");
		assertTrue(result);
	}


	@Test
	public void testUpload_givenFileToUploadDoesNotExist() throws DbxException {
		// given
		DropboxClient testee = spy(new DropboxClient(dropboxService));
		File nonExistingFile = new File("thisFileDoesNotExist.txt");

		// when
		boolean result = testee.upload(nonExistingFile);

		// then
		verifyNoInteractions(dropboxService);
		verify(testee, never()).doesFileExist(anyString());
		assertFalse(result);
	}


	@Test
	public void testUpload_failureBecauseDbxExceptionDuringUpload() throws DbxException, IOException {
		// given
		DropboxClient testee = spy(new DropboxClient(dropboxService));

		// Mocking a file is not a good idea
		File testFileToUpload = new File("src/test/resources/testFileToUpload.txt");
		if (!testFileToUpload.exists()) {
			System.out.println("Test file does not exist...");
		}
		when(dropboxService.files().uploadBuilder(anyString()).uploadAndFinish(any())).thenThrow(DbxException.class);

		// when
		boolean result = testee.upload(testFileToUpload);

		// then
		verify(dropboxService.files().uploadBuilder("/testFileToUpload.txt")).uploadAndFinish(any(FileInputStream.class));
		verify(testee, never()).doesFileExist(anyString());
		assertFalse(result);
	}


	@Test
	public void testUpload_failureBecauseFileNotFoundAfterUpload() throws DbxException, IOException {
		// given
		DropboxClient testee = spy(new DropboxClient(dropboxService));

		// Mocking a file is not a good idea
		File testFileToUpload = new File("src/test/resources/testFileToUpload.txt");
		if (!testFileToUpload.exists()) {
			System.out.println("Test file does not exist...");
		}
		when(dropboxService.files().uploadBuilder(anyString()).uploadAndFinish(any())).thenReturn(null);
		doReturn(false).when(testee).doesFileExist(anyString());

		// when
		boolean result = testee.upload(testFileToUpload);

		// then
		verify(dropboxService.files().uploadBuilder("/testFileToUpload.txt")).uploadAndFinish(any(FileInputStream.class));
		verify(testee).doesFileExist("testFileToUpload.txt");
		assertFalse(result);
	}


	@Test
	public void testDoesFileExist_true() throws DbxException {
		// given
		DropboxClient testee = new DropboxClient(dropboxService);

		Metadata m1 = new Metadata("folder1");
		Metadata m2 = new Metadata("folder2");
		Metadata m3 = new Metadata("testFileToUpload.txt");
		List<Metadata> metadataList = List.of(m1, m2, m3);
		ListFolderResult listFolderResult = new ListFolderResult(metadataList, "3", true);
		when(dropboxService.files().listFolder(anyString())).thenReturn(listFolderResult);
		clearInvocations(dropboxService);

		// when
		boolean result = testee.doesFileExist("testFileToUpload.txt");

		// then
		verify(dropboxService.files()).listFolder("");
		assertTrue(result);
	}


	@Test
	public void testDoesFileExist_false() throws DbxException {
		// given
		DropboxClient testee = new DropboxClient(dropboxService);

		Metadata m1 = new Metadata("folder1");
		Metadata m2 = new Metadata("folder2");
		List<Metadata> metadataList = List.of(m1, m2);
		ListFolderResult listFolderResult = new ListFolderResult(metadataList, "2", true);

		when(dropboxService.files().listFolder(anyString())).thenReturn(listFolderResult);
		clearInvocations(dropboxService);

		// when
		boolean result = testee.doesFileExist("testFileToUpload.txt");

		// then
		verify(dropboxService.files()).listFolder("");
		assertFalse(result);
	}

}
