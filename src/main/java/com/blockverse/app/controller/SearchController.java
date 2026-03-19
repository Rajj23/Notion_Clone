package com.blockverse.app.controller;

import com.blockverse.app.dto.SearchResponse;
import com.blockverse.app.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/v1")
public class SearchController {
    private final SearchService searchService;
    
    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(String keyword, int workSpaceId){
        SearchResponse response = searchService.search(keyword, workSpaceId);
        return ResponseEntity.ok(response);
    }
}
