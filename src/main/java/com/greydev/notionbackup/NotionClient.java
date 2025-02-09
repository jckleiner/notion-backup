package com.greydev.notionbackup;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class NotionClient {

	private static final int FETCH_DOWNLOAD_URL_RETRY_SECONDS = 5;

	private static final String ENQUEUE_ENDPOINT = "https://www.notion.so/api/v3/enqueueTask";
	private static final String NOTIFICATION_ENDPOINT = "https://www.notion.so/api/v3/getNotificationLogV2";
	private static final String TOKEN_V2 = "token_v2";
	private static final String EXPORT_FILE_NAME = "notion-export";
	private static final String EXPORT_FILE_EXTENSION = ".zip";

	private static final String KEY_DOWNLOADS_DIRECTORY_PATH = "DOWNLOADS_DIRECTORY_PATH";
	private static final String KEY_NOTION_SPACE_ID = "NOTION_SPACE_ID";
	private static final String KEY_NOTION_TOKEN_V2 = "NOTION_TOKEN_V2";
	private static final String KEY_NOTION_EXPORT_TYPE = "NOTION_EXPORT_TYPE";
	private static final String KEY_NOTION_FLATTEN_EXPORT_FILETREE = "NOTION_FLATTEN_EXPORT_FILETREE";
	private static final String KEY_NOTION_EXPORT_COMMENTS = "NOTION_EXPORT_COMMENTS";
	private static final String DEFAULT_NOTION_EXPORT_TYPE = "markdown";
	private static final boolean DEFAULT_NOTION_FLATTEN_EXPORT_FILETREE = false;
	private static final boolean DEFAULT_NOTION_EXPORT_COMMENTS = true;
	private static final String DEFAULT_DOWNLOADS_PATH = "/downloads";

	private final String notionSpaceId;
	private final String notionTokenV2;
	private final String exportType;
	private final boolean flattenExportFileTree;
	private final boolean exportComments;
	private String downloadsDirectoryPath;

	private final HttpClient client;
	private final ObjectMapper objectMapper = new ObjectMapper();


	NotionClient(Dotenv dotenv) {
		this.client = HttpClient.newBuilder().build();

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

		exportType = StringUtils.isNotBlank(dotenv.get(KEY_NOTION_EXPORT_TYPE)) ? dotenv.get(KEY_NOTION_EXPORT_TYPE) : DEFAULT_NOTION_EXPORT_TYPE;
		flattenExportFileTree = StringUtils.isNotBlank(dotenv.get(KEY_NOTION_FLATTEN_EXPORT_FILETREE)) ?
				Boolean.parseBoolean(dotenv.get(KEY_NOTION_FLATTEN_EXPORT_FILETREE)) :
				DEFAULT_NOTION_FLATTEN_EXPORT_FILETREE;
		exportComments = StringUtils.isNotBlank(dotenv.get(KEY_NOTION_EXPORT_COMMENTS)) ?
				Boolean.parseBoolean(dotenv.get(KEY_NOTION_EXPORT_COMMENTS)) :
				DEFAULT_NOTION_EXPORT_COMMENTS;

		exitIfRequiredEnvVariablesNotValid();

		log.info("Using export type: {}", exportType);
		log.info("Flatten export file tree: {}", flattenExportFileTree);
		log.info("Export comments: {}", exportComments);
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
			long exportTriggerTimestamp = System.currentTimeMillis();
			// TODO - taskId is not really needed anymore
			Optional<String> taskId = triggerExportTask();

			if (taskId.isEmpty()) {
				log.info("taskId could not be extracted");
				return Optional.empty();
			}
			log.info("taskId extracted");

			Optional<String> downloadLink = fetchDownloadUrl(exportTriggerTimestamp);
			if (downloadLink.isEmpty()) {
				log.info("downloadLink could not be extracted");
				return Optional.empty();
			}
			log.info("Download link extracted");

			log.info("Downloading file...");
			String fileName = String.format("%s-%s%s_%s%s",
					EXPORT_FILE_NAME,
					exportType,
					flattenExportFileTree ? "-flattened" : "",
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
			client.send(request, HttpResponse.BodyHandlers.ofFile(downloadPath));
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

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		JsonNode responseJsonNode = objectMapper.readTree(response.body());

		/*	This will be the response if the given token is not valid anymore (for example if a logout occurred)
			{
				"errorId": "<some-UUID>",
				"name":"UnauthorizedError",
				"message":"Token was invalid or expired.",
				"clientData":{"type":"login_try_again"}
			}
		 */
		if (responseJsonNode.get("taskId") == null) {
			JsonNode errorName = responseJsonNode.get("name");
			log.error("Error name: {}, error message: {}", errorName, responseJsonNode.get("message"));
			if (StringUtils.equalsIgnoreCase(errorName.toString(), "UnauthorizedError")) {
				log.error("UnauthorizedError: seems like your token is not valid anymore. Try to log in to Notion again and replace you old token.");
			}
			return Optional.empty();
		}

		return Optional.of(responseJsonNode.get("taskId").asText());
	}

	private Optional<String> fetchDownloadUrl(long exportTriggerTimestamp) throws IOException, InterruptedException {
		try {
			for (int i = 0; i < 500; i++) {
				sleep(FETCH_DOWNLOAD_URL_RETRY_SECONDS);

				HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create(NOTIFICATION_ENDPOINT))
						.header("Cookie", TOKEN_V2 + "=" + notionTokenV2)
						.header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString(getNotificationJson()))
						.build();

				HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
				JsonNode rootNode = objectMapper.readTree(response.body());

				JsonNode node = rootNode.path("recordMap");
				node = node.path("activity");
				node = node.fields().next().getValue();
				node = node.path("value");

				long notificationStartTimestamp = Long.parseLong(node.path("start_time").asText());

				// we want the notification newer than the export trigger timestamp
				// since the Notion response also contains older export trigger notifications
				if (notificationStartTimestamp < exportTriggerTimestamp) {
					log.info("The newest export trigger notification is still not in the Notion response. " +
							"Trying again in {} seconds...", FETCH_DOWNLOAD_URL_RETRY_SECONDS);
					continue;
				}
				log.info("Found a new export trigger notification in the Notion response. " +
						"Attempting to extract the download URL. " +
						"Timestamp of when the export was triggered: {}. " +
						"Timestamp of the notification: {}", exportTriggerTimestamp, notificationStartTimestamp);

				node = node.path("edits");
				node = node.get(0);
				JsonNode exportActivity = node.path("link");

				if (exportActivity.isMissingNode()) {
					log.info("The download URL is not yet present. Trying again in {} seconds...", FETCH_DOWNLOAD_URL_RETRY_SECONDS);
					continue;
				}
				return Optional.of(exportActivity.textValue());
			}
		}
		catch (Exception e) {
			log.error("An exception occurred: ", e);
		}
		return Optional.empty();
	}

	private String getTaskJson() {
		String taskJsonTemplate = "{" +
				"  \"task\": {" +
				"    \"eventName\": \"exportSpace\"," +
				"    \"request\": {" +
				"      \"spaceId\": \"%s\"," +
				"      \"shouldExportComments\": %s," +
				"      \"exportOptions\": {" +
				"        \"exportType\": \"%s\"," +
				"        \"flattenExportFiletree\": %s," +
				"        \"timeZone\": \"Europe/Berlin\"," +
				"        \"locale\": \"en\"" +
				"      }" +
				"    }" +
				"  }" +
				"}";
		return String.format(taskJsonTemplate, notionSpaceId, exportComments, exportType.toLowerCase(), flattenExportFileTree);
	}

	private String getNotificationJson() {
		String notificationJsonTemplate = "{" +
				"  \"spaceId\": \"%s\"," +
				"  \"size\": 20," +
				"  \"type\": \"unread_and_read\"," +
				"  \"variant\": \"no_grouping\"" +
				"}";
		return String.format(notificationJsonTemplate, notionSpaceId);
	}

	private void exit(String message) {
		log.error(message);
		System.exit(1);
	}


	private void sleep(int seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
			log.error("An exception occurred: ", e);
			Thread.currentThread().interrupt();
		}
	}

}
