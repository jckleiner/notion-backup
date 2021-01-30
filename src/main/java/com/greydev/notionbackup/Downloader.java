package com.greydev.notionbackup;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Downloader {

	// TODO dist Downloader-Apache Http client

	//	public File downloadOLD(URL url, File dstFile) {
	//		CloseableHttpClient httpclient = HttpClients.custom()
	//				.setRedirectStrategy(new LaxRedirectStrategy()) // adds HTTP REDIRECT support to GET and POST methods
	//				.build();
	//		try {
	//			HttpGet get = new HttpGet(url.toURI()); // we're using GET but it could be via POST as well
	//			File downloaded = httpclient.execute(get, new FileDownloadResponseHandler(dstFile));
	//			return downloaded;
	//		} catch (Exception e) {
	//			throw new IllegalStateException(e);
	//		} finally {
	//			IOUtils.closeQuietly(httpclient);
	//		}
	//	}
	//
	//	static class FileDownloadResponseHandler implements ResponseHandler<File> {
	//
	//		private final File target;
	//
	//
	//		public FileDownloadResponseHandler(File target) {
	//			this.target = target;
	//		}
	//
	//
	//		@Override
	//		public File handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
	//			InputStream source = response.getEntity().getContent();
	//			FileUtils.copyInputStreamToFile(source, this.target);
	//			return this.target;
	//		}
	//
	//	}

}