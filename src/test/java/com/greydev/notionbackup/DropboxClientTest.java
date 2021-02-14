package com.greydev.notionbackup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

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
public class DropboxClientTest {

	// TODO use before each or a similar construct to aggregate common logic


	@Test
	public void testUpload_success() throws DbxException, IOException {
		// given
		DbxClientV2 dbxClientMock = mock(DbxClientV2.class, RETURNS_DEEP_STUBS); // Mock a fluent API object
		DropboxClient testee = spy(new DropboxClient(dbxClientMock));

		// Mocking a file is not a good idea
		File testFileToUpload = new File("src/test/resources/testFileToUpload.txt");
		if (!testFileToUpload.exists()) {
			System.out.println("Test file does not exist...");
		}
		// TODO what to return here?
		when(dbxClientMock.files().uploadBuilder(anyString()).uploadAndFinish(any())).thenReturn(null);
		doReturn(true).when(testee).doesFileExist(anyString());

		// when
		boolean result = testee.upload(testFileToUpload);

		// then
		verify(dbxClientMock.files().uploadBuilder("/testFileToUpload.txt")).uploadAndFinish(any(FileInputStream.class));
		verify(testee).doesFileExist("testFileToUpload.txt");
		assertTrue(result);
	}


	@Test
	public void testUpload_givenFileToUploadDoesNotExist() throws DbxException {
		// given
		DbxClientV2 dbxClientMock = mock(DbxClientV2.class, RETURNS_DEEP_STUBS); // Mock a fluent API object
		DropboxClient testee = spy(new DropboxClient(dbxClientMock));
		File nonExistingFile = new File("thisFileDoesNotExist.txt");

		// when
		boolean result = testee.upload(nonExistingFile);

		// then
		verifyNoInteractions(dbxClientMock);
		verify(testee, never()).doesFileExist(anyString());
		assertFalse(result);
	}


	@Test
	public void testUpload_failureBecauseDbxExceptionDuringUpload() throws DbxException, IOException {
		// given
		DbxClientV2 dbxClientMock = mock(DbxClientV2.class, RETURNS_DEEP_STUBS); // Mock a fluent API object
		DropboxClient testee = spy(new DropboxClient(dbxClientMock));

		// Mocking a file is not a good idea
		File testFileToUpload = new File("src/test/resources/testFileToUpload.txt");
		if (!testFileToUpload.exists()) {
			System.out.println("Test file does not exist...");
		}
		when(dbxClientMock.files().uploadBuilder(anyString()).uploadAndFinish(any())).thenThrow(DbxException.class);

		// when
		boolean result = testee.upload(testFileToUpload);

		// then
		verify(dbxClientMock.files().uploadBuilder("/testFileToUpload.txt")).uploadAndFinish(any(FileInputStream.class));
		verify(testee, never()).doesFileExist(anyString());
		assertFalse(result);
	}


	@Test
	public void testUpload_failureBecauseFileNotFoundAfterUpload() throws DbxException, IOException {
		// given
		DbxClientV2 dbxClientMock = mock(DbxClientV2.class, RETURNS_DEEP_STUBS); // Mock a fluent API object
		DropboxClient testee = spy(new DropboxClient(dbxClientMock));

		// Mocking a file is not a good idea
		File testFileToUpload = new File("src/test/resources/testFileToUpload.txt");
		if (!testFileToUpload.exists()) {
			System.out.println("Test file does not exist...");
		}
		when(dbxClientMock.files().uploadBuilder(anyString()).uploadAndFinish(any())).thenReturn(null);
		doReturn(false).when(testee).doesFileExist(anyString());

		// when
		boolean result = testee.upload(testFileToUpload);

		// then
		verify(dbxClientMock.files().uploadBuilder("/testFileToUpload.txt")).uploadAndFinish(any(FileInputStream.class));
		verify(testee).doesFileExist("testFileToUpload.txt");
		assertFalse(result);
	}


	@Test
	public void testDoesFileExist_true() throws DbxException {
		// given
		DbxClientV2 dbxClientMock = mock(DbxClientV2.class, RETURNS_DEEP_STUBS); // Mock a fluent API object
		DropboxClient testee = new DropboxClient(dbxClientMock);

		Metadata m1 = new Metadata("folder1");
		Metadata m2 = new Metadata("folder2");
		Metadata m3 = new Metadata("testFileToUpload.txt");
		List<Metadata> metadataList = List.of(m1, m2, m3);
		ListFolderResult listFolderResult = new ListFolderResult(metadataList, "3", true);
		when(dbxClientMock.files().listFolder(anyString())).thenReturn(listFolderResult);
		clearInvocations(dbxClientMock); // TODO wut?

		// when
		boolean result = testee.doesFileExist("testFileToUpload.txt");

		// then
		verify(dbxClientMock.files()).listFolder("");
		assertTrue(result);
	}


	@Test
	public void testDoesFileExist_false() throws DbxException {
		// given
		DbxClientV2 dbxClientMock = mock(DbxClientV2.class, RETURNS_DEEP_STUBS); // Mock a fluent API object
		DropboxClient testee = new DropboxClient(dbxClientMock);

		Metadata m1 = new Metadata("folder1");
		Metadata m2 = new Metadata("folder2");
		List<Metadata> metadataList = List.of(m1, m2);
		ListFolderResult listFolderResult = new ListFolderResult(metadataList, "2", true);

		when(dbxClientMock.files().listFolder(anyString())).thenReturn(listFolderResult);
		clearInvocations(dbxClientMock); // TODO wut?

		// when
		boolean result = testee.doesFileExist("testFileToUpload.txt");

		// then
		verify(dbxClientMock.files()).listFolder("");
		assertFalse(result);
	}

}
