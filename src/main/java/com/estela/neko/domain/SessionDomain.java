package com.estela.neko.domain;

import org.springframework.stereotype.Component;

/**
 * @author fuming.lj 2018/7/30
 **/
@Component
public class SessionDomain {
    private String sessionKey;
    private long loginTime;

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public long getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(long loginTime) {
        this.loginTime = loginTime;
    }
}
