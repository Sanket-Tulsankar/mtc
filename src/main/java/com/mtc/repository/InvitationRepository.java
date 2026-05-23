package com.mtc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mtc.entity.Invitation;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    
	Optional<Invitation> findByToken(String token);
    
	Optional<Invitation> findByEmailAndWorkspaceIdAndUsedFalse(String email, UUID workspaceId);
}
