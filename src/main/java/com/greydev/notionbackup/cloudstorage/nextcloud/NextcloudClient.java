package com.greydev.notionbackup.cloudstorage.nextcloud;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.greydev.notionbackup.cloudstorage.CloudStorageClient;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class NextcloudClient implements CloudStorageClient {

	private final String email;
	private final String password;
	private final String webdavUrl;


	public NextcloudClient(String email, String password, String webdavUrl) {
		this.email = email;
		this.password = password;
		this.webdavUrl = webdavUrl;

	}


	@Override
	public boolean upload(File fileToUpload) {
		log.info("Nextcloud: uploading file '{}' ...", fileToUpload.getName());

		try {
			HttpResponse<String> response = uploadFileToNextcloud(fileToUpload);

			if (response.statusCode() == 201) {
				log.info("Nextcloud: successfully uploaded '{}'", fileToUpload.getName());
			} else if (response.statusCode() == 204) {
				log.info("Nextcloud: file upload response code is {}). " +
						"This probably means a file with the same name already exists and it was overwritten/replaced.", response.statusCode());
			} else if (response.statusCode() == 404) {
				log.info("Nextcloud: file upload response code is {}. The path you provided was not found.", response.statusCode());
			} else {
				log.info("Nextcloud: Unknown Nextcloud response code: '{}'", response.statusCode());
			}

		} catch (IOException | InterruptedException e) {
			log.error("Nextcloud Exception: ", e);
		}

		return false;
	}


	private HttpResponse<String> uploadFileToNextcloud(File fileToUpload) throws IOException, InterruptedException {
		HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.authenticator(new Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(email, password.toCharArray());
					}
				})
				.version(HttpClient.Version.HTTP_1_1)
				.build();

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(webdavUrl.endsWith("/") ? (webdavUrl + fileToUpload.getName()) : webdavUrl))
				.PUT(HttpRequest.BodyPublishers.ofFile(fileToUpload.toPath()))
				.version(HttpClient.Version.HTTP_1_1)
				.build();

		// Return code is 201 when the file was successfully uploaded
		// Return code is 204 when the file already exists (file will be overwritten/replaced)
		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

}
