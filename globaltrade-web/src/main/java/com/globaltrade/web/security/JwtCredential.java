package com.globaltrade.web.security;

import jakarta.security.enterprise.credential.Credential;

public class JwtCredential implements Credential {

    private final String token;

    public JwtCredential(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}