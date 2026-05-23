package com.mtc.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mtc.entity.Message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

	Page<Message> findAllByChannelIdAndDeletedFalseOrderBySequenceNumAsc(UUID channelId, Pageable pageable);

	@Query("SELECT m FROM Message m " + "WHERE m.channel.id = :channelId " + "AND m.sequenceNum > :afterSeq "
			+ "AND m.deleted = false " + "ORDER BY m.sequenceNum ASC")
	List<Message> findMissedMessages(@Param("channelId") UUID channelId, @Param("afterSeq") Long afterSeq);

	@Query("SELECT m FROM Message m " + "WHERE m.channel.id = :channelId " + "AND m.sequenceNum < :beforeSeq "
			+ "AND m.deleted = false " + "ORDER BY m.sequenceNum DESC")
	Page<Message> findBeforeSeq(@Param("channelId") UUID channelId, @Param("beforeSeq") Long beforeSeq,
			Pageable pageable);

	Optional<Message> findByChannelIdAndClientMsgId(UUID channelId, String clientMsgId);

	@Query("SELECT COALESCE(MAX(m.sequenceNum), 0) FROM Message m WHERE m.channel.id = :channelId")
	Long findMaxSequenceByChannelId(@Param("channelId") UUID channelId);
}
