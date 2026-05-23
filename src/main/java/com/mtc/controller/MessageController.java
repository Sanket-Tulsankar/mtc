package com.mtc.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.mtc.dto.MessageDto;
import com.mtc.security.UserPrincipal;
import com.mtc.service.MessageService;

import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/channels/{channelId}/messages")
@RequiredArgsConstructor
public class MessageController {

	private final MessageService messageService;

	/** REST fallback for sending messages (WebSocket preferred) */
	@PostMapping
	public ResponseEntity<MessageDto.Response> sendMessage(@PathVariable UUID workspaceId, @PathVariable UUID channelId,
			@Valid @RequestBody MessageDto.SendRequest request, @AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(messageService.sendMessage(workspaceId, channelId, request, principal.getId()));
	}

	/** Paginated message history */
	@GetMapping
	public ResponseEntity<MessageDto.PagedResponse> getHistory(@PathVariable UUID workspaceId,
			@PathVariable UUID channelId, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size, @AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.ok(messageService.getHistory(workspaceId, channelId, principal.getId(), page, size));
	}

	/** Fetch missed messages since a sequence number (reconnect) */
	@GetMapping("/missed")
	public ResponseEntity<?> getMissed(@PathVariable UUID workspaceId, @PathVariable UUID channelId,
			@RequestParam Long afterSeq, @AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.ok(messageService.getMissedMessages(channelId, afterSeq, principal.getId()));
	}

	@DeleteMapping("/{messageId}")
	public ResponseEntity<MessageDto.Response> deleteMessage(@PathVariable UUID workspaceId,
			@PathVariable UUID channelId, @PathVariable UUID messageId,
			@AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.ok(messageService.deleteMessage(workspaceId, channelId, messageId, principal.getId()));
	}
}
