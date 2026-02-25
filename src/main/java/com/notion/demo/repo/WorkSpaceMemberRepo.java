package com.notion.demo.repo;

import com.notion.demo.dto.WorkSpaceDetailsResponse;
import com.notion.demo.entity.User;
import com.notion.demo.entity.WorkSpace;
import com.notion.demo.entity.WorkSpaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkSpaceMemberRepo extends JpaRepository<WorkSpaceMember, Integer> {
      
      @Query("""
        SELECT new com.notion.demo.dto.WorkSpaceDetailsResponse(
        w.id,
        w.name,
        w.type,
        new com.notion.demo.dto.OwnerInfo(o.id, o.name),
        COUNT(allMembers.id),
        currentMember.role
        )
        FROM WorkSpaceMember currentMember
        JOIN currentMember.workSpace w
        JOIN WorkSpaceMember ownerMember 
        ON ownerMember.workSpace = w 
        AND ownerMember.role = com.notion.demo.enums.WorkSpaceRole.OWNER
        JOIN ownerMember.user o
        JOIN WorkSpaceMember allMembers 
        ON allMembers.workSpace = w
        WHERE currentMember.user = :user
        GROUP BY w.id, w.name, w.type, o.id, o.name, currentMember.role
        """)
      List<WorkSpaceDetailsResponse> findWorkspaceDetailsForUser(User user);

      
      
        @Query("""
        SELECT new com.notion.demo.dto.WorkSpaceDetailsResponse(
            w.id,
            w.name,
            w.type,
        new com.notion.demo.dto.OwnerInfo(o.id, o.name),
            COUNT(allMembers.id),
            currentMember.role
        )
        FROM WorkSpaceMember currentMember
        JOIN currentMember.workSpace w
        JOIN WorkSpaceMember ownerMember 
            ON ownerMember.workSpace = w 
            AND ownerMember.role = com.notion.demo.enums.WorkSpaceRole.OWNER
            JOIN ownerMember.user o
        JOIN WorkSpaceMember allMembers 
            ON allMembers.workSpace = w
        WHERE currentMember.user = :user
            AND w.id = :workspaceId
        GROUP BY w.id, w.name, w.type, 
             o.id, o.name, currentMember.role
        """)
      Optional<WorkSpaceDetailsResponse>
      findWorkspaceDetailsForUserAndWorkspaceId(User user, int workspaceId);

      Optional<WorkSpaceMember> findByUserAndWorkSpaceAndDeletedAtIsNull(User currentUser, WorkSpace workSpace);

      Optional<WorkSpaceMember> findByUserAndWorkSpace(User user, WorkSpace workSpace);

      List<WorkSpaceDetailsResponse> findWorkspaceDetailsForUserAndDeletedAtIsNull(User currentUser);

      int countByWorkSpaceIdAndDeletedAtIsNull(int workSpaceId);
}
