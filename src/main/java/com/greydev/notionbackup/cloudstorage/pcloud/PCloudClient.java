package com.greydev.notionbackup.cloudstorage.pcloud;

import com.greydev.notionbackup.cloudstorage.CloudStorageClient;
import com.pcloud.sdk.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

@Slf4j
public class PCloudClient implements CloudStorageClient {

	private final ApiClient apiClient;
	private final long folderId;

	public PCloudClient(ApiClient apiClient, long folderId) {
		this.apiClient = apiClient;
		this.folderId = folderId;
	}

	@Override
	public boolean upload(File fileToUpload) {
		try {
			apiClient.createFile(folderId, fileToUpload.getName(), DataSource.create(fileToUpload)).execute();
			log.info("pCloud: successfully uploaded {}", fileToUpload.getName());
			return true;
		} catch (IOException | ApiError e) {
			log.warn("pCloud: exception during upload of file '{}'", fileToUpload.getName(), e);
		}
		return false;
	}
}
