package com.mtc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mtc.entity.Workspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

	Optional<Workspace> findBySlug(String slug);

	boolean existsBySlug(String slug);

	@Query("SELECT w FROM Workspace w JOIN w.members m WHERE m.user.id = :userId AND w.active = true")
	List<Workspace> findAllByUserId(@Param("userId") UUID userId);
}
