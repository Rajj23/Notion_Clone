package com.blockverse.app.repo;

import com.blockverse.app.entity.BlockChangeLog;
import com.blockverse.app.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlockChangeLogRepo extends JpaRepository<BlockChangeLog, Integer> {
    List<BlockChangeLog> findByDocumentAndVersionNumberGreaterThanOrderByVersionNumberDesc(Document document, Long targetVersion);

    List<BlockChangeLog> findByDocumentOrderByVersionNumberDesc(Document document);
}
