package com.estela.neko.common;

import com.alibaba.fastjson.JSONObject;
import com.estela.neko.domain.Result;
import com.estela.neko.login.SessionControl;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * @author fuming.lj 2018/7/31
 * 用于登录session验证
 **/
@Aspect
@Component
public class TradeServiceAopSsl {
    private static final Logger logger = LoggerFactory.getLogger(TradeServiceAopSsl.class);

    @Autowired
    SessionControl sessionControl;
    @Pointcut("execution(public * com.estela.neko.control.*.*(..))")
    public void access(){}

    @Around("access()")
    public Object deBefore(ProceedingJoinPoint pjp ) throws Throwable {
        // 接收到请求，记录请求内容
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        Result result = new Result();
        try {
            String key =request.getSession().getId();
            logger.info("获取key:"+key+"  是否已经登录:"+sessionControl);
            if(sessionControl.isLogin(key)){
                Object o =  pjp.proceed();
                result.setModel(o);
                return JSONObject.toJSON(result);
            }else{
                result.setErrorMsg("请重新登录");
                result.setSuccess(false);
            }
        } catch (Throwable e) {
            logger.error("环绕验证session失败:",e);
            result.setErrorMsg("请重新登录");
            result.setSuccess(false);

        }
        return  JSONObject.toJSON(result);
    }


}
