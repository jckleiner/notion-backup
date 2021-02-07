package com.greydev.notionbackup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class DropboxClient {

	public static final String KEY_DROPBOX_ACCESS_TOKEN = "DROPBOX_ACCESS_TOKEN";

	private final String dropboxAccessToken;
	private DbxClientV2 dropboxClient;


	DropboxClient(Dotenv dotenv) {
		dropboxAccessToken = dotenv.get(KEY_DROPBOX_ACCESS_TOKEN);

		// TODO how to handle it better if env vars not provided?
		if (StringUtils.isBlank(dropboxAccessToken)) {
			log.info("Cannot instantiate instance because {} is empty", KEY_DROPBOX_ACCESS_TOKEN);
		} else {
			DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/notion-backup").build();
			dropboxClient = new DbxClientV2(config, dropboxAccessToken);
		}
	}


	public void upload(File fileToUpload) {
		if (StringUtils.isBlank(dropboxAccessToken)) {
			log.info("Skipping upload to Dropbox because {} is empty", KEY_DROPBOX_ACCESS_TOKEN);
			return;
		}
		// This does not override if it's the same file with the same name and silently executes
		// Throws an UploadErrorException if we try to upload a different file with an already existing name
		log.info("Uploading file '{}' to Dropbox...", fileToUpload.getName());
		try (InputStream in = new FileInputStream(fileToUpload)) {
			// without slash: IllegalArgumentException: String 'path' does not match pattern
			dropboxClient.files().uploadBuilder("/" + fileToUpload.getName()).uploadAndFinish(in);

			if (doesFileExist(fileToUpload.getName())) {
				log.info("Successfully uploaded '{}' to Dropbox", fileToUpload.getName());
			} else {
				log.warn("Could not upload '{}' to Dropbox", fileToUpload.getName());
			}
		} catch (IOException | DbxException e) {
			log.warn("Exception during upload of file '{}'", fileToUpload.getName(), e);
		}
	}


	private boolean doesFileExist(String fileName) throws DbxException {
		ListFolderResult result = dropboxClient.files().listFolder("");
		return result.getEntries().stream()
				.anyMatch(entry -> StringUtils.equalsIgnoreCase(entry.getName(), fileName));
	}

}
