package com.agentforge.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public User findByUsername(String username) {
        return userMapper.selectOne(
            Wrappers.<User>lambdaQuery().eq(User::getUsername, username)
        );
    }

    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    public User findByOAuthId(String oauthId) {
        return userMapper.selectOne(
            Wrappers.<User>lambdaQuery().eq(User::getOauthId, oauthId)
        );
    }

    public User findByOAuthProviderAndId(String provider, String oauthId) {
        return userMapper.selectOne(
            Wrappers.<User>lambdaQuery()
                .eq(User::getOauthProvider, provider)
                .eq(User::getOauthId, oauthId)
        );
    }

    public User create(String username, String rawPassword, String displayName, Long tenantId) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setDisplayName(displayName != null ? displayName : username);
        user.setTenantId(tenantId != null ? tenantId : 1L);
        user.setRole("user");
        user.setStatus("active");
        userMapper.insert(user);
        return user;
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
        userMapper.insert(user);
        return user;
    }

    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }
}
