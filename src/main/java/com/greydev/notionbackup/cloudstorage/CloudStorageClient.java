package com.greydev.notionbackup.cloudstorage;

import java.io.File;


public interface CloudStorageClient {

	boolean upload(File fileToUpload);

	// TODO how to handle implementation specific Exceptions?
	// maybe a custom wrapper CloudStorageException or smth?
	boolean doesFileExist(String fileName) throws Exception;

}
