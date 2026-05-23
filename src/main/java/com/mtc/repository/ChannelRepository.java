package com.mtc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mtc.entity.Channel;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, UUID> {

    List<Channel> findAllByWorkspaceIdAndArchivedFalse(UUID workspaceId);

    Optional<Channel> findByWorkspaceIdAndName(UUID workspaceId, String name);

    boolean existsByWorkspaceIdAndName(UUID workspaceId, String name);

    @Query("SELECT c FROM Channel c JOIN c.members m " +
    	    "WHERE c.workspace.id = :workspaceId " +
    	    "AND m.user.id = :userId " +
    	    "AND c.archived = false"
    	   )
    List<Channel> findAllByWorkspaceIdAndMemberUserId(
            @Param("workspaceId") UUID workspaceId,
            @Param("userId") UUID userId);

    @Query("SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END " +
    	    "FROM ChannelMember cm " +
    	    "WHERE cm.channel.id = :channelId " +
    	    "AND cm.user.id = :userId"
    	    )
    boolean isUserMember(@Param("channelId") UUID channelId, @Param("userId") UUID userId);
}