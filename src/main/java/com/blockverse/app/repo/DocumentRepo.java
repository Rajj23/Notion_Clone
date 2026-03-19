package com.blockverse.app.repo;

import com.blockverse.app.entity.Document;
import com.blockverse.app.entity.WorkSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepo extends JpaRepository<Document, Integer> {
    List<Document> findByWorkSpaceAndArchivedFalse(WorkSpace workSpace);

    Optional<Document> findByIdAndArchivedFalseAndDeletedFalse(int id);

    Optional<Document> findByIdAndDeletedFalse(int id);

    Optional<Document> findByIdAndArchivedTrueAndDeletedFalse(int documentId);

    List<Document> findByWorkSpaceAndArchivedFalseAndDeletedFalseOrderByCreatedAtDesc(WorkSpace workSpace);

    List<Document> findByWorkSpaceAndDeletedTrue(WorkSpace workSpace);

    @Query(value = "SELECT * FROM document " +
            "WHERE workspace_id = :workSpaceId " +
            "AND title LIKE CONCAT('%', :keyword, '%')",
            nativeQuery = true)
    List<Document> searchDocuments(@Param("keyword") String keyword,
                                   @Param("workSpaceId") int workSpaceId);
}
