package com.mtc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank 
        @Email
        private String email;

        @NotBlank @Size(min = 3, max = 30)
        private String username;

        @NotBlank @Size(min = 6, max = 100)
        private String password;

        private String displayName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank @Email
        private String email;

        @NotBlank
        private String password;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        
    	private String accessToken;
        
    	private String refreshToken;
        
    	private String tokenType = "Bearer";
        
    	private UserDto user;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshRequest {
        
    	@NotBlank
        private String refreshToken;
    }
}
