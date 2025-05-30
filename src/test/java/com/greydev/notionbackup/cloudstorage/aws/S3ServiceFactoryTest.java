package com.greydev.notionbackup.cloudstorage.aws;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class S3ServiceFactoryTest {

    @Test
    void testCreate_ValidCredentials() {
        // given
        String accessKey = "test-access-key";
        String secretKey = "test-secret-key";
        String region = "us-east-1";

        // when
        var result = S3ServiceFactory.create(accessKey, secretKey, region);

        // then
        assertTrue(result.isPresent());
    }

    @Test
    void testCreate_InvalidCredentials() {
        // given
        String accessKey = null;
        String secretKey = null;
        String region = "invalid-region";

        // when
        var result = S3ServiceFactory.create(accessKey, secretKey, region);

        // then
        assertFalse(result.isPresent());
    }
} 