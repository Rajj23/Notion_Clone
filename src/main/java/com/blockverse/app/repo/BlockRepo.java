package com.blockverse.app.repo;

import com.blockverse.app.entity.Block;
import com.blockverse.app.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlockRepo extends JpaRepository<Block, Integer> {
    List<Block> findByDocumentAndParentAndDeletedFalseOrderByPositionAsc(Document document, Block parent);
    List<Block> findByDocumentAndDeletedFalseOrderByPositionAsc(Document document);
    List<Block> findByParentAndDeletedFalseOrderByPositionAsc(Block parent);

    List<Block> findByDocumentAndParentIsNull(Document document);
}
