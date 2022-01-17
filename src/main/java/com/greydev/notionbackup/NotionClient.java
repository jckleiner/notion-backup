package com.greydev.notionbackup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.protocol.HTTP;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.api.client.http.HttpStatusCodes;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;


@Slf4j
public class NotionClient {

	private static final String GET_TASKS_ENDPOINT = "https://www.notion.so/api/v3/getTasks";
	private static final String ENQUEUE_ENDPOINT = "https://www.notion.so/api/v3/enqueueTask";
	private static final String LOGIN_ENDPOINT = "https://www.notion.so/api/v3/loginWithEmail";
	private static final String TOKEN_V2 = "token_v2";
	private static final String EXPORT_FILE_NAME = "notion-export";
	private static final String EXPORT_FILE_EXTENSION = ".zip";

	private static final String KEY_NOTION_SPACE_ID = "NOTION_SPACE_ID";
	private static final String KEY_NOTION_EMAIL = "NOTION_EMAIL";
	private static final String KEY_NOTION_PASSWORD = "NOTION_PASSWORD";
	private static final String KEY_EXPORT_TYPE = "EXPORT_TYPE";
	private static final String DEFAULT_EXPORT_TYPE = "markdown";

	private final String notionSpaceId;
	private final String notionEmail;
	private final String notionPassword;
	private final String exportType;

	private final HttpClient newClient;
	private final CookieManager cookieManager;
	private final ObjectMapper objectMapper = new ObjectMapper();


	NotionClient(Dotenv dotenv) {
		this.cookieManager = new CookieManager();
		CookieHandler.setDefault(this.cookieManager);
		this.newClient = HttpClient.newBuilder().cookieHandler(this.cookieManager).build();

		// both environment variables and variables defined in the .env file can be accessed this way
		notionSpaceId = dotenv.get(KEY_NOTION_SPACE_ID);
		notionEmail = dotenv.get(KEY_NOTION_EMAIL);
		notionPassword = dotenv.get(KEY_NOTION_PASSWORD);
		exportType = StringUtils.isNotBlank(dotenv.get(KEY_EXPORT_TYPE)) ? dotenv.get(KEY_EXPORT_TYPE) : DEFAULT_EXPORT_TYPE;
		log.info("Using export type: {}", exportType);

		exitIfRequiredEnvVariablesNotSet();
	}


	private void exitIfRequiredEnvVariablesNotSet() {
		if (StringUtils.isBlank(notionSpaceId)) {
			exit(KEY_NOTION_SPACE_ID + " is missing!");
		}
		if (StringUtils.isBlank(notionEmail)) {
			exit(KEY_NOTION_EMAIL + " is missing!");
		}
		if (StringUtils.isBlank(notionPassword)) {
			exit(KEY_NOTION_PASSWORD + " is missing!");
		}
	}


	public Optional<File> export() {
		File downloadedFile = null;
		try {
			Optional<String> tokenV2 = getTokenV2();
			if (tokenV2.isEmpty()) {
				log.info("tokenV2 could not be extracted");
				return Optional.empty();
			}
			// TODO delete token log
			log.info("tokenV2 extracted: " + tokenV2);

			String taskId = triggerExportTask(tokenV2.get());
			// TODO delete token log
			log.info("taskId extracted: " + taskId);

			Optional<String> downloadLink = getDownloadLink(taskId, tokenV2.get());
			if (downloadLink.isEmpty()) {
				log.info("downloadLink could not be extracted");
				return Optional.empty();
			}
			log.info("downloadLink extracted: " + downloadLink.get());

			log.info("Downloading file...");
			String fileName = String.format("%s-%s_%s%s",
					EXPORT_FILE_NAME,
					exportType,
					LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")),
					EXPORT_FILE_EXTENSION);
			// This will override the already existing file
			downloadedFile = downloadToFile(downloadLink.get(), new java.io.File(fileName));
			// downloadedFile = downloadToFile(downloadLink, new java.io.File(fileName));
			log.info("Download finished: {}", downloadedFile.getName());
		} catch (IOException | InterruptedException e) {
			log.warn("Exception during export", e);
		}
		return Optional.ofNullable(downloadedFile);
	}


	private File downloadToFile(String url, File destinationFile) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.GET()
				.build();

		newClient.send(request, HttpResponse.BodyHandlers.ofFile(destinationFile.toPath()));

		return destinationFile;
	}


	// TODO create gist
	private Optional<String> getTokenV2() throws IOException, InterruptedException {
		String credentialsTemplate = "{" +
				"\"email\": \"%s\"," +
				"\"password\": \"%s\"" +
				"}";
		String credentialsJson = String.format(credentialsTemplate, notionEmail, notionPassword);

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(LOGIN_ENDPOINT))
				.POST(HttpRequest.BodyPublishers.ofString(credentialsJson))
				.header("Content-Type", "application/json")
				.build();

		HttpResponse<String> response = newClient.send(request, HttpResponse.BodyHandlers.ofString());
		System.out.println("response: " + response);

		if (response.statusCode() == 429) {
			log.warn("Too many requests were sent. Notion is returning a 429 status code (Too many requests).");
			return Optional.empty();
		}

		return Optional.of(cookieManager.getCookieStore()
				.get(URI.create(LOGIN_ENDPOINT))
				.stream()
				.filter(cookie -> TOKEN_V2.equals(cookie.getName()))
				.findFirst()
				.orElseThrow()
				.getValue());

	}


	private String triggerExportTask(String tokenV2) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(ENQUEUE_ENDPOINT))
				.header("Cookie", TOKEN_V2 + "=" + tokenV2)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(getTaskJson()))
				.build();

		HttpResponse<String> response = newClient.send(request, HttpResponse.BodyHandlers.ofString());

		JsonNode responseJsonNode = objectMapper.readTree(response.body());
		return responseJsonNode.get("taskId").asText();
	}


	private Optional<String> getDownloadLink(String taskId, String tokenV2) throws IOException, InterruptedException {
		String postBody = String.format("{\"taskIds\": [\"%s\"]}", taskId);

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(GET_TASKS_ENDPOINT))
				.header("Cookie", TOKEN_V2 + "=" + tokenV2)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(postBody))
				.build();

		for (int i = 0; i < 20; i++) {
			HttpResponse<String> response = newClient.send(request, HttpResponse.BodyHandlers.ofString());
			System.out.println("response.body(): " + response.body());

			// TODO Need to prepare Jackson Document and see how this is handled. I don't wan't this wrapper "Results" class
			Results results = objectMapper.readValue(response.body(), Results.class);

			if (!results.getResults().isEmpty()) {
				Result result = results.getResults().stream().findFirst().get();
				log.info("state: " + result.getState());

				if (result.getStatus() != null) {
					log.info("pagesExported: " + result.getStatus().getPagesExported());
				}

				if (result.isSuccess()) {
					return Optional.of(result.getStatus().getExportUrl());
				}
			}

			sleep(4000);
		}
		return Optional.empty();
	}


	private String getTaskJson() {
		String taskJsonTemplate = "{" +
				"  \"task\": {" +
				"    \"eventName\": \"exportSpace\"," +
				"    \"request\": {" +
				"      \"spaceId\": \"%s\"," +
				"      \"exportOptions\": {" +
				"        \"exportType\": \"%s\"," +
				"        \"timeZone\": \"Europe/Berlin\"," +
				"        \"locale\": \"en\"" +
				"      }" +
				"    }" +
				"  }" +
				"}";
		return String.format(taskJsonTemplate, notionSpaceId, exportType);
	}


	private void exit(String message) {
		log.error(message);
		System.exit(1);
	}


	private void sleep(int ms) {
		try {
			log.info("sleeping for {}ms", ms);
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			log.error("An exception occurred: ", e);
		}
	}

}
