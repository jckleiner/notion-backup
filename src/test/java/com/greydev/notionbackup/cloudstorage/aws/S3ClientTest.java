package com.greydev.notionbackup.cloudstorage.aws;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class S3ClientTest {

    @Mock
    private AmazonS3 s3Client;

    @Test
    void testUpload() {
        // given
        File fileToUpload = new File("src/test/resources/testFileToUpload.txt");
        String bucketName = "test-bucket";
        S3Client client = new S3Client(s3Client, bucketName);

        // when
        boolean result = client.upload(fileToUpload);

        // then
        assertTrue(result);
        verify(s3Client).putObject(any(PutObjectRequest.class));
    }

    @Test
    void testUpload_Exception() {
        // given
        File fileToUpload = new File("src/test/resources/testFileToUpload.txt");
        String bucketName = "test-bucket";
        S3Client client = new S3Client(s3Client, bucketName);

        doThrow(new RuntimeException("S3 error")).when(s3Client).putObject(any(PutObjectRequest.class));

        // when
        boolean result = client.upload(fileToUpload);

        // then
        assertFalse(result);
        verify(s3Client).putObject(any(PutObjectRequest.class));
    }

    @Test
    void testUpload_invalidFile() {
        // given
        File fileToUpload = new File("thisFileDoesNotExist.txt");
        String bucketName = "test-bucket";
        S3Client client = new S3Client(s3Client, bucketName);

        // when
        boolean result = client.upload(fileToUpload);

        // then
        assertFalse(result);
        verifyNoInteractions(s3Client);
    }
} 