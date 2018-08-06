package com.estela.neko.utils;

import com.alibaba.fastjson.JSONObject;
import com.estela.neko.common.StrategyStatus;
import com.estela.neko.config.Diamond;
import com.estela.neko.core.PriceMemery;
import com.estela.neko.core.PriceStrategy;
import com.estela.neko.domain.SystemModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author fuming.lj 2018/7/31
 **/
@Service
public class CommonUtil {
    @Autowired
    PriceStrategy priceStrategy;
    @Autowired
    Diamond diamond;
    @Autowired
    PriceMemery priceMemery;
    @Autowired
    StrategyStatus strategyStatus;


    public SystemModel generateCurrentModel(){
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
        return systemModel;


    }

}
