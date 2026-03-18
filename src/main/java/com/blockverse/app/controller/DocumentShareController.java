package com.blockverse.app.controller;

import com.blockverse.app.dto.document.DocumentDetailsResponse;
import com.blockverse.app.service.DocumentShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/share")
public class DocumentShareController {
    
    private final DocumentShareService documentShareService;
    
    @GetMapping("/{token}")
    public ResponseEntity<DocumentDetailsResponse> getSharedDocument(@PathVariable String token) {
        return ResponseEntity.ok(documentShareService.getSharedDocument(token));
    }
}
