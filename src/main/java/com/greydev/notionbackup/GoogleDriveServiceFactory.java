package com.greydev.notionbackup;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class GoogleDriveServiceFactory {

	private static final String OAUTH_SCOPE_GOOGLE_DRIVE = "https://www.googleapis.com/auth/drive";
	// TODO do we need an application name?
	private static final String APPLICATION_NAME = "NOTION-BACKUP";


	private GoogleDriveServiceFactory() {
		// can't instantiate
	}


	public static Optional<Drive> create(String googleDriveServiceAccountSecret) {
		//Build service account credential
		// We need to set a scope?
		// https://developers.google.com/identity/protocols/oauth2/scopes
		Drive drive = null;
		try {
			GoogleCredentials googleCredentials = GoogleCredentials
					.fromStream(new ByteArrayInputStream(googleDriveServiceAccountSecret.getBytes()))
					.createScoped(Collections.singletonList(OAUTH_SCOPE_GOOGLE_DRIVE));
			drive = new Drive
					.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), new HttpCredentialsAdapter(googleCredentials))
					.setApplicationName(APPLICATION_NAME)
					.build();
		} catch (IOException | GeneralSecurityException e) {
			log.error("TODO", e);
		}
		return Optional.ofNullable(drive);
	}

}
