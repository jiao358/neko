package com.estela.neko.utils;

import com.alibaba.fastjson.JSONObject;
import com.estela.neko.common.FundReport;
import com.estela.neko.core.TradeModelFactory;
import com.estela.neko.domain.SystemModel;
import com.estela.neko.domain.TradeDimension;
import com.estela.neko.huobi.api.ApiNewClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author fuming.lj 2018/7/31
 **/
@Service
public class CommonUtil {
    private  static Logger logger = LoggerFactory.getLogger(CommonUtil.class);





    @Autowired
    FundReport fundReport;
    @Autowired
    TradeModelFactory factory;
    @Autowired
    ApiNewClient apiNewClient;

    volatile String time ;

    boolean initSuccess =true;

    List<JSONObject> accountAmountList = new ArrayList<>();


    public SystemModel generateCurrentModel(String symbol){
        TradeDimension dimension= factory.getDimension(symbol);

        if(time==null || !initSuccess){
            time = dimension.getPriceStrategy().getChinaTime();
            //init(dimension);
        }


        SystemModel systemModel = new SystemModel();
        systemModel.setDiffPrice(dimension.getStrategyStatus().getDiffPrice());
        systemModel.setFluctuation(dimension.getStrategyStatus().getFluctuation());
        systemModel.setHighriskPrice(dimension.getStrategyStatus().getHighriskPrice());
        systemModel.setLotSize(dimension.getStrategyStatus().getLotSize());
        systemModel.setLowRisiPrice(dimension.getStrategyStatus().getLowRisiPrice());
        systemModel.setMaxOrderSize(dimension.getStrategyStatus().getMaxOrderSize());
        systemModel.setCurrentPrice(dimension.getPriceStrategy().priceNow);
        systemModel.setFloatStrategy(dimension.getDiamond().floatStrategy.getValue());
        systemModel.setRunning(dimension.getDiamond().canRunning?"运行中":"暂停中");
        List<Integer> buyOrderList = dimension.getPriceStrategy().price_order.stream().collect(Collectors.toList());
        List<Integer> sellOrderList = dimension.getPriceStrategy().sell_order.stream().collect(Collectors.toList());
        systemModel.setSymbol(symbol);
        Collections.sort(buyOrderList);
        Collections.sort(sellOrderList);
        systemModel.setBuyAndOrder(JSONObject.toJSONString(dimension.getPriceStrategy().buyOrder));
        systemModel.setSellAndOrder(JSONObject.toJSONString(dimension.getPriceStrategy().sellOrder));

        systemModel.setTodayTrade(dimension.getStrategyStatus().getTodayCompleteTrade().get());
        systemModel.setBuyOrder(buyOrderList);
        systemModel.setSellOrder(sellOrderList);
        systemModel.setAlreadyDual(dimension.getStrategyStatus().getCompleteTrade());
        systemModel.setGhost(dimension.getDiamond().HUOBILog);
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
        //init(dimension);
        setAmountModel(systemModel,dimension);

        //当前HT余额以及冻结

        return systemModel;


    }

    private void setAmountModel (SystemModel systemModel,TradeDimension dimension){
        accountAmountList.forEach(domain->{
            String currency= domain.getString("currency");
            String type = domain.getString("type");
            BigDecimal amount = domain.getBigDecimal("balance");
            amount.setScale(18);

            if("usdt".equals(currency) && "trade".equals(type)){
                systemModel.setUsdtNow(amount);

            }else if("usdt".equals(currency) && "frozen".equals(type)){
                systemModel.setUsdtFrozen(amount);

            }else if(dimension.getCurrency().equals(currency)&& "trade".equals(type)){
                systemModel.setHtNow(amount);

            }else if(dimension.getCurrency().equals(currency)&&"frozen".equals(type)){
                systemModel.setHtFrozen(amount);
            }



        });


    }
    //获取账户金额信息   todo  增加获取账户信息的内容
    private void init(TradeDimension dimension){
//4267079
        /**
         * 获取账户信息
         * AccountsResponse<List<Accounts>> accounts = apiClient.accounts();
         Accounts account = accounts.getData().get(0);
         dimension.setAccountId(account.getId()+"");
         **/

        try {
            if(StringUtils.isEmpty(dimension.getAccountId())){
                  apiNewClient.getAccounts(dimension);
            }
            accountAmountList = apiNewClient.getAccountAmount(Integer.parseInt(dimension.getAccountId()),dimension.getCurrency(),dimension);
        } catch (Exception e) {
            logger.error("初始化account内容失败:",e);
            initSuccess = false;
        }
    }
}
