package com.estela.neko.core;

import com.estela.neko.common.AccountModel;
import com.estela.neko.common.HttpHelper;
import com.estela.neko.common.StrategyStatus;
import com.estela.neko.huobi.api.ApiClient;
import com.sun.deploy.net.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * @author fuming.lj 2018/7/23
 **/
@RequestMapping("/tradeService")
@RestController
public class TradeSerivce {
    private static final Logger logger = LoggerFactory.getLogger(TradeSerivce.class);
    @Autowired
    StrategyStatus tradeStatus;
    @Autowired
    AccountModel accountModel;
    @Autowired
    PriceStrategy priceStrategy;

    @Autowired
    HttpHelper helper;
    @Autowired
    ApiClient apiClient;

    @RequestMapping("/price")
    public Object getPrice(){
        Object ob=helper.get("https://api.huobipro.com/market/tradesymbol=htusdt&AccessKeyId="+"a7fd725a-502746cd-69b903fd-4418a");
        return ob;


    }


    @RequestMapping("/setProperties")
    public StrategyStatus setProperties(String riskPrice,String maxOrderSize ,String lotSize ,String diffPrice){
        try{
            if(!StringUtils.isEmpty(riskPrice)){
                //乘以10000 以上
                tradeStatus.setRiskPrice(new BigDecimal(riskPrice));
            }
            if(!StringUtils.isEmpty(maxOrderSize)){

                tradeStatus.setMaxOrderSize(Integer.valueOf(maxOrderSize));
            }
            if(!StringUtils.isEmpty(lotSize)){

                tradeStatus.setLotSize(Double.valueOf(lotSize));
            }

            if(!StringUtils.isEmpty(diffPrice)){

                tradeStatus.setDiffPrice(Integer.valueOf(diffPrice));
            }



        }catch (Exception e){
            logger.error("设置参数错误",e);
            tradeStatus.setSysMsg("设置参数非法");
        }




        return tradeStatus;
    }

    @RequestMapping("/look")
    public StrategyStatus queryStatus(){
        return tradeStatus;
    }


    @RequestMapping("/go")
    public StrategyStatus go(String startOrder){
        if(!StringUtils.isEmpty(startOrder)){
            tradeStatus.setStartOrder(new BigDecimal(startOrder));
        }
        accountModel.setKey("a7fd725a-502746cd-69b903fd-4418a","5774a589-a4b36db6-382fdc6f-6bbae");

        tradeStatus.setTrading(true);
        priceStrategy.startReflashPrice();


        return tradeStatus;
    }

    @RequestMapping("/start")
    public StrategyStatus start(@RequestParam(required = true) String accessKey, @RequestParam(required = true)String securityKey, BigDecimal startOrderPrice){
        if(StringUtils.isEmpty(accessKey)|| StringUtils.isEmpty(securityKey)){
            tradeStatus.setSysMsg("输入参数异常,请检查重试");
            logger.error("输入参数有误:"+accessKey+":"+accessKey+",securityKey:"+securityKey);
            return tradeStatus;
        }
        if(tradeStatus.getTrading()){
            logger.info("系统已经启动");
            return tradeStatus;
        }
        tradeStatus.setStartOrder(startOrderPrice);
        accountModel.setKey(accessKey,securityKey);
        tradeStatus.setTrading(true);
        priceStrategy.startReflashPrice();




        return tradeStatus;
    }


    @RequestMapping("/api")
    public String helloWorld(){
        return " hello world ";
    }
}
