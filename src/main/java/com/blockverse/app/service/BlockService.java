package com.blockverse.app.service;

import com.blockverse.app.dto.block.*;
import com.blockverse.app.entity.*;
import com.blockverse.app.exception.BlockNotFoundException;
import com.blockverse.app.exception.DocumentNotFoundException;
import com.blockverse.app.exception.NotWorkSpaceMemberException;
import com.blockverse.app.mapper.BlockMapper;
import com.blockverse.app.repo.BlockRepo;
import com.blockverse.app.repo.DocumentRepo;
import com.blockverse.app.repo.WorkSpaceMemberRepo;
import com.blockverse.app.repo.WorkSpaceRepo;
import com.blockverse.app.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class BlockService {
    
    private final DocumentRepo documentRepo;
    private final WorkSpaceRepo workSpaceRepo;
    private final WorkSpaceMemberRepo workSpaceMemberRepo;
    private final SecurityUtil securityUtil;
    private final BlockRepo blockRepo;
    
    private Document getDocumentOrThrow(int documentId) {
        return documentRepo.findById(documentId)
                .orElseThrow(()-> new DocumentNotFoundException("Document not found"));
    }
    
    private WorkSpaceMember getMembershipOrThrow(User user, WorkSpace workSpace) {
        return workSpaceMemberRepo.findByUserAndWorkSpaceAndDeletedAtIsNull(user, workSpace)
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
                throw new RuntimeException("Parent block must belong to the same document");
            }
            block.setParent(parent);
        }
        
        block.setContent(request.getContent());
        block.setType(request.getType());

        BigInteger position = BigInteger.valueOf(1000);
        List<Block> siblings = blockRepo.
                findByDocumentAndParentAndDeletedFalseOrderByPositionAsc(document, parent);
        if(!siblings.isEmpty()){
            position = siblings.get(siblings.size()-1)
                    .getPosition()
                    .add(BigInteger.valueOf(1000));
        }
        block.setPosition(position);
        
        return BlockMapper.toBlockResponse(blockRepo.save(block));
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
            }
            else{
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
        WorkSpaceMember member = getMembershipOrThrow(currentUser, workSpace);
        
        block.setContent(request.getContent());
        block.setType(request.getType());
        
        return BlockMapper.toBlockResponse(blockRepo.save(block));
    }
    
    public void deleteBlock(int blockId) {
        User currentUser = securityUtil.getLoggedInUser();
        Block block = getBlockOrThrow(blockId);
        
        Document document = block.getDocument();
        WorkSpace workSpace = document.getWorkSpace();
        WorkSpaceMember member = getMembershipOrThrow(currentUser, workSpace);
        
        block.setDeleted(true);
        blockRepo.save(block);
    }
    
    public BlockResponse reorderBlock(int blockId, ReorderBlockRequest request){
        User user = securityUtil.getLoggedInUser();
        Block block = getBlockOrThrow(blockId);
        Document document = block.getDocument();
        getMembershipOrThrow(user, document.getWorkSpace());
        
        block.setPosition(request.getNewPosition());
        return BlockMapper.toBlockResponse(blockRepo.save(block));
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
                throw new RuntimeException("New parent must belong to the same document");
            }
            if(isDescendant(block, newParent)){
                throw new RuntimeException("Cannot move a block under its own descendant");
            }
        }
        
        block.setParent(newParent);
        block.setPosition(request.getNewPosition());
        
        return BlockMapper.toBlockResponse(blockRepo.save(block));
    }
    
}
