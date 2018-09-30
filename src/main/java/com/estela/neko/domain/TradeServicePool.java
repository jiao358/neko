package com.estela.neko.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author fuming.lj 2018/9/29
 * 交易中涉及的交易变量
 **/
public class TradeServicePool {
    private volatile String currentDate;
    /**
     * 缓存accountId
     */
    private volatile long accountId = 4267079L;

    private ScheduledExecutorService scheduleReflash = new ScheduledThreadPoolExecutor(1);

    private ScheduledExecutorService tradingSchedule = new ScheduledThreadPoolExecutor(1);

    private ScheduledExecutorService sellScheduleOrder = new ScheduledThreadPoolExecutor(1);
    private ScheduledExecutorService buyScheduleOrder = new ScheduledThreadPoolExecutor(1);

    private Executor executor = Executors.newFixedThreadPool(1);
    private Executor calTradeHandlers = Executors.newFixedThreadPool(1);


    public Set<Integer> price_order = Collections.synchronizedSet(new HashSet());
    public Set<Integer> sell_order = Collections.synchronizedSet(new HashSet());

    //存储 order  和售卖价格  当确定售卖后 清除 price_order 以及sell_order
    public ConcurrentHashMap<Long, Integer> sellOrder = new ConcurrentHashMap();
    public ConcurrentHashMap<Long, Integer> buyOrder = new ConcurrentHashMap();

    public Object lock = new Object();

    private int startPrice;
    private int lastBuyPrice;
    private int lastSellPrice;

    public static int cash = 0 * 10000;
    public static final int priceLimit = 300;

    public int logTime = 0;

    public int sellLogTime = 0;
    public volatile static  int lastPrice= 0;



}
