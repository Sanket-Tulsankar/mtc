package com.mtc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

import com.mtc.entity.User;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

	private UUID id;
	private String email;
	private String username;
	private String displayName;
	private String avatarUrl;
	private boolean online;
	private Instant createdAt;

	public static UserDto from(User user) {
		return UserDto.builder().id(user.getId()).email(user.getEmail()).username(user.getUsername())
				.displayName(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername())
				.avatarUrl(user.getAvatarUrl()).createdAt(user.getCreatedAt()).build();
	}
}
