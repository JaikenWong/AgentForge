package com.agentforge.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final ObjectMapper objectMapper;

    @Value("${feishu.app-id}")
    private String appId;

    @Value("${feishu.app-secret}")
    private String appSecret;

    @Value("${feishu.redirect-uri}")
    private String redirectUri;

    @Value("${feishu.scope:}")
    private String scope;

    private static final String FEISHU_AUTHORIZE_URL = "https://accounts.feishu.cn/open-apis/authen/v1/authorize";
    private static final String FEISHU_TOKEN_URL = "https://open.feishu.cn/open-apis/authen/v2/oauth/token";
    private static final String FEISHU_USER_INFO_URL = "https://open.feishu.cn/open-apis/authen/v1/user_info";

    public String getFeishuAuthUrl() {
        requireConfigured();
        String encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
        StringBuilder url = new StringBuilder(FEISHU_AUTHORIZE_URL)
            .append("?client_id=").append(appId)
            .append("&redirect_uri=").append(encodedRedirect)
            .append("&state=agentforge");
        if (scope != null && !scope.isBlank()) {
            url.append("&scope=").append(URLEncoder.encode(scope, StandardCharsets.UTF_8));
        }
        return url.toString();
    }

    public OAuthUser getFeishuUserInfo(String code) {
        requireConfigured();
        try {
            String userAccessToken = exchangeCodeForToken(code);
            return fetchUserInfo(userAccessToken);
        } catch (Exception e) {
            log.error("Failed to get Feishu user info: {}", e.getMessage());
            throw new RuntimeException("Failed to get Feishu user info: " + e.getMessage(), e);
        }
    }

    private String exchangeCodeForToken(String code) throws Exception {
        Map<String, String> body = Map.of(
            "grant_type", "authorization_code",
            "client_id", appId,
            "client_secret", appSecret,
            "code", code,
            "redirect_uri", redirectUri
        );

        String response = RestClient.builder().build().post()
            .uri(FEISHU_TOKEN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        if (root.path("code").asInt(-1) != 0) {
            throw new RuntimeException("Feishu token error: " + root.path("msg").asText());
        }
        return root.path("access_token").asText();
    }

    private OAuthUser fetchUserInfo(String userAccessToken) throws Exception {
        String response = RestClient.builder().build().get()
            .uri(FEISHU_USER_INFO_URL)
            .header("Authorization", "Bearer " + userAccessToken)
            .retrieve()
            .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        if (root.path("code").asInt(-1) != 0) {
            throw new RuntimeException("Feishu user_info error: " + root.path("msg").asText());
        }

        JsonNode data = root.path("data");
        OAuthUser user = new OAuthUser();
        user.setOpenId(data.path("open_id").asText());
        user.setUnionId(data.path("union_id").asText());
        user.setDisplayName(data.path("name").asText());
        user.setEmail(data.path("email").asText());
        JsonNode avatarNode = data.path("avatar_url");
        String avatar = avatarNode.isMissingNode() || avatarNode.isNull() ? "" : avatarNode.asText();
        if (avatar.isBlank()) {
            JsonNode big = data.path("avatar_big");
            avatar = big.isMissingNode() || big.isNull() ? "" : big.asText();
        }
        user.setAvatarUrl(avatar);
        return user;
    }

    private void requireConfigured() {
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new IllegalStateException("FEISHU_APP_ID and FEISHU_APP_SECRET must be configured");
        }
    }

    @lombok.Data
    public static class OAuthUser {
        private String openId;
        private String unionId;
        private String displayName;
        private String email;
        private String avatarUrl;
    }
}
