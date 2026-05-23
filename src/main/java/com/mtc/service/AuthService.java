package com.mtc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mtc.dto.AuthDto;
import com.mtc.dto.UserDto;
import com.mtc.entity.User;
import com.mtc.exception.BadRequestException;
import com.mtc.repository.UserRepository;
import com.mtc.security.JwtTokenProvider;
import com.mtc.security.UserPrincipal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	private final AuthenticationManager authenticationManager;

	private final JwtTokenProvider tokenProvider;

	@Transactional
	public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new BadRequestException("Email already in use: " + request.getEmail());
		}
		if (userRepository.existsByUsername(request.getUsername())) {
			throw new BadRequestException("Username already taken: " + request.getUsername());
		}

		User user = User.builder().email(request.getEmail().toLowerCase()).username(request.getUsername().toLowerCase())
				.password(passwordEncoder.encode(request.getPassword()))
				.displayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername())
				.build();

		user = userRepository.save(user);
		log.info("New user registered: {}", user.getEmail());

		String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());
		String refreshToken = tokenProvider.generateRefreshToken(user.getId());

		return AuthDto.AuthResponse.builder().accessToken(accessToken).refreshToken(refreshToken)
				.user(UserDto.from(user)).build();
	}

	public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase(), request.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);
		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

		User user = userRepository.findById(principal.getId())
				.orElseThrow(() -> new BadRequestException("User not found"));

		String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());
		String refreshToken = tokenProvider.generateRefreshToken(user.getId());

		return AuthDto.AuthResponse.builder().accessToken(accessToken).refreshToken(refreshToken)
				.user(UserDto.from(user)).build();
	}

	public AuthDto.AuthResponse refresh(AuthDto.RefreshRequest request) {
		String refreshToken = request.getRefreshToken();
		if (!tokenProvider.validateToken(refreshToken)) {
			throw new BadRequestException("Invalid refresh token");
		}

		java.util.UUID userId = tokenProvider.getUserIdFromToken(refreshToken);
		User user = userRepository.findById(userId).orElseThrow(() -> new BadRequestException("User not found"));

		String newAccessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());
		String newRefreshToken = tokenProvider.generateRefreshToken(user.getId());

		return AuthDto.AuthResponse.builder().accessToken(newAccessToken).refreshToken(newRefreshToken)
				.user(UserDto.from(user)).build();
	}
}
