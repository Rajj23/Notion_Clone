package com.blockverse.app.repo;

import com.blockverse.app.entity.BlockChangeLog;
import com.blockverse.app.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlockChangeLogRepo extends JpaRepository<BlockChangeLog, Integer> {
    List<BlockChangeLog> findByDocumentAndVersionNumberGreaterThanOrderByVersionNumberDesc(Document document, Long targetVersion);

    List<BlockChangeLog> findByDocumentOrderByVersionNumberDesc(Document document);

    @Modifying
    @Query("DELETE FROM BlockChangeLog b WHERE b.document = :document")
    void deleteByDocument(@Param("document") Document document);
}
