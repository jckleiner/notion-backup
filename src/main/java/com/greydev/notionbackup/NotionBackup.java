package com.greydev.notionbackup;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;

import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class NotionBackup {

	public static void main(String[] args) throws IOException, GeneralSecurityException {
		Dotenv dotenv = initDotenv();
		CookieStore cookieStore = new BasicCookieStore();
		CloseableHttpClient httpClient = initHttpClient(cookieStore);

		NotionClient notionClient = new NotionClient(dotenv, httpClient, cookieStore);
		File exportedFile = notionClient.export()
				.orElseThrow(() -> new IllegalStateException("Could not export notion file"));

		// TODO if google enabled
		//		GoogleClient googleClient = new GoogleClient(dotenv);
		//		googleClient.upload(exportedFile);
		//		DropboxClient dropboxClient = new DropboxClient(dotenv);
		//		dropboxClient.upload(exportedFile);

		// TODO GitClient
		// TODO NexcloudClient
		// TODO With cron to local folder
		// TODO give file name as param?
		// TODO add tests, mockito
		// TODO testing with okhttp's mock server?
		// TODO test where the download will go if the path is "exportFolder/", will the jar create it in the pwd?
		// TODO make a common interface, make a list, loop and call upload on each, make upload async
		// TODO best way to run async code in java?
		CompletableFuture.runAsync(() -> {
			// method call or code to be asynch.
		});

		//		NotionBackup notionBackup = new NotionBackup();
		//		notionBackup.uploadNotionExportToGDrive();
		//		notionBackup.start();
	}


	private static Dotenv initDotenv() {
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.ignoreIfMalformed()
				.load();
		if (dotenv == null) {
			log.error("Could not load dotenv!");
			System.exit(1);
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