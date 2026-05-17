package com.agentforge.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User findByOAuthId(String oauthId) {
        return userRepository.findByOauthId(oauthId);
    }

    public User findByOAuthProviderAndId(String provider, String oauthId) {
        return userRepository.findByOauthProviderAndOauthId(provider, oauthId).orElse(null);
    }

    public User create(String username, String rawPassword, String displayName, Long tenantId) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setDisplayName(displayName != null ? displayName : username);
        user.setTenantId(tenantId != null ? tenantId : 1L);
        user.setRole("user");
        user.setStatus("active");
        return userRepository.save(user);
    }

    public User createOAuthUser(String openId, String unionId, String displayName, String email, String avatarUrl) {
        User user = new User();
        user.setOauthProvider("feishu");
        user.setOauthId(openId);
        user.setUsername("feishu_" + openId);
        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setAvatarUrl(avatarUrl);
        user.setTenantId(1L);
        user.setRole("user");
        user.setStatus("active");
        return userRepository.save(user);
    }

    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }
}
