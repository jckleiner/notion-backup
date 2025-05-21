package com.greydev.notionbackup.cloudstorage.aws;

import java.io.File;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.greydev.notionbackup.cloudstorage.CloudStorageClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class S3Client implements CloudStorageClient {

    private final AmazonS3 s3Client;
    private final String bucketName;

    public S3Client(AmazonS3 s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public boolean upload(File fileToUpload) {
        log.info("S3: uploading file '{}' ...", fileToUpload.getName());
        if (!(fileToUpload.exists() && fileToUpload.isFile())) {
            log.error("S3: could not find {} in project root directory", fileToUpload.getName());
            return false;
        }

        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileToUpload.getName(), fileToUpload);
            s3Client.putObject(putObjectRequest);
            log.info("S3: successfully uploaded '{}'", fileToUpload.getName());
            return true;
        } catch (Exception e) {
            log.error("S3: Error uploading file", e);
            return false;
        }
    }
}
