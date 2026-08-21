package com.globaltrade.web.security;

import com.globaltrade.core.enums.UserRole;
import com.globaltrade.core.util.JwtUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;

import java.util.Set;

@ApplicationScoped
public class AppIdentityStore implements IdentityStore {

    @Override
    public CredentialValidationResult validate(Credential credential) {
        if (credential instanceof JwtCredential jwtCredential) {
            String token = jwtCredential.getToken();

            try {
                String username = JwtUtil.extractUsername(token);
                String roleStr = JwtUtil.extractRole(token);

                if (username != null && !JwtUtil.isTokenExpired(token)) {
                    UserRole role = UserRole.valueOf(roleStr);
                    return new CredentialValidationResult(username, Set.of(role.name()));
                }
            } catch (Exception e) {
                return CredentialValidationResult.INVALID_RESULT;
            }
        }
        return CredentialValidationResult.NOT_VALIDATED_RESULT;
    }

    @Override
    public Set<String> getCallerGroups(CredentialValidationResult validationResult) {
        return validationResult.getCallerGroups();
    }
}

