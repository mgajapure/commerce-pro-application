package com.commerce_pro_backend.user_identity.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.commerce_pro_backend.common.dto.ApiResponse;
import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.user_identity.dto.AuthRequest;
import com.commerce_pro_backend.user_identity.dto.AuthResponse;
import com.commerce_pro_backend.user_identity.dto.ChangePasswordRequest;
import com.commerce_pro_backend.user_identity.dto.MfaDisableRequest;
import com.commerce_pro_backend.user_identity.dto.MfaSetupResponse;
import com.commerce_pro_backend.user_identity.dto.MfaVerificationRequest;
import com.commerce_pro_backend.user_identity.dto.RefreshTokenRequest;
import com.commerce_pro_backend.user_identity.service.AuthService;
import com.commerce_pro_backend.user_identity.service.CurrentUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, logout, and token management")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and get JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.authenticate(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refreshToken(request.getRefreshToken())));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user and invalidate tokens")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader("Authorization") String authHeader) {
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw ApiException.badRequest("Authorization header must use Bearer token");
        }
        String token = authHeader.substring(7);
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password for authenticated user")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUserService.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @PostMapping("/mfa/setup")
    @Operation(summary = "Initialize MFA setup and return TOTP secret + otpauth URL")
    public ResponseEntity<ApiResponse<MfaSetupResponse>> setupMfa() {
        return ResponseEntity.ok(ApiResponse.success(authService.setupMfa(currentUserService.getCurrentUserId())));
    }

    @PostMapping("/mfa/enable")
    @Operation(summary = "Enable MFA by verifying a TOTP code")
    public ResponseEntity<ApiResponse<String>> enableMfa(@Valid @RequestBody MfaVerificationRequest request) {
        authService.enableMfa(currentUserService.getCurrentUserId(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success("MFA enabled successfully"));
    }

    @PostMapping("/mfa/disable")
    @Operation(summary = "Disable MFA using current password and TOTP code")
    public ResponseEntity<ApiResponse<String>> disableMfa(@Valid @RequestBody MfaDisableRequest request) {
        authService.disableMfa(currentUserService.getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("MFA disabled successfully"));
    }
}
