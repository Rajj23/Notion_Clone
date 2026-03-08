package com.blockverse.app.repo;

import com.blockverse.app.dto.workspace.WorkSpaceDetailsResponse;
import com.blockverse.app.entity.User;
import com.blockverse.app.entity.WorkSpace;
import com.blockverse.app.entity.WorkSpaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WorkSpaceMemberRepo extends JpaRepository<WorkSpaceMember, Integer> {

    @Query("""
            SELECT new com.blockverse.app.dto.workspace.WorkSpaceDetailsResponse(
            w.id,
            w.name,
            w.type,
            new com.blockverse.app.dto.user.OwnerInfo(o.id, o.name),
            COUNT(allMembers.id),
            currentMember.role
            )
            FROM WorkSpaceMember currentMember
            JOIN currentMember.workSpace w
            JOIN WorkSpaceMember ownerMember
            ON ownerMember.workSpace = w
            AND ownerMember.role = com.blockverse.app.enums.WorkSpaceRole.OWNER
            JOIN ownerMember.user o
            JOIN WorkSpaceMember allMembers
            ON allMembers.workSpace = w
            WHERE currentMember.user = :user
            GROUP BY w.id, w.name, w.type, o.id, o.name, currentMember.role
            """)
    List<WorkSpaceDetailsResponse> findWorkspaceDetailsForUser(User user);

    @Query("""
            SELECT new com.blockverse.app.dto.workspace.WorkSpaceDetailsResponse(
                w.id,
                w.name,
                w.type,
            new com.blockverse.app.dto.user.OwnerInfo(o.id, o.name),
                COUNT(allMembers.id),
                currentMember.role
            )
            FROM WorkSpaceMember currentMember
            JOIN currentMember.workSpace w
            JOIN WorkSpaceMember ownerMember
                ON ownerMember.workSpace = w
                AND ownerMember.role = com.blockverse.app.enums.WorkSpaceRole.OWNER
                JOIN ownerMember.user o
            JOIN WorkSpaceMember allMembers
                ON allMembers.workSpace = w
            WHERE currentMember.user = :user
                AND w.id = :workspaceId
            GROUP BY w.id, w.name, w.type,
                 o.id, o.name, currentMember.role
            """)
    Optional<WorkSpaceDetailsResponse> findWorkspaceDetailsForUserAndWorkspaceId(User user, int workspaceId);

    Optional<WorkSpaceMember> findByUserAndWorkSpaceAndDeletedAtIsNull(User currentUser, WorkSpace workSpace);

    Optional<WorkSpaceMember> findByUserAndWorkSpace(User user, WorkSpace workSpace);

    @Query("""
            SELECT new com.blockverse.app.dto.workspace.WorkSpaceDetailsResponse(
            w.id,
            w.name,
            w.type,
            new com.blockverse.app.dto.user.OwnerInfo(o.id, o.name),
            COUNT(allMembers.id),
            currentMember.role
            )
            FROM WorkSpaceMember currentMember
            JOIN currentMember.workSpace w
            JOIN WorkSpaceMember ownerMember
            ON ownerMember.workSpace = w
            AND ownerMember.role = com.blockverse.app.enums.WorkSpaceRole.OWNER
            JOIN ownerMember.user o
            JOIN WorkSpaceMember allMembers
            ON allMembers.workSpace = w
            WHERE currentMember.user = :user
            AND currentMember.deletedAt IS NULL
            GROUP BY w.id, w.name, w.type, o.id, o.name, currentMember.role
            """)
    List<WorkSpaceDetailsResponse> findWorkspaceDetailsForUserAndDeletedAtIsNull(User user);

    int countByWorkSpaceIdAndDeletedAtIsNull(int workSpaceId);
}
