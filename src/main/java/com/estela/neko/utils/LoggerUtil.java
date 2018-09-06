package com.estela.neko.utils;

import com.estela.neko.config.Diamond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author fuming.lj 2018/9/6
 * 货币使用的logger
 **/
public class LoggerUtil {
    private static final Logger logger = LoggerFactory.getLogger("HUOBI");

    public void info(String message){
        if(Diamond.HUOBILog){
            logger.info(message);
        }
    }


    public void error(String message,Throwable e){
        if(Diamond.HUOBILog){
            logger.error(message,e);
        }
    }
}
