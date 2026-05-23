package com.mtc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mtc.entity.WorkspaceMember;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

	Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

	List<WorkspaceMember> findAllByWorkspaceId(UUID workspaceId);

	boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

	void deleteByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
}