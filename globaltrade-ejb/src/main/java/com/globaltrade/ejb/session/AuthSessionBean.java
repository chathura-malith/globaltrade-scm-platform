package com.globaltrade.ejb.session;

import com.globaltrade.core.dto.request.LoginRequestDto;
import com.globaltrade.core.dto.request.RegisterRequestDto;
import com.globaltrade.core.dto.request.TokenRefreshRequestDto;
import com.globaltrade.core.dto.response.AuthResponseDto;
import com.globaltrade.core.entity.RefreshToken;
import com.globaltrade.core.entity.User;
import com.globaltrade.core.exception.BusinessRuleViolationException;
import com.globaltrade.core.exception.InvalidInputException;
import com.globaltrade.core.exception.SecurityAuthenticationException;
import com.globaltrade.core.service.AuthService;
import com.globaltrade.core.util.JwtUtil;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.UUID;

@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class AuthSessionBean implements AuthService {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    // Refresh Token Validity: 7 Days
    private static final long REFRESH_TOKEN_DAYS = 7;

    @Override
    public void register(RegisterRequestDto request) {
        // 1. Check if username or email already exists
        if (request.getUsername() == null || request.getUsername().isBlank() ||
                request.getPassword() == null || request.getPassword().isBlank() ||
                request.getEmail() == null || request.getEmail().isBlank() ||
                request.getRole() == null) {
            throw new InvalidInputException("Mandatory registration fields (username, password, email, role) cannot be blank", 400);
        }
        Long count = em.createQuery(
                        "SELECT COUNT(u) FROM User u WHERE u.username = :username OR u.email = :email", Long.class)
                .setParameter("username", request.getUsername())
                .setParameter("email", request.getEmail())
                .getSingleResult();

        if (count > 0) {
            throw new BusinessRuleViolationException("Username or Email already registered in the system", 409);
        }

        // 2. Hash the password using BCrypt
        String salt = BCrypt.gensalt(12);
        String passwordHash = BCrypt.hashpw(request.getPassword(), salt);

        // 3. Create and persist the new User entity
        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .fullName(request.getFullName())
                .organizationName(request.getOrganizationName())
                .role(request.getRole())
                .active(true)
                .build();

        em.persist(newUser);
        em.flush();

    }

    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new SecurityAuthenticationException("Invalid username or password credentials", 401);
        }
        User user;
        try {
            user = em.createQuery(
                            "SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", request.getUsername())
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new SecurityAuthenticationException("Invalid username or password credentials", 401);
        }

        if (!user.isActive()) {
            throw new SecurityAuthenticationException("User account is inactive or suspended", 403);
        }

        // Validate BCrypt password match
        if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
            throw new SecurityAuthenticationException("Invalid username or password credentials", 401);
        }

        return createAuthResponse(user);
    }

    @Override
    public AuthResponseDto refreshToken(TokenRefreshRequestDto request) {
        RefreshToken storedToken;
        try {
            storedToken = em.createQuery(
                            "SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :token", RefreshToken.class)
                    .setParameter("token", request.getRefreshToken())
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new SecurityAuthenticationException("Invalid or non-existent refresh token", 401);
        }

        if (storedToken.isRevoked() || storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new SecurityAuthenticationException("Refresh token has expired or been revoked. Please login again.", 401);
        }

        User user = storedToken.getUser();
        if (!user.isActive()) {
            throw new SecurityAuthenticationException("User account is inactive", 403);
        }

        // Revoke the old token (Token Rotation Security Pattern)
        storedToken.setRevoked(true);
        em.merge(storedToken);

        return createAuthResponse(user);
    }

    @Override
    public void logout(TokenRefreshRequestDto request) {
        try {
            RefreshToken token = em.createQuery(
                            "SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :token", RefreshToken.class)
                    .setParameter("token", request.getRefreshToken())
                    .getSingleResult();

            token.setRevoked(true);
            em.merge(token);
        } catch (NoResultException ignored) {
        }
    }

    private AuthResponseDto createAuthResponse(User user) {
        String accessToken = JwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getOrganizationName()
        );

        String rawRefreshToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(rawRefreshToken)
                .expiryDate(LocalDateTime.now().plusDays(REFRESH_TOKEN_DAYS))
                .revoked(false)
                .build();

        em.persist(refreshToken);

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .expiresIn(JwtUtil.ACCESS_TOKEN_VALIDITY / 1000) // seconds
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}