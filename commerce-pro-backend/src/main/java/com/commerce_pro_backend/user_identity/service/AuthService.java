package com.commerce_pro_backend.user_identity.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.commerce_pro_backend.common.exception.ApiException;
import com.commerce_pro_backend.user_identity.dto.AuthRequest;
import com.commerce_pro_backend.user_identity.dto.AuthResponse;
import com.commerce_pro_backend.user_identity.dto.ChangePasswordRequest;
import com.commerce_pro_backend.user_identity.dto.MfaDisableRequest;
import com.commerce_pro_backend.user_identity.dto.MfaSetupResponse;
import com.commerce_pro_backend.user_identity.entity.RefreshToken;
import com.commerce_pro_backend.user_identity.entity.User;
import com.commerce_pro_backend.user_identity.enums.AuditAction;
import com.commerce_pro_backend.user_identity.repository.RefreshTokenRepository;
import com.commerce_pro_backend.user_identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;
    private final TotpService totpService;
    private final AuditService auditService;
    private final SuperAdminSetupService superAdminSetupService;
    private final EmailNotificationService emailNotificationService;

    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        // Issue 2 FIX: Check account lock BEFORE attempting authentication
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long minutesRemaining = java.time.Duration.between(
                LocalDateTime.now(), user.getLockedUntil()).toMinutes() + 1;
            throw new LockedException("Account is locked. Try again in " + minutesRemaining + " minute(s).");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            // Issue 5 FIX: Verify MFA BEFORE setting the authentication in SecurityContext.
            // If MFA fails the context is never populated.
            if (Boolean.TRUE.equals(user.getMfaEnabled())) {
                if (request.getMfaCode() == null || !verifyMfaCode(user, request.getMfaCode())) {
                    incrementFailedAttempts(user);
                    throw new BadCredentialsException("Invalid MFA code");
                }
            }

            // Only set context after ALL factors are verified
            SecurityContextHolder.getContext().setAuthentication(authentication);

            user.setLastLoginAt(LocalDateTime.now());
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);

            boolean isSuperAdmin = superAdminSetupService.isSuperAdmin(user.getUsername());
            String accessToken = jwtTokenProvider.generateToken(authentication, isSuperAdmin);

            // Issue 1 FIX: Persist hashed refresh token with a token family
            String rawRefreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());
            String family = UUID.randomUUID().toString();
            persistRefreshToken(rawRefreshToken, user.getUsername(), family);

            auditService.log(user.getId(), AuditAction.LOGIN_SUCCESS, "USER", user.getId(),
                user.getUsername(), user.getUsername(), null, "Login successful", true);

            return buildAuthResponse(accessToken, rawRefreshToken, user, isSuperAdmin);

        } catch (BadCredentialsException | LockedException e) {
            if (!(e instanceof LockedException)) {
                incrementFailedAttempts(user);
            }
            auditService.log(user.getId(), AuditAction.LOGIN_FAILURE, "USER", user.getId(),
                user.getUsername(), user.getUsername(), null, "Failed login: " + e.getMessage(), false);
            throw e;
        }
    }

    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {
        if (!jwtTokenProvider.validateToken(rawRefreshToken) ||
                !jwtTokenProvider.isRefreshToken(rawRefreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String tokenHash = jwtTokenProvider.hashToken(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new BadCredentialsException("Refresh token not recognised"));

        // Issue 1 FIX: Detect token reuse (rotation attack) — revoke the entire family
        if (stored.isRevoked()) {
            log.warn("Refresh token reuse detected for user {}. Revoking entire family {}.",
                stored.getUsername(), stored.getFamily());
            refreshTokenRepository.revokeAllByFamily(
                stored.getFamily(), LocalDateTime.now(), "Token reuse detected - possible theft");
            auditService.log(stored.getUsername(), AuditAction.LOGIN_FAILURE, "USER",
                stored.getUsername(), stored.getUsername(), stored.getUsername(),
                null, "Refresh token reuse detected — all sessions revoked", false);
            throw new BadCredentialsException(
                "Refresh token already used. All sessions revoked for security.");
        }

        if (stored.isExpired()) {
            throw new BadCredentialsException("Refresh token has expired");
        }

        String username = jwtTokenProvider.getUsernameFromToken(rawRefreshToken);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!user.getIsActive()) {
            throw new BadCredentialsException("User account is deactivated");
        }

        // Revoke the consumed token (one-time use rotation)
        stored.setRevoked(true);
        stored.setRevokedAt(LocalDateTime.now());
        stored.setRevocationReason("Rotated");
        refreshTokenRepository.save(stored);

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities()
        );

        boolean isSuperAdmin = superAdminSetupService.isSuperAdmin(username);
        String newAccessToken = jwtTokenProvider.generateToken(authentication, isSuperAdmin);

        // Issue new refresh token in the SAME family for family-wide revocation on logout
        String newRawRefreshToken = jwtTokenProvider.generateRefreshToken(username);
        persistRefreshToken(newRawRefreshToken, username, stored.getFamily());

        return buildAuthResponse(newAccessToken, newRawRefreshToken, user, isSuperAdmin);
    }

    @Transactional
    public void logout(String token) {
        String username = jwtTokenProvider.getUsernameFromToken(token);

        // Issue 1 FIX: Revoke all refresh tokens for this user
        int revoked = refreshTokenRepository.revokeAllByUsername(
            username, LocalDateTime.now(), "User logged out");
        log.info("Revoked {} refresh token(s) for user {} on logout", revoked, username);

        userRepository.findByUsername(username).ifPresent(user ->
            auditService.log(user.getId(), AuditAction.LOGOUT, "USER", user.getId(),
                username, username, null, "User logged out", true));

        SecurityContextHolder.clearContext();
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> ApiException.notFound("User", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword(false);
        userRepository.save(user);

        // Revoke all sessions on password change
        refreshTokenRepository.revokeAllByUsername(
            user.getUsername(), LocalDateTime.now(), "Password changed");

        auditService.log(userId, AuditAction.PASSWORD_CHANGED, "USER", userId,
            user.getUsername(), user.getUsername(), null, "Password changed by user", true);
    }

    @Transactional
    public MfaSetupResponse setupMfa(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> ApiException.notFound("User", userId));

        String secret = totpService.generateSecret();
        user.setMfaSecret(secret);  // Encrypted at rest via @Convert(MfaSecretEncryptor)
        user.setMfaEnabled(false);
        userRepository.save(user);

        String otpAuthUrl = totpService.buildOtpAuthUrl(user.getUsername(), secret);

        auditService.log(userId, AuditAction.MFA_VERIFIED, "USER", userId,
            user.getUsername(), user.getUsername(), null, "MFA setup initiated", true);

        return MfaSetupResponse.builder()
            .secret(secret)
            .otpauthUrl(otpAuthUrl)
            .issuer(totpService.getIssuer())
            .accountName(user.getUsername())
            .build();
    }

    @Transactional
    public void enableMfa(String userId, String code) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> ApiException.notFound("User", userId));

        if (user.getMfaSecret() == null) {
            throw ApiException.badRequest("MFA setup has not been initialized");
        }

        if (!totpService.verifyCode(user.getMfaSecret(), code)) {
            throw ApiException.badRequest("Invalid MFA code");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);

        auditService.log(userId, AuditAction.MFA_ENABLED, "USER", userId,
            user.getUsername(), user.getUsername(), null, "MFA enabled", true);

        // Issue 10 FIX: Send notification email
        emailNotificationService.sendMfaEnabledNotification(user.getEmail(), user.getUsername());
    }

    @Transactional
    public void disableMfa(String userId, MfaDisableRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> ApiException.notFound("User", userId));

        if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw ApiException.badRequest("MFA is not enabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Password is incorrect");
        }

        if (!totpService.verifyCode(user.getMfaSecret(), request.getCode())) {
            throw ApiException.badRequest("Invalid MFA code");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);

        auditService.log(userId, AuditAction.MFA_DISABLED, "USER", userId,
            user.getUsername(), user.getUsername(), null, "MFA disabled", true);

        // Issue 10 FIX: Send notification email
        emailNotificationService.sendMfaDisabledNotification(user.getEmail(), user.getUsername());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void incrementFailedAttempts(User user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            log.warn("Account locked for user {} until {}", user.getUsername(), user.getLockedUntil());
            emailNotificationService.sendAccountLockedNotification(
                user.getEmail(), user.getUsername(), LOCK_DURATION_MINUTES);
        }
        userRepository.save(user);
    }

    private boolean verifyMfaCode(User user, String code) {
        return user.getMfaSecret() != null && totpService.verifyCode(user.getMfaSecret(), code);
    }

    private void persistRefreshToken(String rawToken, String username, String family) {
        long expiryMs = jwtTokenProvider.getRefreshExpirationMs();
        RefreshToken token = RefreshToken.builder()
            .tokenHash(jwtTokenProvider.hashToken(rawToken))
            .username(username)
            .family(family)
            .revoked(false)
            .createdAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusNanos(expiryMs * 1_000_000L))
            .build();
        refreshTokenRepository.save(token);
    }

    private AuthResponse buildAuthResponse(String accessToken, String rawRefreshToken,
                                            User user, boolean isSuperAdmin) {
        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(rawRefreshToken)
            .tokenType("Bearer")
            .expiresIn(900L)
            .issuedAt(LocalDateTime.now())
            .userId(user.getId())
            .username(user.getUsername())
            .superAdmin(isSuperAdmin)
            .build();
    }

    /**
     * Nightly cleanup of expired refresh tokens to prevent unbounded DB growth.
     * Runs at 3 AM daily.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        int deleted = refreshTokenRepository.deleteExpiredBefore(LocalDateTime.now());
        log.info("Purged {} expired refresh tokens", deleted);
    }
}
