package com.greydev.notionbackup.cloudstorage.pcloud;

import com.pcloud.sdk.ApiClient;
import com.pcloud.sdk.Authenticators;
import com.pcloud.sdk.PCloudSdk;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class PCloudApiClientFactory {

	public static Optional<ApiClient> create(String accessToken, String apiHost) {
		ApiClient client = null;
		try {
			client = PCloudSdk.newClientBuilder()
					.authenticator(Authenticators.newOauthAuthenticator(() -> accessToken))
					.apiHost(apiHost)
					.create();
		} catch (Exception e) {
			log.warn("An exception occurred: ", e);
		}
		return Optional.ofNullable(client);
	}
}
