package com.blockverse.app.service;

import com.blockverse.app.exception.TooManyRequestsException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RateLimiterService {
    
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    
    public void clearCache() {
        cache.clear();
    }
    
    private Bucket createNewBucketForAction(String action) {
        Bandwidth limit;
        switch (action) {
            case "CREATE_BLOCK":
                limit = Bandwidth.simple(25, Duration.ofMinutes(1)); // 20 requests per minute
                break; 
            case "UPDATE_BLOCK": 
                limit = Bandwidth.simple(30, Duration.ofMinutes(1)); // 20 requests per minute
                break;
            case "CREATE_DOCUMENT":
                limit = Bandwidth.simple(5, Duration.ofMinutes(1)); // 5 requests per minute
                break;
            case "EDIT_DOCUMENT":
                limit = Bandwidth.simple(10, Duration.ofMinutes(1)); // 10 requests per minute
                break;
            case "DELETE_DOCUMENT":
                limit = Bandwidth.simple(3, Duration.ofMinutes(1)); // 3 requests per minute
                break;
            case "FILE_UPLOAD":
                limit = Bandwidth.simple(5, Duration.ofMinutes(1)); // 5 uploads per minute
                break;
            case "SEARCH":
                limit = Bandwidth.simple(20, Duration.ofMinutes(1)); // 20 searches per minute
                break;
            case "ACTIVITY_FEED":
                limit = Bandwidth.simple(30, Duration.ofMinutes(1)); // 30 feed loads per minute
                break;
            default:
                limit = Bandwidth.simple(20, Duration.ofMinutes(1)); // Default: 20 requests per minute
        }

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
    
    public void checkRateLimit(int userId, String action){
        Bucket bucket = cache.computeIfAbsent(userId + ":" + action, k -> createNewBucketForAction(action));
        
        if(!bucket.tryConsume(1)){
            System.out.println("Rate limit exceeded for user: " + userId + " action: " + action);
            throw new TooManyRequestsException("Too many requests. Please try again later.");
        }
    }
}
