package com.greydev.notionbackup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final OkHttpClient okHttpClient;
	private final CookieJar cookieJar;
	private static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8");


	NotionClient(Dotenv dotenv) {

		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		this.cookieJar = new JavaNetCookieJar(cookieManager);
		this.okHttpClient = new OkHttpClient.Builder().cookieJar(cookieJar).build();

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
			String tokenV2 = getTokenV2();
			log.info("tokenV2 extracted");

			String taskId = triggerExportTask(tokenV2);
			log.info("taskId extracted");

			String downloadLink = getDownloadLink(taskId, tokenV2);
			log.info("downloadLink extracted");

			log.info("Downloading file...");
			String fileName = String.format("%s-%s_%s%s",
					EXPORT_FILE_NAME,
					exportType,
					LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")),
					EXPORT_FILE_EXTENSION);
			// This will override the already existing file
			downloadedFile = downloadToFile(downloadLink, new java.io.File(fileName));
			log.info("Download finished: {}", downloadedFile.getName());
		} catch (IOException e) {
			log.warn("Exception during export", e);
		}
		return Optional.ofNullable(downloadedFile);
	}


	private File downloadToFile(String url, File destinationFile) throws IOException {
		Request request = new Request.Builder()
				.url(url)
				.build();
		try (Response response = okHttpClient.newCall(request).execute()) {
			InputStream in = response.body().byteStream();
			FileUtils.copyInputStreamToFile(in, destinationFile);
			return destinationFile;
		}
	}


	// TODO create gist
	private String getTokenV2() throws IOException {
		String credentialsTemplate = "{" +
				"\"email\": \"%s\"," +
				"\"password\": \"%s\"" +
				"}";
		String credentialsJson = String.format(credentialsTemplate, notionEmail, notionPassword);
		Request request = new Request.Builder()
				.url(LOGIN_ENDPOINT)
				.post(RequestBody.create(credentialsJson, MEDIA_TYPE_JSON))
				.build();
		try (Response response = okHttpClient.newCall(request).execute()) {
			return cookieJar.loadForRequest(request.url()).stream()
					.filter(cookie -> TOKEN_V2.equals(cookie.name()))
					.findFirst()
					.orElseThrow()
					.value();
		}
	}

	//	private String getTokenV2OLD() throws IOException {
	//		String credentialsTemplate = "{" +
	//				"\"email\": \"%s\"," +
	//				"\"password\": \"%s\"" +
	//				"}";
	//		String credentialsJson = String.format(credentialsTemplate, notionEmail, notionPassword);
	//
	//		HttpPost loginRequest = new HttpPost(LOGIN_ENDPOINT);
	//		loginRequest.setEntity(new StringEntity(credentialsJson, ContentType.APPLICATION_JSON));
	//		try (CloseableHttpResponse response = httpClient.execute(loginRequest)) {
	//			List<Cookie> cookies = cookieStore.getCookies();
	//			return extractTokenV2(cookies);
	//		}
	//	}

	//	private String extractTokenV2(List<Cookie> cookies) {
	//		return cookies.stream()
	//				.filter(cookie -> TOKEN_V2.equals(cookie.getName()))
	//				.findFirst()
	//				.orElseThrow()
	//				.getValue();
	//	}


	private String triggerExportTask(String tokenV2) throws IOException {
		Request request = new Request.Builder()
				.url(ENQUEUE_ENDPOINT)
				.addHeader("Cookie", TOKEN_V2 + "=" + tokenV2)
				.post(RequestBody.create(getTaskJson(), MEDIA_TYPE_JSON))
				.build();

		try (Response response = okHttpClient.newCall(request).execute()) {
			JsonNode responseJsonNode = objectMapper.readTree(response.body().string());
			return responseJsonNode.get("taskId").asText();
		}
	}

	// TODO add as snippet -> HttpClient setCooke, PostRequest
	//	private String triggerExportTaskOLD(String tokenV2) throws IOException {
	//		HttpPost postRequest = new HttpPost(ENQUEUE_ENDPOINT);
	//		postRequest.addHeader("Cookie", TOKEN_V2 + "=" + tokenV2);
	//		postRequest.setEntity(new StringEntity(getTaskJson(), ContentType.APPLICATION_JSON));
	//
	//		try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
	//			String responseAsString = EntityUtils.toString(response.getEntity());
	//			JsonNode responseJsonNode = objectMapper.readTree(responseAsString);
	//			return responseJsonNode.get("taskId").asText();
	//		}
	//	}

	//	private String getDownloadLinkOLD(String taskId, String tokenV2) throws IOException {
	//		HttpPost taskStatusRequest = new HttpPost(GET_TASKS_ENDPOINT);
	//		taskStatusRequest.addHeader("Cookie", TOKEN_V2 + "=" + tokenV2);
	//		String postBody = String.format("{\"taskIds\": [\"%s\"]}", taskId);
	//		taskStatusRequest.setEntity(new StringEntity(postBody, ContentType.APPLICATION_JSON));
	//
	//		for (int i = 0; i < 10; i++) {
	//			try (CloseableHttpResponse responseForTasks = httpClient.execute(taskStatusRequest)) {
	//				String responseAsString = EntityUtils.toString(responseForTasks.getEntity());
	//				JsonNode taskResultJsonNode = objectMapper.readTree(responseAsString);
	//				// TODO find result with correct id
	//				String state = taskResultJsonNode.get("results").get(0).get("state").asText();
	//				log.info("state: " + state);
	//
	//				if ("success".equals(state)) {
	//					return taskResultJsonNode.get("results").get(0).get("status").get("exportURL").asText();
	//				}
	//				sleep(4000);
	//			}
	//		}
	//		// TODO
	//		return "";
	//	}


	private String getDownloadLink(String taskId, String tokenV2) throws IOException {
		String postBody = String.format("{\"taskIds\": [\"%s\"]}", taskId);
		Request request = new Request.Builder()
				.url(GET_TASKS_ENDPOINT)
				.addHeader("Cookie", TOKEN_V2 + "=" + tokenV2)
				.post(RequestBody.create(postBody, MEDIA_TYPE_JSON))
				.build();
		for (int i = 0; i < 10; i++) {
			try (Response response = okHttpClient.newCall(request).execute()) {
				JsonNode responseJsonNode = objectMapper.readTree(response.body().string());
				// TODO find result with correct id
				String state = responseJsonNode.get("results").get(0).get("state").asText();
				log.info("state: " + state);
				if ("success".equals(state)) {
					return responseJsonNode.get("results").get(0).get("status").get("exportURL").asText();
				}
				sleep(4000);
			}
		}
		//		TODO
		return "";
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
