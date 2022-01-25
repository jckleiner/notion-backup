package com.greydev.notionbackup.cloudstorage;

import java.io.File;


public interface CloudStorageClient {

	boolean upload(File fileToUpload);

}
