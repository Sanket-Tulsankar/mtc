package com.mtc.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.mtc.dto.WorkspaceDto;
import com.mtc.security.UserPrincipal;
import com.mtc.service.WorkspaceService;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

	private final WorkspaceService workspaceService;

	@PostMapping
	public ResponseEntity<WorkspaceDto.Response> createWorkspace(@Valid @RequestBody WorkspaceDto.CreateRequest request,
			@AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(workspaceService.createWorkspace(request, principal.getId()));
	}

	@GetMapping
	public ResponseEntity<List<WorkspaceDto.Response>> getMyWorkspaces(
			@AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.ok(workspaceService.getUserWorkspaces(principal.getId()));
	}

	@GetMapping("/{workspaceId}")
	public ResponseEntity<WorkspaceDto.Response> getWorkspace(@PathVariable UUID workspaceId,
			@AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.ok(workspaceService.getWorkspace(workspaceId, principal.getId()));
	}

	@GetMapping("/{workspaceId}/members")
	public ResponseEntity<List<WorkspaceDto.MemberResponse>> getMembers(@PathVariable UUID workspaceId,
			@AuthenticationPrincipal UserPrincipal principal) {
		return ResponseEntity.ok(workspaceService.getMembers(workspaceId, principal.getId()));
	}

	@PostMapping("/{workspaceId}/invite")
	public ResponseEntity<Map<String, String>> inviteMember(@PathVariable UUID workspaceId,
			@Valid @RequestBody WorkspaceDto.InviteRequest request, @AuthenticationPrincipal UserPrincipal principal) {
		String token = workspaceService.inviteMember(workspaceId, request, principal.getId());
		return ResponseEntity
				.ok(Map.of("inviteToken", token, "message", "Invitation created. Share this token with the user."));
	}
}
