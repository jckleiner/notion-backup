package com.greydev.notionbackup.cloudstorage.dropbox;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class DropboxServiceFactory {

	public static Optional<DbxClientV2> create(String dropboxAccessToken) {
		if (StringUtils.isBlank(dropboxAccessToken)) {
			log.warn("The given dropboxAccessToken is blank.");
			return Optional.empty();
		}
		DbxClientV2 service = null;
		try {
			DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/notion-backup").build();
			service = new DbxClientV2(config, dropboxAccessToken);
		} catch (Exception e) {
			log.warn("An exception occurred: ", e);
		}
		return Optional.ofNullable(service);
	}
}
