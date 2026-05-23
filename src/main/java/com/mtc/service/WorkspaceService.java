package com.mtc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mtc.dto.WorkspaceDto;
import com.mtc.entity.Channel;
import com.mtc.entity.ChannelMember;
import com.mtc.entity.Invitation;
import com.mtc.entity.User;
import com.mtc.entity.Workspace;
import com.mtc.entity.WorkspaceMember;
import com.mtc.exception.BadRequestException;
import com.mtc.exception.ForbiddenException;
import com.mtc.exception.ResourceNotFoundException;
import com.mtc.repository.ChannelMemberRepository;
import com.mtc.repository.ChannelRepository;
import com.mtc.repository.InvitationRepository;
import com.mtc.repository.UserRepository;
import com.mtc.repository.WorkspaceMemberRepository;
import com.mtc.repository.WorkspaceRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

	private final WorkspaceRepository workspaceRepository;

	private final WorkspaceMemberRepository memberRepository;

	private final UserRepository userRepository;

	private final ChannelRepository channelRepository;

	private final ChannelMemberRepository channelMemberRepository;

	private final InvitationRepository invitationRepository;

	@Transactional
	public WorkspaceDto.Response createWorkspace(WorkspaceDto.CreateRequest request, UUID ownerId) {
		if (workspaceRepository.existsBySlug(request.getSlug())) {
			throw new BadRequestException("Slug already taken: " + request.getSlug());
		}

		User owner = userRepository.findById(ownerId).orElseThrow(() -> new ResourceNotFoundException("User", ownerId));

		Workspace workspace = Workspace.builder().name(request.getName()).slug(request.getSlug())
				.description(request.getDescription()).build();
		workspace = workspaceRepository.save(workspace);

		WorkspaceMember ownerMembership = WorkspaceMember.builder().workspace(workspace).user(owner)
				.role(WorkspaceMember.Role.OWNER).build();
		memberRepository.save(ownerMembership);

		Channel general = Channel.builder().workspace(workspace).name("general").description("General discussion")
				.createdBy(owner).build();
		general = channelRepository.save(general);

		ChannelMember ownerChannelMember = ChannelMember.builder().channel(general).user(owner).build();
		channelMemberRepository.save(ownerChannelMember);

		log.info("Workspace created: slug={}, owner={}", workspace.getSlug(), ownerId);
		return WorkspaceDto.Response.from(workspace);
	}

	@Transactional(readOnly = true)
	public List<WorkspaceDto.Response> getUserWorkspaces(UUID userId) {
		return workspaceRepository.findAllByUserId(userId).stream().map(WorkspaceDto.Response::from)
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public WorkspaceDto.Response getWorkspace(UUID workspaceId, UUID userId) {
		Workspace workspace = getWorkspaceAndVerifyMember(workspaceId, userId);
		return WorkspaceDto.Response.from(workspace);
	}

	@Transactional(readOnly = true)
	public List<WorkspaceDto.MemberResponse> getMembers(UUID workspaceId, UUID userId) {
		getWorkspaceAndVerifyMember(workspaceId, userId);
		return memberRepository.findAllByWorkspaceId(workspaceId).stream().map(WorkspaceDto.MemberResponse::from)
				.collect(Collectors.toList());
	}

	@Transactional
	public String inviteMember(UUID workspaceId, WorkspaceDto.InviteRequest request, UUID inviterId) {
		Workspace workspace = getWorkspaceAndVerifyMember(workspaceId, inviterId);
		verifyRole(workspaceId, inviterId, WorkspaceMember.Role.ADMIN);

		userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
			if (memberRepository.existsByWorkspaceIdAndUserId(workspaceId, user.getId())) {
				throw new BadRequestException("User is already a member");
			}
		});

		String token = UUID.randomUUID().toString();
		Invitation invitation = Invitation.builder().workspaceId(workspaceId).email(request.getEmail().toLowerCase())
				.token(token).invitedBy(inviterId).expiresAt(Instant.now().plusSeconds(7 * 24 * 3600)) // 7 days
				.build();
		invitationRepository.save(invitation);

		log.info("Invitation created for {} to workspace {}", request.getEmail(), workspaceId);
		return token;
	}

	@Transactional
	public WorkspaceDto.Response acceptInvitation(String token, UUID userId) {
		Invitation invitation = invitationRepository.findByToken(token)
				.orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

		if (invitation.isUsed()) {
			throw new BadRequestException("Invitation already used");
		}
		if (invitation.getExpiresAt().isBefore(Instant.now())) {
			throw new BadRequestException("Invitation expired");
		}

		User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));

		if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
			throw new ForbiddenException("Invitation is for a different email address");
		}

		UUID workspaceId = invitation.getWorkspaceId();
		if (memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
			throw new BadRequestException("Already a member of this workspace");
		}

		Workspace workspace = workspaceRepository.findById(workspaceId)
				.orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));

		WorkspaceMember member = WorkspaceMember.builder().workspace(workspace).user(user)
				.role(WorkspaceMember.Role.MEMBER).invitedBy(invitation.getInvitedBy()).build();
		memberRepository.save(member);

		channelRepository.findAllByWorkspaceIdAndArchivedFalse(workspaceId).stream().filter(c -> !c.isPrivateChannel())
				.forEach(c -> {
					if (!channelMemberRepository.existsByChannelIdAndUserId(c.getId(), userId)) {
						channelMemberRepository.save(ChannelMember.builder().channel(c).user(user).build());
					}
				});

		invitation.setUsed(true);
		invitation.setUsedAt(Instant.now());
		invitationRepository.save(invitation);

		log.info("User {} accepted invitation to workspace {}", userId, workspaceId);
		return WorkspaceDto.Response.from(workspace);
	}

	private Workspace getWorkspaceAndVerifyMember(UUID workspaceId, UUID userId) {
		Workspace workspace = workspaceRepository.findById(workspaceId)
				.orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
		if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
			throw new ForbiddenException("Not a member of this workspace");
		}
		return workspace;
	}

	private void verifyRole(UUID workspaceId, UUID userId, WorkspaceMember.Role minRole) {
		WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
				.orElseThrow(() -> new ForbiddenException("Not a member"));
		if (member.getRole().ordinal() > minRole.ordinal()) {
			throw new ForbiddenException("Insufficient permissions");
		}
	}
}
