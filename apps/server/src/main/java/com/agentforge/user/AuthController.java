package com.agentforge.user;

import com.agentforge.security.JwtTokenProvider;
import com.agentforge.security.OAuthService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final OAuthService oauthService;
    private final InvitationService invitationService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userService.findByUsername(request.getUsername());
        if (user == null || !userService.checkPassword(user, request.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getTenantId(), user.getRole());
        return ResponseEntity.ok(Map.of(
            "token", token,
            "user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "displayName", user.getDisplayName(),
                "role", user.getRole(),
                "tenantId", user.getTenantId(),
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""
            )
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userService.findByUsername(request.getUsername()) != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }

        // Validate invitation if provided
        Long tenantId = request.getTenantId();
        if (request.getInvitationCode() != null) {
            Invitation invitation = invitationService.validateInvitation(request.getInvitationCode());
            tenantId = invitation.getTenantId();
        }

        User user = userService.create(
            request.getUsername(),
            request.getPassword(),
            request.getDisplayName(),
            tenantId
        );

        // Mark invitation as used if provided
        if (request.getInvitationCode() != null) {
            invitationService.useInvitation(request.getInvitationCode(), user.getId());
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getTenantId(), user.getRole());
        return ResponseEntity.ok(Map.of(
            "token", token,
            "user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "displayName", user.getDisplayName(),
                "role", user.getRole(),
                "tenantId", user.getTenantId()
            )
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestAttribute("userId") Long userId) {
        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "displayName", user.getDisplayName(),
            "role", user.getRole(),
            "tenantId", user.getTenantId(),
            "avatarUrl", user.getAvatarUrl(),
            "email", user.getEmail()
        ));
    }

    @GetMapping("/feishu/url")
    public ResponseEntity<?> getFeishuAuthUrl() {
        String url = oauthService.getFeishuAuthUrl();
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/feishu/callback")
    public ResponseEntity<?> feishuCallback(@RequestParam String code) {
        try {
            OAuthService.OAuthUser oauthUser = oauthService.getFeishuUserInfo(code);

            // Find or create user
            User user = userService.findByOAuthProviderAndId("feishu", oauthUser.getOpenId());
            if (user == null) {
                user = userService.createOAuthUser(
                    oauthUser.getOpenId(),
                    oauthUser.getUnionId(),
                    oauthUser.getDisplayName(),
                    oauthUser.getEmail(),
                    oauthUser.getAvatarUrl()
                );
            }

            String token = jwtTokenProvider.generateToken(user.getId(), user.getTenantId(), user.getRole());
            return ResponseEntity.ok(Map.of(
                "token", token,
                "user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "displayName", user.getDisplayName(),
                    "role", user.getRole(),
                    "tenantId", user.getTenantId(),
                    "avatarUrl", user.getAvatarUrl()
                )
            ));
        } catch (Exception e) {
            log.error("Feishu OAuth failed: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "OAuth failed: " + e.getMessage()));
        }
    }

    @Data
    static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    static class RegisterRequest {
        private String username;
        private String password;
        private String displayName;
        private Long tenantId;
        private String invitationCode;
    }
}
