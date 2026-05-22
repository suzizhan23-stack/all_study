package com.wordlearning.service;

import com.wordlearning.dto.request.LoginRequest;
import com.wordlearning.dto.request.RegisterRequest;
import com.wordlearning.dto.response.LoginResponse;
import com.wordlearning.entity.User;
import com.wordlearning.entity.UserStat;
import com.wordlearning.exception.BusinessException;
import com.wordlearning.repository.UserRepository;
import com.wordlearning.repository.UserStatRepository;
import com.wordlearning.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final UserStatRepository userStatRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public LoginResponse register(RegisterRequest req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw BusinessException.conflict("username already exists");
        }
        if (req.getEmail() != null && !req.getEmail().isBlank()
                && userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw BusinessException.conflict("email already exists");
        }

        User user = User.builder()
                .username(req.getUsername())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .email(req.getEmail())
                .nickname(req.getNickname() != null ? req.getNickname() : req.getUsername())
                .role(User.Role.user)
                .permissionLevel(0)
                .isActive(true)
                .build();
        userRepository.save(user);

        UserStat stat = UserStat.builder()
                .userId(user.getId())
                .xp(0)
                .level(1)
                .streakDays(0)
                .longestStreak(0)
                .totalWordsLearned(0)
                .totalReviews(0)
                .totalTimeSpentSec(0)
                .isPublic(false)
                .build();
        userStatRepository.save(stat);

        String token = jwtUtil.generateToken(user.getUuid(), user.getUsername(), user.getRole().name());
        return LoginResponse.builder()
                .token(token)
                .expiresIn(expirationMs / 1000)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getUuid())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .avatarUrl(user.getAvatarUrl())
                        .role(user.getRole().name())
                        .level(stat.getLevel())
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> BusinessException.badRequest("invalid username or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw BusinessException.badRequest("invalid username or password");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        UserStat stat = userStatRepository.findByUserId(user.getId())
                .orElse(null);

        String token = jwtUtil.generateToken(user.getUuid(), user.getUsername(), user.getRole().name());
        return LoginResponse.builder()
                .token(token)
                .expiresIn(expirationMs / 1000)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getUuid())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .avatarUrl(user.getAvatarUrl())
                        .role(user.getRole().name())
                        .level(stat != null ? stat.getLevel() : 1)
                        .build())
                .build();
    }

    public String getUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
    }
}
