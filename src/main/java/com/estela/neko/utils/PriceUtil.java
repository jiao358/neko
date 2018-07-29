package com.estela.neko.utils;

import com.estela.neko.Enutype.FloatEnu;
import com.estela.neko.common.StrategyStatus;
import com.estela.neko.config.Diamond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * @author fuming.lj 2018/7/29
 **/
@Service
public class PriceUtil {

    private  static Logger logger = LoggerFactory.getLogger(PriceUtil.class);

    @Autowired
    private static StrategyStatus strategyStatus;

    /**
     * 补全满足策略的价格
     * @param currentPrice 当前价格
     * @return
     */
    public static int fillPrice(int currentPrice,int lastPrice){
        int fluctuation =strategyStatus.getFluctuation();

        int aa =lastPrice/fluctuation *fluctuation;
        int bb =currentPrice/fluctuation *fluctuation;
        int result =lastPrice ;
        if(aa-bb==0){
            //不交易
        }else if( aa-bb >0){
            //市场下跌
            result= lastPrice-fluctuation;
        }else{
            result=  lastPrice + fluctuation;
        }

        logger.info("价格补全策略,当前交易价格:"+currentPrice+",上次交易价格:"+lastPrice);

        return result;
    }

    /**
     * 判断当前的价格是否超过预定阈值 或者低于预定阈值
     */

    public static boolean isOverRishPriceOrLowPrice(int priceNow){
        if(priceNow<strategyStatus.getHighriskPrice()&& priceNow>strategyStatus.getLowRisiPrice()){
            return false;
        }else {
            return true;
        }
    }


    /**
     * 判断当前价格是否 满足交易策略
     *
     * @param currentPrice  当前价格
     * @return
     */
    public static boolean isSatisfyTrading (int currentPrice, int zhengdianPrice) {
        int result = currentPrice-zhengdianPrice;
        int diff = strategyStatus.getDiffPrice();
        if(currentPrice==0){
            return false;
        }

        if(Diamond.floatStrategy.equals(FloatEnu.ALL_FLOAT)){
            if(result<0 && result<= (-1*diff)){
                return true;
            }
            if(result>=0 && result<=diff){
                return true;
            }
        }

        if(Diamond.floatStrategy.equals(FloatEnu.DOWN_FLOAT)){
            if(result<0 && result<= (-1*diff)){
                return true;
            }
        }
        if(Diamond.floatStrategy.equals(FloatEnu.UP_FLOAT)){

            if(result>=0 && result<=diff){
                return true;
            }
        }



        return false;
    }
}
