package com.estela.neko.login;

import com.estela.neko.domain.SessionDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author fuming.lj 2018/7/30
 * 用于管理session
 **/
@Component
public class SessionControl {
    private static final Logger logger = LoggerFactory.getLogger(SessionControl.class);


    private final static long expireTime = 20*60*1000;
    private static ConcurrentHashMap<String,SessionDomain> sessionMap= new ConcurrentHashMap<>();

    private static ScheduledExecutorService sessionSchedule =  new ScheduledThreadPoolExecutor(1) ;

    static{

        sessionSchedule.scheduleAtFixedRate(()->{

                sessionMap.forEach((key,value)->{
                    try{
                    long currentTime = System.currentTimeMillis();
                    if(currentTime-value.getLoginTime()>expireTime){
                        sessionMap.remove(key);
                    }
                    }catch (Exception e){
                        logger.error("清除session失败: sessionKey:"+key);
                    }
                });



        },4000,6000, TimeUnit.MILLISECONDS);
    }

    public void addSession(String sessionKey){
        SessionDomain sessionDomain = new SessionDomain();
        sessionDomain.setLoginTime(System.currentTimeMillis());
        sessionDomain.setSessionKey(sessionKey);
        sessionMap.put(sessionKey,sessionDomain);
    }


    public boolean isLogin(String sessionKey){
        return sessionMap.containsKey(sessionKey);
    }
}
