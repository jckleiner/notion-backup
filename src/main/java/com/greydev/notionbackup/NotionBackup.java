package com.greydev.notionbackup;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class NotionBackup {

	public static final String KEY_DROPBOX_ACCESS_TOKEN = "DROPBOX_ACCESS_TOKEN";


	public static void main(String[] args) throws IOException, GeneralSecurityException {
		Dotenv dotenv = initDotenv();
		CookieStore cookieStore = new BasicCookieStore();
		CloseableHttpClient httpClient = initHttpClient(cookieStore);

		NotionClient notionClient = new NotionClient(dotenv, httpClient, cookieStore);
		File exportedFile = notionClient.export()
				.orElseThrow(() -> new IllegalStateException("Could not export notion file"));

		GoogleClient googleClient = new GoogleClient(dotenv);
		googleClient.upload(exportedFile);


		// TODO
		String dropboxAccessToken = dotenv.get(KEY_DROPBOX_ACCESS_TOKEN);
		if (StringUtils.isBlank(dropboxAccessToken)) {
			throw new IllegalArgumentException("The given accessToken is blank!");
		}
		DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/notion-backup").build();
		DbxClientV2 dbxClient = new DbxClientV2(config, dropboxAccessToken);
		DropboxClient dropboxClient = new DropboxClient(dbxClient);
		dropboxClient.upload(exportedFile);

		// TODO if export limit exceeded, print response
		// TODO publish github package with actions when new merge in master
		// TODO GitClient
		// TODO NexcloudClient
		// TODO With cron to local folder
		// TODO Docker
		// TODO give file name as param?
		// TODO add tests, mockito
		// TODO testing with okhttp's mock server?
		// TODO test where the download will go if the path is "exportFolder/", will the jar create it in the pwd?
		// TODO make a common interface, make a list, loop and call upload on each, make upload async
		// TODO best way to run async code in java?
		CompletableFuture.runAsync(() -> {
			// method call or code to be async.
		});
	}


	private static Dotenv initDotenv() {
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.ignoreIfMalformed()
				.load();
		if (dotenv == null) {
			throw new IllegalStateException("Could not load dotenv!");
		}
		return dotenv;
	}


	public static CloseableHttpClient initHttpClient(CookieStore cookieStore) {
		// Prevent warning 'Invalid cookie header' warning by setting a cookie spec
		// Also adding a cookie store to be able to access the cookies
		return HttpClients.custom()
				.setDefaultCookieStore(cookieStore)
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
				.build();
	}

}