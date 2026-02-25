package com.notion.demo.controller;

import com.notion.demo.dto.UpdateWorkSpaceRequest;
import com.notion.demo.dto.WorkSpaceCreateRequest;
import com.notion.demo.dto.WorkSpaceDetailsResponse;
import com.notion.demo.service.WorkSpaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/workspaces")
public class WorkSpaceController {
    
    private final WorkSpaceService workSpaceService;
    
    @PostMapping("/create")
    public ResponseEntity<String> createWorkSpace(@Valid @RequestBody WorkSpaceCreateRequest request){
        return ResponseEntity.ok(workSpaceService.createWorkSpace(request));
    }
    
    @DeleteMapping("/delete/{workSpaceId}")
    public ResponseEntity<String> deleteWorkSpace(@PathVariable int workSpaceId){
        workSpaceService.deleteWorkSpace(workSpaceId);
        return ResponseEntity.ok("WorkSpace deleted successfully");
    }
    
    @GetMapping("/all")
    public ResponseEntity<List<WorkSpaceDetailsResponse>> getAllWorkSpacesForUser(){
        return ResponseEntity.ok(workSpaceService.getAllWorkSpacesForUser());
    }
    
    @GetMapping("/{workSpaceId}")
    public ResponseEntity<WorkSpaceDetailsResponse> getWorkSpaceDetails(@PathVariable int workSpaceId) {
        return ResponseEntity.ok(workSpaceService.getWorkSpaceDetails(workSpaceId));
    }
    
    @PutMapping("/update/{workSpaceId}")
    public ResponseEntity<String> updateWorkSpace(@PathVariable int workSpaceId, @RequestBody UpdateWorkSpaceRequest request) {
                workSpaceService.updateWorkSpace(workSpaceId, request);
        return ResponseEntity.ok("WorkSpace updated successfully");
    }
    
}
