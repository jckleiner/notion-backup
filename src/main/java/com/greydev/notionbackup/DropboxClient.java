package com.greydev.notionbackup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class DropboxClient {

	/*
	When you want to be able to define the value of a private field, you have two options:
		1. The first one is to create a constructor that receives the value to be set and
		2. The second one is to create a setter for such private field.
 	*/
	private DbxClientV2 dbxClient;

	// TODO naming dbxClient
	// TODO how to test constructor?


	DropboxClient(DbxClientV2 dbxClient) {
		this.dbxClient = dbxClient;
	}


	/**
	 * Uploads a given file to the users Dropbox instance.
	 *
	 * @param fileToUpload file to upload
	 * @return true if the upload was successful, false otherwise.
	 */
	public boolean upload(File fileToUpload) {
		log.info("Uploading file '{}' to Dropbox...", fileToUpload.getName());
		try (InputStream in = new FileInputStream(fileToUpload)) {
			// This method not override if it's the same file with the same name and silently executes
			// Throws an UploadErrorException if we try to upload a different file with an already existing name
			// without slash: IllegalArgumentException: String 'path' does not match pattern
			dbxClient.files().uploadBuilder("/" + fileToUpload.getName()).uploadAndFinish(in);

			if (doesFileExist(fileToUpload.getName())) {
				log.info("Successfully uploaded '{}' to Dropbox", fileToUpload.getName());
				return true;
			} else {
				log.warn("Could not upload '{}' to Dropbox", fileToUpload.getName());
			}
		} catch (IOException | DbxException e) {
			log.warn("Exception during upload of file '{}'", fileToUpload.getName(), e);
		}
		return false;
	}


	public boolean doesFileExist(String fileName) throws DbxException {
		ListFolderResult result = dbxClient.files().listFolder("");
		return result.getEntries().stream()
				.anyMatch(entry -> StringUtils.equalsIgnoreCase(entry.getName(), fileName));
	}
}
