package com.greydev.notionbackup;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.cdimascio.dotenv.Dotenv;


public class NotionBackup {

	private static final Logger LOG = LoggerFactory.getLogger(NotionBackup.class);
	private static final String GET_TASKS_ENDPOINT = "https://www.notion.so/api/v3/getTasks";
	private static final String ENQUEUE_ENDPOINT = "https://www.notion.so/api/v3/enqueueTask";
	private static final String LOGIN_ENDPOINT = "https://www.notion.so/api/v3/loginWithEmail";
	private static final String DEFAULT_EXPORT_TYPE = "markdown";
	private static final String TOKEN_V2 = "token_v2";

	private final CookieStore httpCookieStore = new BasicCookieStore();
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final CloseableHttpClient httpClient;
	private final String notionSpaceId;
	private final String notionEmail;
	private final String notionPassword;
	private final String exportType;


	NotionBackup() {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().ignoreIfMalformed().load();
		if (dotenv == null) {
			exit("Could not load dotenv!");
		}

		// both environment variables and variables defined in the .env file can be accessed this way
		notionSpaceId = dotenv.get("NOTION_SPACE_ID");
		notionEmail = dotenv.get("NOTION_EMAIL");
		notionPassword = dotenv.get("NOTION_PASSWORD");
		exportType = StringUtils.isNotBlank(dotenv.get("EXPORT_TYPE")) ? dotenv.get("EXPORT_TYPE") : DEFAULT_EXPORT_TYPE;

		if (StringUtils.isBlank(notionSpaceId)) {
			exit("notionSpaceId is missing!");
		}
		if (StringUtils.isBlank(notionEmail)) {
			exit("notionEmail is missing!");
		}
		if (StringUtils.isBlank(notionPassword)) {
			exit("notionPassword is missing!");
		}

		// Prevent warning 'Invalid cookie header' warning by setting a cookie spec
		// Also adding a cookie store to access the cookies
		httpClient = HttpClients.custom()
				.setDefaultCookieStore(httpCookieStore)
				.setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
				.build();
		LOG.info("init done");
	}


	public static void main(String[] args) {
		NotionBackup notionBackup = new NotionBackup();
		try {
			String tokenV2 = notionBackup.getTokenV2();
			LOG.info("tokenV2 extracted");
			String taskId = notionBackup.triggerExportTask(tokenV2);
			LOG.info("taskId extracted");
			String downloadLink = notionBackup.getDownloadLink(taskId, tokenV2);
			LOG.info("downloadLink extracted");
			// TODO download file
			// TODO upload it to GDrive
		} catch (IOException e) {
			LOG.error("An exception occurred: ", e);
		}
	}


	private String triggerExportTask(String tokenV2) throws IOException {
		HttpPost postRequest = new HttpPost(ENQUEUE_ENDPOINT);
		postRequest.addHeader("Cookie", TOKEN_V2 + "=" + tokenV2);
		postRequest.setEntity(new StringEntity(getTaskJson(), ContentType.APPLICATION_JSON));

		try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
			String responseAsString = EntityUtils.toString(response.getEntity());
			JsonNode responseJsonNode = objectMapper.readTree(responseAsString);
			return responseJsonNode.get("taskId").asText();
		}
	}


	private String getDownloadLink(String taskId, String tokenV2) throws IOException {
		HttpPost taskStatusRequest = new HttpPost(GET_TASKS_ENDPOINT);
		taskStatusRequest.addHeader("Cookie", TOKEN_V2 + "=" + tokenV2);
		String postBody = String.format("{\"taskIds\": [\"%s\"]}", taskId);
		taskStatusRequest.setEntity(new StringEntity(postBody, ContentType.APPLICATION_JSON));

		for (int i = 0; i < 10; i++) {
			try (CloseableHttpResponse responseForTasks = httpClient.execute(taskStatusRequest)) {
				String responseAsString = EntityUtils.toString(responseForTasks.getEntity());
				JsonNode taskResultJsonNode = objectMapper.readTree(responseAsString);
				// TODO find result with correct id
				String state = taskResultJsonNode.get("results").get(0).get("state").asText();
				LOG.info("state: " + state);

				if ("success".equals(state)) {
					return taskResultJsonNode.get("results").get(0).get("status").get("exportURL").asText();
				}
				sleep(4000);
			}
		}
		// TODO
		return "";
	}


	private String getTokenV2() throws IOException {
		String credentialsTemplate = "{" +
				"\"email\": \"%s\"," +
				"\"password\": \"%s\"" +
				"}";
		String credentialsJson = String.format(credentialsTemplate, notionEmail, notionPassword);

		HttpPost loginRequest = new HttpPost(LOGIN_ENDPOINT);
		loginRequest.setEntity(new StringEntity(credentialsJson, ContentType.APPLICATION_JSON));
		try (CloseableHttpResponse response = httpClient.execute(loginRequest)) {
			List<Cookie> cookies = httpCookieStore.getCookies();
			return extractTokenV2(cookies);
		}
	}


	private String extractTokenV2(List<Cookie> cookies) {
		Cookie tokenV2 = cookies
				.stream()
				.filter(c -> TOKEN_V2.equals(c.getName()))
				.findFirst()
				.orElseThrow();
		return tokenV2.getValue();
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


	private void sleep(int ms) {
		try {
			LOG.info("sleeping for {}ms", ms);
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			LOG.error("An exception occurred: ", e);
		}
	}


	private void exit(String message) {
		LOG.error(message);
		System.exit(1);
	}

}