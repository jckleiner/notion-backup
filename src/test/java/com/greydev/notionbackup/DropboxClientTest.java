package com.greydev.notionbackup;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.reflect.FieldUtils;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@Slf4j
public class DropboxClientTest {

	public static void main(String[] args) throws IOException, DbxException, IllegalAccessException {

		// given
		// TODO Don't mock classes that you don't own! -> Then instantiate them?
		Dotenv dotenvMock = mock(Dotenv.class);
		FileMetadata fileMetadataMock = mock(FileMetadata.class);
		DbxClientV2 dbxClientMock = mock(DbxClientV2.class, RETURNS_DEEP_STUBS);
		DropboxClient dropboxClientMock = mock(DropboxClient.class);
		// Mocking a file is not a good idea
		File testFileToUpload = new File("src/test/resources/testFileToUpload.txt");
		if (!testFileToUpload.exists()) {
			System.out.println("FILE DOES NOT EXIST........");
		}

		when(dotenvMock.get(any())).thenReturn("some string");
		when(dbxClientMock.files().uploadBuilder(anyString()).uploadAndFinish(any()))
				.thenReturn(fileMetadataMock);
		when(dropboxClientMock.doesFileExist(anyString())).thenReturn(true);

		// TODO how to mock method call chains?

		// when
		// This method will invoke dotenvMock.get(...);
		DropboxClient testee = new DropboxClient(dotenvMock);
		// This is bad. We ignore the logic inside the constructor
		FieldUtils.writeField(testee, "dbxClient", dbxClientMock, true);
		testee.upload(testFileToUpload);

		// then
		verify(dotenvMock).get(DropboxClient.KEY_DROPBOX_ACCESS_TOKEN);
		verify(dbxClientMock, times(3)).files();
		// new FileInputStream(testFileToUpload)
		verify(dbxClientMock, times(3)).files().uploadBuilder("/testFileToUpload.txt").uploadAndFinish(any());
		verify(dropboxClientMock).doesFileExist("testFileToUpload.txt");

	}

}
