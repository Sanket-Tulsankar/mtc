package com.mtc.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.mtc.dto.UserDto;
import com.mtc.dto.WorkspaceDto;
import com.mtc.entity.User;
import com.mtc.repository.UserRepository;
import com.mtc.security.UserPrincipal;
import com.mtc.service.PresenceService;
import com.mtc.service.WorkspaceService;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MiscController {

	private final WorkspaceService workspaceService;

	private final PresenceService presenceService;

	private final UserRepository userRepository;

	@PostMapping("/api/invitations/accept/{token}")
	public ResponseEntity<WorkspaceDto.Response> acceptInvitation(@PathVariable String token,
			@AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.ok(workspaceService.acceptInvitation(token, principal.getId()));
	}

	@GetMapping("/api/me")
	public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal UserPrincipal principal) {
		User user = userRepository.findById(principal.getId()).orElseThrow();
		UserDto dto = UserDto.from(user);
		dto.setOnline(presenceService.isOnline(principal.getId()));
		return ResponseEntity.ok(dto);
	}

	@GetMapping("/api/workspaces/{workspaceId}/presence")
	public ResponseEntity<Map<String, Object>> getPresence(@PathVariable UUID workspaceId,
			@AuthenticationPrincipal UserPrincipal principal) {
		Set<UUID> onlineUsers = presenceService.getOnlineUsers();
		return ResponseEntity.ok(Map.of("onlineUserIds", onlineUsers));
	}
}