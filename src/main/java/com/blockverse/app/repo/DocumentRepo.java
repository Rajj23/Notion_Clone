package com.blockverse.app.repo;

import com.blockverse.app.entity.Document;
import com.blockverse.app.entity.WorkSpace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepo extends JpaRepository<Document, Integer> {
    List<Document> findByWorkSpaceAndArchivedFalse(WorkSpace workSpace);
}
