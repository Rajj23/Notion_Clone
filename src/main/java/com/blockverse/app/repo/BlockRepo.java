package com.blockverse.app.repo;

import com.blockverse.app.entity.Block;
import com.blockverse.app.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlockRepo extends JpaRepository<Block, Integer> {
    List<Block> findByDocumentAndParentAndDeletedFalseOrderByPositionAsc(Document document, Block parent);
    List<Block> findByDocumentAndDeletedFalseOrderByPositionAsc(Document document);
    List<Block> findByParentAndDeletedFalseOrderByPositionAsc(Block parent);
    List<Block> findByDocumentAndParentIsNull(Document document);

    @Query(value = "SELECT b.* FROM block b " + 
                   "JOIN document d ON b.document_id = d.id " + 
                    "WHERE d.workspace_id = :workSpaceId " +
                    "AND MATCH(b.content) AGAINST(:keyword IN BOOLEAN MODE)",
                     nativeQuery = true)
    List<Block> searchBlocks(@Param("keyword") String keyword,
                             @Param("workSpaceId") int workSpaceId);
}
