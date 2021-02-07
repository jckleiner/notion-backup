package com.greydev.notionbackup;

import java.io.File;

import org.apache.commons.lang3.reflect.FieldUtils;

import com.dropbox.core.v2.DbxClientV2;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@Slf4j
public class DropboxClientTest {

	public static void main(String[] args) throws IllegalAccessException {

		// given
		// TODO Don't mock classes that you don't own! -> Then instantiate them?
		Dotenv dotenvMock = mock(Dotenv.class);
		DbxClientV2 dropboxClientMock = mock(DbxClientV2.class);
		File file = new File("someFile");

		// TODO how to mock method call chains?

		// when
		when(dotenvMock.get(any())).thenReturn("some string");

		// This method will invoke dotenvMock.get(...);
		DropboxClient testee = new DropboxClient(dotenvMock);
		FieldUtils.writeField(testee, "dropboxClient", dropboxClientMock, true);
		testee.upload(file);

		// then
		verify(dotenvMock).get(DropboxClient.KEY_DROPBOX_ACCESS_TOKEN);

	}

}
