package com.estela.neko.core;

import com.estela.neko.common.StrategyStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Transient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author fuming.lj 2018/7/23
 * 用于存储价格 以及交易中的订单数据
 **/
@Service
public class PriceMemery {

    public static int priceNow;



    @Transient
    @Autowired
    StrategyStatus strategyStatus;
    /**
     * 该价格已经乘以了10000
     */
    private BigDecimal price ;

    private Long priceData;

    private volatile BigDecimal preOrderPrice;

    /**
     * 用于存放 整点的交易记录
     */
    private HashSet<Integer> orderList = new HashSet<>();


    /**
     * 获取当前价格
     * @return
     */
    public BigDecimal getCurrentPrice(){
        return price;
    }

    public synchronized void reflashPrice(BigDecimal newPrice){
        price = newPrice;
        priceData= System.currentTimeMillis();
    }

    /**
     * 没有任何交易记录
     * @return
     */
    public synchronized boolean noAnyTradeOrder(){
        return orderList.isEmpty();
    }

    /**
     * 还有相同的订单
     * @param price
     * @return
     */
    public synchronized boolean hasSameOrder(int price){
        return orderList.contains(price);
    }

    /**
     * 清除存在价格
     * @param price
     */
    public synchronized void cleanPrice(int price){
        if(orderList.contains(price)){
            orderList.remove(price);
        }
    }

    /**
     * 已经存在的订单数量
     */
    public synchronized int currentHandleOrder(){
        return orderList.size();
    }

    /**
     * 执行交易订单
     */
    public synchronized void addOrder(BigDecimal orderPrice){
        orderList.add(orderPrice.intValue());

        preOrderPrice = orderPrice;

    }

    /**
     * 执行卖单加入
     * @param sellOrderPrice
     */
    public synchronized void sellOrder(BigDecimal sellOrderPrice){
        orderList.add(sellOrderPrice.intValue());
    }

    public int getPreOrderPrice(){
        return preOrderPrice.intValue();
    }


    public String getOrderLists(){
        return orderList.toString();

    }
}
