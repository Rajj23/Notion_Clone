package com.blockverse.app.service;

import com.blockverse.app.dto.block.*;
import com.blockverse.app.entity.*;
import com.blockverse.app.enums.BlockOperationType;
import com.blockverse.app.exception.*;
import com.blockverse.app.mapper.BlockMapper;
import com.blockverse.app.repo.*;
import com.blockverse.app.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class BlockService {

    private final DocumentRepo documentRepo;
    private final WorkSpaceMemberRepo workSpaceMemberRepo;
    private final SecurityUtil securityUtil;
    private final BlockRepo blockRepo;
    private final BlockChangeLogRepo blockChangeLogRepo;

    private Document getDocumentOrThrow(int documentId) {
        return documentRepo.findById(documentId)
                .orElseThrow(()-> new DocumentNotFoundException("Document not found"));
    }

    private void getMembershipOrThrow(User user, WorkSpace workSpace) {
        workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(user, workSpace)
                .orElseThrow(() -> new NotWorkSpaceMemberException("User is not a member of this workspace"));
    }

    private Block getBlockOrThrow(int blockId) {
        return blockRepo.findById(blockId)
                .orElseThrow(() -> new BlockNotFoundException("Block not found"));
    }

    private boolean isDescendant(Block block, Block potentialAncestor){
        Block current = potentialAncestor;

        while(current != null){
            if(current.getId() == block.getId()){
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private void logChange(
            Document document,
            Block block,
            BlockOperationType operationType,
            String oldContent,
            String newContent,
            BigInteger oldPosition,
            BigInteger newPosition,
            Integer oldParentId,
            Integer newParentId,
            User user) {

        document.setUpdatedAt(LocalDateTime.now());
        Document savedDocument = documentRepo.save(document);

        BlockChangeLog log = BlockChangeLog.builder()
                .document(savedDocument)
                .block(block)
                .operationType(operationType)
                .oldContent(oldContent)
                .newContent(newContent)
                .oldPosition(oldPosition)
                .newPosition(newPosition)
                .oldParentId(oldParentId)
                .newParentId(newParentId)
                .changedBy(user)
                .versionNumber(savedDocument.getVersion())
                .build();

        blockChangeLogRepo.save(log);
    }

    private void checkConflict(Document document, Long clientVersion) {
        if (clientVersion == null)
            return;

        Long serverVersion = document.getVersion() == null ? 0L : document.getVersion();

        if(!serverVersion.equals(clientVersion)){
            throw new DocumentVersionMismatchException("Document modified by another user");
        }

    }

    public BlockResponse createBlock(int documentId, CreateBlockRequest request){
        User currentUser = securityUtil.getLoggedInUser();

        Document document = getDocumentOrThrow(documentId);
        WorkSpace workSpace = document.getWorkSpace();
        getMembershipOrThrow(currentUser, workSpace);

        Block block = new Block();

        block.setDocument(document);

        Block parent = null;
        if(request.getParentId() != null){
            parent = getBlockOrThrow(request.getParentId());
            if(parent.getDocument().getId() != documentId){
                throw new BlockLevelException("Parent block must belong to the same document");
            }
            block.setParent(parent);
        }

        block.setContent(request.getContent());
        block.setType(request.getType());

        BigInteger position = BigInteger.valueOf(10000);
        List<Block> siblings = blockRepo.findByDocumentAndParentAndDeletedFalseOrderByPositionAsc(document, parent);
        if (!siblings.isEmpty()) {
            position = siblings.getLast()
                    .getPosition()
                    .add(BigInteger.valueOf(10000));
        }
        block.setPosition(position);

        checkConflict(document, request.getDocumentVersion());

        Block savedBlock = blockRepo.save(block);

        logChange(document, savedBlock,
                BlockOperationType.CREATE,
                null,
                savedBlock.getContent(),
                null,
                position,
                null,
                null,
                currentUser);

        
        return BlockMapper.toBlockResponse(savedBlock);
    }

    public List<BlockResponse> getBlocksForDocument(int documentId){
        User currentUser = securityUtil.getLoggedInUser();
        Document document = getDocumentOrThrow(documentId);
        WorkSpace workSpace = document.getWorkSpace();
        getMembershipOrThrow(currentUser, workSpace);

        List<Block> blocks = blockRepo.findByDocumentAndDeletedFalseOrderByPositionAsc(document);

        Map<Integer, BlockResponse> map = new HashMap<>();
        List<BlockResponse> roots = new ArrayList<>();

        for(Block block : blocks){
            map.put(block.getId(), BlockMapper.toBlockResponse(block));
        }

        for(Block block : blocks){
            BlockResponse dto = map.get(block.getId());

            if(block.getParent() == null){
                roots.add(dto);
            } else {
                map.get(block.getParent().getId())
                        .getChildren()
                        .add(dto);
            }
        }

        return roots;
    }

    public BlockResponse updateBlock(int blockId, UpdateBlockRequest request) {
        User currentUser = securityUtil.getLoggedInUser();
        Block block = getBlockOrThrow(blockId);

        Document document = block.getDocument();
        WorkSpace workSpace = document.getWorkSpace();
        getMembershipOrThrow(currentUser, workSpace);

        checkConflict(document, request.getDocumentVersion());

        String oldContent = block.getContent();

        block.setContent(request.getContent());
        block.setType(request.getType());
        Block updatedBlock = blockRepo.save(block);

        logChange(document,
                updatedBlock,
                BlockOperationType.UPDATE,
                oldContent,
                block.getContent(),
                null,
                null,
                null,
                null,
                currentUser);

        return BlockMapper.toBlockResponse(updatedBlock);
    }

    public void deleteBlock(int blockId, DeleteBlockRequest request) {
        User currentUser = securityUtil.getLoggedInUser();
        Block block = getBlockOrThrow(blockId);

        Document document = block.getDocument();
        WorkSpace workSpace = document.getWorkSpace();
        getMembershipOrThrow(currentUser, workSpace);

        checkConflict(document, request.getDocumentVersion());

        String oldContent = block.getContent();
        BigInteger oldPosition = block.getPosition();

        logChange(document,
                block,
                BlockOperationType.DELETE,
                oldContent,
                null,
                oldPosition,
                null,
                null,
                null,
                currentUser);

        block.setDeleted(true);
        blockRepo.save(block);
    }

    public List<BlockResponse> getChildren(int parentId){
        User user = securityUtil.getLoggedInUser();
        Block parent = getBlockOrThrow(parentId);
        Document document = parent.getDocument();
        getMembershipOrThrow(user, document.getWorkSpace());

        List<Block> children = blockRepo.findByParentAndDeletedFalseOrderByPositionAsc(parent);

        return children.stream()
                .map(BlockMapper::toBlockResponse)
                .toList();
    }

    public BlockResponse moveBlock(int blockId, MoveBlockRequest request) {
        User user = securityUtil.getLoggedInUser();
        Block block = getBlockOrThrow(blockId);
        Document document = block.getDocument();
        getMembershipOrThrow(user, document.getWorkSpace());

        Block newParent = null;
        if(request.getNewParentId() != null){
            newParent = getBlockOrThrow(request.getNewParentId());
            if(newParent.getDocument().getId() != document.getId()){
                throw new DocumentLevelException("New parent must belong to the same document");
            }
            if(isDescendant(block, newParent)){
                throw new BlockLevelException("Cannot move a block under its own descendant");
            }
        }

        checkConflict(document, request.getDocumentVersion());

        Integer oldParentId = block.getParent() != null ? block.getParent().getId() : null;
        BigInteger oldPosition = block.getPosition();

        block.setParent(newParent);
        block.setPosition(request.getNewPosition());

        logChange(document,
                block,
                BlockOperationType.MOVE,
                null,
                null,
                oldPosition,
                block.getPosition(),
                oldParentId,
                newParent != null ? newParent.getId() : null,
                user);

        return BlockMapper.toBlockResponse(blockRepo.save(block));
    }
    
    @Transactional
    public void restoreDocumentVersion(int documentId, Long targetVersion){
        User user = securityUtil.getLoggedInUser();
        Document document = getDocumentOrThrow(documentId);
        getMembershipOrThrow(user, document.getWorkSpace());
        
        if(document.getVersion() < targetVersion){
            throw new DocumentLevelException("Target version must be less than or equal to current version");
        }
        
        List<BlockChangeLog> logs = blockChangeLogRepo
                .findByDocumentAndVersionNumberGreaterThanOrderByVersionNumberDesc(
                        document, targetVersion
                );
        
        for(BlockChangeLog log : logs){
            Block block = blockRepo.findById(log.getBlock().getId())
                    .orElse(null);
            
            if(block == null) continue;
            
            switch (log.getOperationType()){
                
                case CREATE :
                    block.setDeleted(true);
                    break;
                    
                case DELETE:
                    block.setDeleted(false);
                    block.setContent(log.getOldContent());
                    block.setPosition(log.getOldPosition());
                    break;
                    
                case UPDATE:
                    block.setContent(log.getOldContent());
                    break;
                    
                case MOVE:
                    block.setPosition(log.getOldPosition());
                    
                    if(log.getOldParentId() != null){
                        Block parent = blockRepo.findById(log.getOldParentId())
                                .orElseThrow(() -> new BlockNotFoundException("Parent block not found"));
                        block.setParent(parent);
                    }
                    else{
                        block.setParent(null);
                    }
                    break;
            }
            blockRepo.save(block);
        }
        document.setVersion(targetVersion);
        documentRepo.save(document);
    }
    
    public List<BlockChangeLog> getDocumentHistory(int documentId){
        User user = securityUtil.getLoggedInUser();
        Document document = getDocumentOrThrow(documentId);
        getMembershipOrThrow(user, document.getWorkSpace());

        return blockChangeLogRepo.findByDocumentOrderByVersionNumberDesc(document);
    }

}
