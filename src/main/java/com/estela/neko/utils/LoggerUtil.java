package com.estela.neko.utils;

import com.estela.neko.config.Diamond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author fuming.lj 2018/9/6
 * 货币使用的logger
 **/
public class LoggerUtil {
    private static final Logger logger = LoggerFactory.getLogger("HUOBI");

    public static void loggerSellInfo(String symbol, ConcurrentHashMap<Long, Integer> sellOrder){
        logger.info("当前货币对:"+symbol+",空单为:"+sellOrder);

    }

}
