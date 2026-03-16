package com.blockverse.app.repo;

import com.blockverse.app.entity.Document;
import com.blockverse.app.entity.WorkSpace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepo extends JpaRepository<Document, Integer> {
    List<Document> findByWorkSpaceAndArchivedFalse(WorkSpace workSpace);

    Optional<Document> findByIdAndArchivedFalseAndDeletedFalse(int id);

    Optional<Document> findByIdAndDeletedFalse(int id);

    Optional<Document> findByIdAndArchivedTrueAndDeletedFalse(int documentId);

    List<Document> findByWorkSpaceAndArchivedFalseAndDeletedFalseOrderByCreatedAtDesc(WorkSpace workSpace);

    List<Document> findByWorkSpaceAndDeletedTrue(WorkSpace workSpace);
}
