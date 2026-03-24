package com.blockverse.app.controller;

import com.blockverse.app.entity.User;
import com.blockverse.app.security.SecurityUtil;
import com.blockverse.app.service.RateLimiterService;
import com.blockverse.app.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/files")
public class FileController {
    
    private final S3Service s3Service;
    private final SecurityUtil securityUtil;
    private final RateLimiterService rateLimiterService;
    
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file")MultipartFile file){
        User user = securityUtil.getLoggedInUser();
        rateLimiterService.checkRateLimit(user.getId(), "FILE_UPLOAD");
        return ResponseEntity.ok(s3Service.uploadFile(file));
    }
}
