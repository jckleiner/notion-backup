package com.greydev.notionbackup.cloudstorage.aws;

import java.util.Optional;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class S3ServiceFactory {

    public static Optional<AmazonS3> create(String accessKey, String secretKey, String region) {
        try {
            BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(region)
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                    .build();
            return Optional.of(s3Client);
        } catch (Exception e) {
            log.error("Error creating S3 client", e);
            return Optional.empty();
        }
    }
}
