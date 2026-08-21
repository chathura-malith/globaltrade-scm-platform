package com.globaltrade.web.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;

@ApplicationScoped
public class JwtAuthMechanism implements HttpAuthenticationMechanism {

    @Inject
    private IdentityStore identityStore;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
                                                HttpServletResponse response,
                                                HttpMessageContext httpMessageContext) throws AuthenticationException {

        String path = request.getRequestURI();

        if (path.contains("auth/login") ||
                path.contains("/auth/register") ||
                path.contains("/auth/refresh")) {
            return httpMessageContext.doNothing();
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            CredentialValidationResult result = identityStore.validate(new JwtCredential(token));

            if (result.getStatus() == CredentialValidationResult.Status.VALID) {
                return httpMessageContext.notifyContainerAboutLogin(result);
            }
        }


        return httpMessageContext.doNothing();
    }
}