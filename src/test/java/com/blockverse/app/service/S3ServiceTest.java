package com.blockverse.app.service;

import com.blockverse.app.exception.S3FileUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
    }

    @Test
    void uploadFile_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String fileName = s3Service.uploadFile(file);

        assertNotNull(fileName);
        assertTrue(fileName.endsWith("_test.txt"));
        
        ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putRequestCaptor.capture(), any(RequestBody.class));
        
        PutObjectRequest capturedRequest = putRequestCaptor.getValue();
        assertEquals("test-bucket", capturedRequest.bucket());
        assertEquals("text/plain", capturedRequest.contentType());
        assertEquals(fileName, capturedRequest.key());
    }

    @Test
    void uploadFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("S3 error"));

        assertThrows(S3FileUploadException.class, () -> s3Service.uploadFile(file));
    }

    @Test
    void generateUrl_success() throws Exception {
        String key = "test-key.txt";
        String expectedUrl = "https://test-bucket.s3.amazonaws.com/test-key.txt";

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL(expectedUrl));

        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        String resultUrl = s3Service.generateUrl(key);

        assertEquals(expectedUrl, resultUrl);

        ArgumentCaptor<GetObjectPresignRequest> presignRequestCaptor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(presignRequestCaptor.capture());

        GetObjectPresignRequest capturedRequest = presignRequestCaptor.getValue();
        assertEquals("test-bucket", capturedRequest.getObjectRequest().bucket());
        assertEquals(key, capturedRequest.getObjectRequest().key());
    }
}
