package com.globaltrade.ejb.service;

import jakarta.ejb.Stateless;

@Stateless
public class SystemBootService {

    public String getStatus() {
        return "GlobalTrade EJB Core Engine Initialized Successfully.";
    }
}