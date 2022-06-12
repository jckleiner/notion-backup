package com.greydev.notionbackup;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greydev.notionbackup.model.Result;
import com.greydev.notionbackup.model.Results;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class NotionClient {

	private static final String GET_TASKS_ENDPOINT = "https://www.notion.so/api/v3/getTasks";
	private static final String ENQUEUE_ENDPOINT = "https://www.notion.so/api/v3/enqueueTask";
	private static final String TOKEN_V2 = "token_v2";
	private static final String EXPORT_FILE_NAME = "notion-export";
	private static final String EXPORT_FILE_EXTENSION = ".zip";

	private static final String KEY_DOWNLOADS_DIRECTORY_PATH = "DOWNLOADS_DIRECTORY_PATH";
	private static final String KEY_NOTION_SPACE_ID = "NOTION_SPACE_ID";
	private static final String KEY_NOTION_EXPORT_TYPE = "NOTION_EXPORT_TYPE";
	private static final String KEY_NOTION_TOKEN_V2 = "NOTION_TOKEN_V2";
	private static final String DEFAULT_NOTION_EXPORT_TYPE = "markdown";
	private static final String DEFAULT_DOWNLOADS_PATH = "/downloads";

	public static final int SLEEP_INTERVAL_IN_MS = 6000;

	private final String notionSpaceId;
	private final String notionTokenV2;
	private final String exportType;
	private String downloadsDirectoryPath;

	private final HttpClient newClient;
	private final ObjectMapper objectMapper = new ObjectMapper();


	NotionClient(Dotenv dotenv) {
		this.newClient = HttpClient.newBuilder().build();

		// both environment variables and variables defined in the .env file can be accessed this way
		notionSpaceId = dotenv.get(KEY_NOTION_SPACE_ID);
		notionTokenV2 = dotenv.get(KEY_NOTION_TOKEN_V2);
		downloadsDirectoryPath = dotenv.get(KEY_DOWNLOADS_DIRECTORY_PATH);

		if (StringUtils.isBlank(downloadsDirectoryPath)) {
			log.info("{} is not set. Downloads will be saved to: {} ", KEY_DOWNLOADS_DIRECTORY_PATH, DEFAULT_DOWNLOADS_PATH);
			downloadsDirectoryPath = DEFAULT_DOWNLOADS_PATH;
		} else {
			log.info("Downloads will be saved to: {} ", downloadsDirectoryPath);
		}

		if (!Files.isDirectory(Path.of(downloadsDirectoryPath))) {
			exit(String.format("Download folder '%s' does not exist or is not a folder! Make sure this folder is present.", downloadsDirectoryPath));
		}
		if (!Files.isReadable(Path.of(downloadsDirectoryPath))) {
			exit(String.format("No read permission for folder '%s'!", downloadsDirectoryPath));
		}
		if (!Files.isWritable(Path.of(downloadsDirectoryPath))) {
			exit(String.format("No write permission for folder '%s'!", downloadsDirectoryPath));
		}

		exportType = StringUtils.isNotBlank(dotenv.get(KEY_NOTION_EXPORT_TYPE))
				? dotenv.get(KEY_NOTION_EXPORT_TYPE).toLowerCase()
				: DEFAULT_NOTION_EXPORT_TYPE.toLowerCase();

		exitIfRequiredEnvVariablesNotValid();

		log.info("Using export type: {}", exportType);
	}


	private void exitIfRequiredEnvVariablesNotValid() {
		if (StringUtils.isBlank(notionSpaceId)) {
			exit(KEY_NOTION_SPACE_ID + " is missing!");
		}
		if (StringUtils.isBlank(notionTokenV2)) {
			exit(KEY_NOTION_TOKEN_V2 + " is missing!");
		}
	}


	public Optional<File> export() {
		try {
			Optional<String> taskIdOptional = triggerExportTask();
			if (taskIdOptional.isEmpty()) {
				log.error("Could not extract taskId");
				return Optional.empty();
			}
			log.info("taskId extracted");

			Optional<String> downloadLink = getDownloadLink(taskIdOptional.get());
			if (downloadLink.isEmpty()) {
				log.info("downloadLink could not be extracted");
				return Optional.empty();
			}
			log.info("Download link extracted");

			log.info("Downloading file...");
			String fileName = String.format("%s-%s_%s%s",
					EXPORT_FILE_NAME,
					exportType,
					LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")),
					EXPORT_FILE_EXTENSION);

			log.info("Downloaded export will be saved to: " + downloadsDirectoryPath);
			log.info("fileName: " + fileName);
			Path downloadPath = Path.of(downloadsDirectoryPath, fileName);
			Optional<File> downloadedFile = downloadToFile(downloadLink.get(), downloadPath);

			if (downloadedFile.isEmpty() || !downloadedFile.get().isFile()) {
				log.info("Could not download file");
				return Optional.empty();
			}

			log.info("Download finished: {}", downloadedFile.get().getName());
			return downloadedFile;
		} catch (IOException | InterruptedException e) {
			log.warn("Exception during export", e);
		}
		return Optional.empty();
	}


	private Optional<File> downloadToFile(String url, Path downloadPath) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.GET()
				.build();

		try {
			log.info("Downloading file to: '{}'", downloadPath);
			newClient.send(request, HttpResponse.BodyHandlers.ofFile(downloadPath));
			return Optional.of(downloadPath.toFile());
		} catch (IOException | InterruptedException e) {
			log.warn("Exception during file download", e);
			return Optional.empty();
		}
	}


	private Optional<String> triggerExportTask() throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(ENQUEUE_ENDPOINT))
				.header("Cookie", TOKEN_V2 + "=" + notionTokenV2)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(getTaskJson()))
				.build();

		HttpResponse<String> response = newClient.send(request, HttpResponse.BodyHandlers.ofString());

		JsonNode responseJsonNode = objectMapper.readTree(response.body());

		if (response.statusCode() == 401) {
			log.error("Notion API returned a 401 (Unauthorized) response with the message: '{}'. Try to login and get a new token_v2.",
					responseJsonNode.get("message"));
			return Optional.empty();
		}

		if (response.statusCode() != 200) {
			log.error("Notion API returned a {} response with the message: '{}'.",
					response.statusCode(), responseJsonNode.get("message"));
			return Optional.empty();
		}

		return Optional.of(responseJsonNode.get("taskId").asText());
	}


	private Optional<String> getDownloadLink(String taskId) throws IOException, InterruptedException {
		String postBody = String.format("{\"taskIds\": [\"%s\"]}", taskId);

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(GET_TASKS_ENDPOINT))
				.header("Cookie", TOKEN_V2 + "=" + notionTokenV2)
				.header("Content-Type", "application/json")
				.timeout(Duration.ofSeconds(10))
				.POST(HttpRequest.BodyPublishers.ofString(postBody))
				.build();

		for (int i = 0; i < 100; i++) {
			HttpResponse<String> response = newClient.send(request, HttpResponse.BodyHandlers.ofString());
			Results results = objectMapper.readValue(response.body(), Results.class);

			if (!results.getResults().isEmpty()) {
				Result result = results.getResults().stream().findFirst().get();

				if (result.getStatus() != null) {
					log.info("Notion API workspace export 'state': '{}', Pages exported so far: {}", result.getState(), result.getStatus().getPagesExported());

					if (StringUtils.isNotBlank(result.getStatus().getExportUrl())) {
						log.info("Notion response now contains the export url. 'state': '{}', Pages exported so far: {}, Status.type: {}",
								result.getState(), result.getStatus().getPagesExported(), result.getStatus().getType());
					}
				}

				if (result.isSuccess()) {
					log.info("Notion API workspace export 'state': '{}', Pages exported so far: {}", result.getState(), result.getStatus().getPagesExported());
					return Optional.of(result.getStatus().getExportUrl());
				}
				if (result.isFailure()) {
					log.error("Notion export failed. Error message: '{}'", result.getError());
					return Optional.empty();
				}
			}

			sleep(SLEEP_INTERVAL_IN_MS);
		}

		log.info("Notion workspace export failed. After 10 minutes, the export status from the Notion API response was still not 'success'");
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
		if (StringUtils.isNotBlank(message)) {
			log.error(message);
		}
		System.exit(1);
	}


	private void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			log.error("An exception occurred: ", e);
		}
	}

}
