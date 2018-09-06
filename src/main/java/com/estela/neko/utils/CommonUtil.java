package com.estela.neko.utils;

import com.alibaba.fastjson.JSONObject;
import com.estela.neko.common.FundReport;
import com.estela.neko.common.StrategyStatus;
import com.estela.neko.config.Diamond;
import com.estela.neko.core.PriceMemery;
import com.estela.neko.core.PriceStrategy;
import com.estela.neko.domain.SystemModel;
import com.estela.neko.huobi.api.ApiNewClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author fuming.lj 2018/7/31
 **/
@Service
public class CommonUtil {
    private  static Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    @Autowired
    PriceStrategy priceStrategy;
    @Autowired
    Diamond diamond;
    @Autowired
    PriceMemery priceMemery;
    @Autowired
    StrategyStatus strategyStatus;
    @Autowired
    FundReport fundReport;

    @Autowired
    ApiNewClient apiNewClient;

    volatile String time ;

    boolean initSuccess =true;

    List<JSONObject> accountAmountList = new ArrayList<>();


    public SystemModel generateCurrentModel(){
        if(time==null || !initSuccess){
            time = priceStrategy.getChinaTime();
            init();
        }


        SystemModel systemModel = new SystemModel();
        systemModel.setDiffPrice(strategyStatus.getDiffPrice());
        systemModel.setFluctuation(strategyStatus.getFluctuation());
        systemModel.setHighriskPrice(strategyStatus.getHighriskPrice());
        systemModel.setLotSize(strategyStatus.getLotSize());
        systemModel.setLowRisiPrice(strategyStatus.getLowRisiPrice());
        systemModel.setMaxOrderSize(strategyStatus.getMaxOrderSize());
        systemModel.setCurrentPrice(PriceMemery.priceNow);
        systemModel.setFloatStrategy(Diamond.floatStrategy.getValue());
        systemModel.setRunning(Diamond.canRunning?"运行中":"暂停中");
        List<Integer> buyOrderList = priceStrategy.price_order.stream().collect(Collectors.toList());
        List<Integer> sellOrderList = priceStrategy.sell_order.stream().collect(Collectors.toList());

        Collections.sort(buyOrderList);
        Collections.sort(sellOrderList);
        systemModel.setBuyAndOrder(JSONObject.toJSONString(priceStrategy.buyOrder));
        systemModel.setSellAndOrder(JSONObject.toJSONString(priceStrategy.sellOrder));

        systemModel.setTodayTrade(strategyStatus.getTodayCompleteTrade().get());
        systemModel.setBuyOrder(buyOrderList);
        systemModel.setSellOrder(sellOrderList);
        systemModel.setAlreadyDual(strategyStatus.getCompleteTrade());
        systemModel.setGhost(Diamond.HUOBILog);
        // 进行费率计算
      /*  systemModel.setProfit(fundReport.getProfit());
        systemModel.setBuyFee(fundReport.getBuyFee());
        systemModel.setSellFee(fundReport.getSellFee());*/

        //当前USDT余额以及冻结
       /* if(time.equals(priceStrategy.getChinaTime())){
            setAmountModel(systemModel);
        }else{
            time = priceStrategy.getChinaTime();

        }*/
        init();
        setAmountModel(systemModel);

        //当前HT余额以及冻结

        return systemModel;


    }

    private void setAmountModel (SystemModel systemModel){
        accountAmountList.forEach(domain->{
            String currency= domain.getString("currency");
            String type = domain.getString("type");
            BigDecimal amount = domain.getBigDecimal("balance");
            amount.setScale(18);

            if("usdt".equals(currency) && "trade".equals(type)){
                systemModel.setUsdtNow(amount);

            }else if("usdt".equals(currency) && "frozen".equals(type)){
                systemModel.setUsdtFrozen(amount);

            }else if("ht".equals(currency)&& "trade".equals(type)){
                systemModel.setHtNow(amount);

            }else if("ht".equals(currency)&&"frozen".equals(type)){
                systemModel.setHtFrozen(amount);
            }



        });


    }

    private void init(){

        try {
            accountAmountList = apiNewClient.getAccountAmount(4267079);
        } catch (Exception e) {
            logger.error("初始化account内容失败:",e);
            initSuccess = false;
        }
    }
}
