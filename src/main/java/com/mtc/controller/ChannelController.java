package com.mtc.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.mtc.dto.ChannelDto;
import com.mtc.security.UserPrincipal;
import com.mtc.service.ChannelService;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/channels")
@RequiredArgsConstructor
public class ChannelController {

	private final ChannelService channelService;

	@PostMapping
	public ResponseEntity<ChannelDto.Response> createChannel(@PathVariable UUID workspaceId,
			@Valid @RequestBody ChannelDto.CreateRequest request, @AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(channelService.createChannel(workspaceId, request, principal.getId()));
	}

	@GetMapping
	public ResponseEntity<List<ChannelDto.Response>> getChannels(@PathVariable UUID workspaceId,
			@AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.ok(channelService.getChannels(workspaceId, principal.getId()));
	}

	@PostMapping("/{channelId}/join")
	public ResponseEntity<Void> joinChannel(@PathVariable UUID workspaceId, @PathVariable UUID channelId,
			@AuthenticationPrincipal UserPrincipal principal) {
		channelService.joinChannel(workspaceId, channelId, principal.getId());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/{channelId}/leave")
	public ResponseEntity<Void> leaveChannel(@PathVariable UUID workspaceId, @PathVariable UUID channelId,
			@AuthenticationPrincipal UserPrincipal principal) {
		channelService.leaveChannel(workspaceId, channelId, principal.getId());
		return ResponseEntity.ok().build();
	}

	@PostMapping("/{channelId}/archive")
	public ResponseEntity<Void> archiveChannel(@PathVariable UUID workspaceId, @PathVariable UUID channelId,
			@AuthenticationPrincipal UserPrincipal principal) {
		channelService.archiveChannel(workspaceId, channelId, principal.getId());
		return ResponseEntity.ok().build();
	}
}
