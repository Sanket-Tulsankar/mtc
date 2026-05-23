package com.mtc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mtc.dto.ChannelDto;
import com.mtc.entity.Channel;
import com.mtc.entity.ChannelMember;
import com.mtc.entity.User;
import com.mtc.entity.Workspace;
import com.mtc.entity.WorkspaceMember;
import com.mtc.exception.BadRequestException;
import com.mtc.exception.ForbiddenException;
import com.mtc.exception.ResourceNotFoundException;
import com.mtc.repository.ChannelMemberRepository;
import com.mtc.repository.ChannelRepository;
import com.mtc.repository.UserRepository;
import com.mtc.repository.WorkspaceMemberRepository;
import com.mtc.repository.WorkspaceRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    
    private final ChannelMemberRepository channelMemberRepository;
    
    private final WorkspaceMemberRepository workspaceMemberRepository;
    
    private final WorkspaceRepository workspaceRepository;
    
    private final UserRepository userRepository;

    @Transactional
    public ChannelDto.Response createChannel(UUID workspaceId,
                                              ChannelDto.CreateRequest request,
                                              UUID userId) {
        verifyWorkspaceMember(workspaceId, userId);

        if (channelRepository.existsByWorkspaceIdAndName(workspaceId, request.getName())) {
            throw new BadRequestException("Channel name already exists: " + request.getName());
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Channel channel = Channel.builder()
                .workspace(workspace)
                .name(request.getName().toLowerCase())
                .description(request.getDescription())
                .privateChannel(request.isPrivateChannel())
                .createdBy(creator)
                .build();
        channel = channelRepository.save(channel);

        ChannelMember member = ChannelMember.builder()
                .channel(channel)
                .user(creator)
                .build();
        channelMemberRepository.save(member);

        log.info("Channel created: #{} in workspace {}", channel.getName(), workspaceId);
        return ChannelDto.Response.from(channel);
    }

    @Transactional(readOnly = true)
    public List<ChannelDto.Response> getChannels(UUID workspaceId, UUID userId) {
        verifyWorkspaceMember(workspaceId, userId);
        return channelRepository.findAllByWorkspaceIdAndMemberUserId(workspaceId, userId)
                .stream()
                .map(ChannelDto.Response::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void joinChannel(UUID workspaceId, UUID channelId, UUID userId) {
        verifyWorkspaceMember(workspaceId, userId);
        Channel channel = getChannelInWorkspace(channelId, workspaceId);

        if (channel.isPrivateChannel()) {
			throw new ForbiddenException("Cannot join a private channel without an invite");
        }
        if (channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)) {
            throw new BadRequestException("Already a member of this channel");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        channelMemberRepository.save(ChannelMember.builder()
                .channel(channel).user(user).build());
    }

    @Transactional
    public void leaveChannel(UUID workspaceId, UUID channelId, UUID userId) {
        verifyWorkspaceMember(workspaceId, userId);
        getChannelInWorkspace(channelId, workspaceId);
        channelMemberRepository.deleteByChannelIdAndUserId(channelId, userId);
    }

    @Transactional
    public void archiveChannel(UUID workspaceId, UUID channelId, UUID userId) {
        verifyWorkspaceAdmin(workspaceId, userId);
        Channel channel = getChannelInWorkspace(channelId, workspaceId);
        channel.setArchived(true);
        channelRepository.save(channel);
    }

    private void verifyWorkspaceMember(UUID workspaceId, UUID userId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new ForbiddenException("Not a member of this workspace");
        }
    }

    private void verifyWorkspaceAdmin(UUID workspaceId, UUID userId) {
        WorkspaceMember member = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ForbiddenException("Not a member"));
        if (member.getRole() == WorkspaceMember.Role.MEMBER) {
            throw new ForbiddenException("Admin permission required");
        }
    }

    public Channel getChannelInWorkspace(UUID channelId, UUID workspaceId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel", channelId));
        if (!channel.getWorkspace().getId().equals(workspaceId)) {
            throw new ForbiddenException("Channel does not belong to this workspace");
        }
        return channel;
    }

    public void verifyChannelMember(UUID channelId, UUID userId) {
        if (!channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)) {
            throw new ForbiddenException("Not a member of this channel");
        }
    }
}
