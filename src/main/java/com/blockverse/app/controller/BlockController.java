package com.blockverse.app.controller;

import com.blockverse.app.dto.block.*;
import com.blockverse.app.entity.BlockChangeLog;
import com.blockverse.app.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/blocks")
@RequiredArgsConstructor
public class BlockController {
    
    private final BlockService blockService;
    
    @PostMapping("/{documentId}")
    public ResponseEntity<BlockResponse> createBlock(@PathVariable int documentId, @RequestBody CreateBlockRequest request){
        return ResponseEntity.ok(blockService.createBlock(documentId, request));
    }
    
    @PutMapping("/{blockId}")
    public ResponseEntity<BlockResponse> updateBlock(@PathVariable int blockId, @RequestBody UpdateBlockRequest request){
        return ResponseEntity.ok(blockService.updateBlock(blockId, request));
    }
    
    @DeleteMapping("/{blockId}")
    public ResponseEntity<String> deleteBlock(@PathVariable int blockId, @RequestBody DeleteBlockRequest request){
        blockService.deleteBlock(blockId, request);
        return ResponseEntity.ok("Block deleted successfully");
    }
    
    @PutMapping("/{blockId}/move")
    public ResponseEntity<BlockResponse> moveBlock(@PathVariable int blockId, @RequestBody MoveBlockRequest request){
        return ResponseEntity.ok(blockService.moveBlock(blockId, request));
    }
    
    @GetMapping("/{blockId}/children")
    public ResponseEntity<List<BlockResponse>> getChildren(@PathVariable int blockId){
        return ResponseEntity.ok(blockService.getChildren(blockId));
    }
    
    @PostMapping("/restore/{documentId}")
    public ResponseEntity<String> restoreDocumentVersion(@PathVariable int documentId, @RequestBody RestoreDocumentVersionRequest request){
        blockService.restoreDocumentVersion(documentId, request);
        return ResponseEntity.ok("Document version restored successfully");
    }
    
    @GetMapping("/history/{documentId}")
    public ResponseEntity<List<BlockChangeLog>> getDocumentHistory(@PathVariable int documentId){
        return ResponseEntity.ok(blockService.getDocumentHistory(documentId));
    }
}
