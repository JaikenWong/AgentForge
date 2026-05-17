package com.agentforge.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByOauthProviderAndOauthId(String provider, String id);

    User findByOauthId(String oauthId);

    boolean existsByUsername(String username);
}
