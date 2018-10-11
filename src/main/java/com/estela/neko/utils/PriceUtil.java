package com.estela.neko.utils;

import com.estela.neko.Enutype.FloatEnu;
import com.estela.neko.Enutype.TradeModelType;
import com.estela.neko.common.StrategyStatus;
import com.estela.neko.config.Diamond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author fuming.lj 2018/7/29
 **/
public class PriceUtil {

    private static Logger logger = LoggerFactory.getLogger(PriceUtil.class);

    private StrategyStatus strategyStatus;



    /**
     * 补全满足策略的价格
     *
     * @param currentPrice 当前价格
     * @return
     */
    public int fillPrice(int currentPrice, int lastPrice) {
        int fluctuation = strategyStatus.getFluctuation();

        int aa = lastPrice / fluctuation * fluctuation;
        int bb = currentPrice / fluctuation * fluctuation;
        int result = lastPrice;
        if (aa - bb == 0) {
            //不交易
        } else if (aa - bb > 0) {
            //市场下跌
            result = lastPrice - fluctuation;
        } else {
            result = lastPrice + fluctuation;
        }

        logger.info("价格补全策略,当前交易价格:" + currentPrice + ",上次交易价格:" + lastPrice);

        return result;
    }

}
