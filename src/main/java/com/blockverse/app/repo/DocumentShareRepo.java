package com.blockverse.app.repo;

import com.blockverse.app.entity.DocumentShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentShareRepo extends JpaRepository<DocumentShare, Integer> {
    Optional<DocumentShare> findByTokenAndActiveTrue(String token);
    void deleteByDocument(com.blockverse.app.entity.Document document);
}
