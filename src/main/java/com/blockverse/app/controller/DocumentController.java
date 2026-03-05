package com.blockverse.app.controller;

import com.blockverse.app.dto.document.CreateDocumentRequest;
import com.blockverse.app.dto.document.DocumentDetailsResponse;
import com.blockverse.app.dto.document.DocumentResponse;
import com.blockverse.app.dto.document.UpdateDocumentRequest;
import com.blockverse.app.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/documents")
public class DocumentController {
    
    private final DocumentService documentService;
    
    @PostMapping("/{workspaceId}")
    public ResponseEntity<DocumentResponse> createDocument(@PathVariable int workspaceId, @RequestBody CreateDocumentRequest request){
        return ResponseEntity.ok(documentService.createDocument(workspaceId, request));
         
    }
    
    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable int documentId){
        return ResponseEntity.ok(documentService.getDocument(documentId));
    }
    
    @GetMapping("/{documentId}/details")
    public ResponseEntity<DocumentDetailsResponse> getDocumentWithBlocks(@PathVariable int documentId) {
        return ResponseEntity.ok(documentService.getDocumentWithBlocks(documentId));
    }
    
    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<List<DocumentResponse>> getDocumentsByWorkspace(@PathVariable int workspaceId){
        return ResponseEntity.ok(documentService.getDocumentsByWorkspace(workspaceId));
    }
    
    @PutMapping("/{documentId}")
    public ResponseEntity<DocumentResponse> updateDocument(@PathVariable int documentId, @RequestBody UpdateDocumentRequest request){
        return ResponseEntity.ok(documentService.updateDocument(documentId, request));
    }
    
    @DeleteMapping("/{documentId}")
    public ResponseEntity<String> archiveDocument(@PathVariable int documentId){
        documentService.archiveDocument(documentId);
        return ResponseEntity.ok("Document archived successfully");
    }
    
    @PostMapping("/{documentId}/restore")
    public ResponseEntity<String> restoreDocument(@PathVariable int documentId){
        documentService.unarchiveDocument(documentId);
        return ResponseEntity.ok("Document restored successfully");
    }  
}
