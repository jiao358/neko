package com.estela.neko.core;

import com.estela.neko.api.NetTradeService;
import com.estela.neko.common.AccountModel;
import com.estela.neko.common.HttpHelper;
import com.estela.neko.common.StrategyStatus;
import com.estela.neko.config.Diamond;
import com.estela.neko.huobi.api.ApiClient;
import com.estela.neko.huobi.request.CreateOrderRequest;
import com.estela.neko.huobi.response.Accounts;
import com.estela.neko.huobi.response.AccountsResponse;
import com.estela.neko.huobi.response.OrdersDetailResponse;
import com.estela.neko.utils.PriceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author fuming.lj 2018/7/23 进行价格刷新的策略
 **/
@Service
public class PriceStrategy implements NetTradeService{
    private static final Logger logger = LoggerFactory.getLogger(PriceStrategy.class);
    @Autowired
    PriceMemery priceMemery;
    @Autowired
    AccountModel accountModel;
    @Autowired
    ApiClient apiClient;
    @Autowired
    StrategyStatus strategyStatus;
    @Autowired
    HttpHelper httpHelper;
    @Autowired
    PriceUtil priceUtil;
    /**
     * 缓存accountId
     */
    private volatile long accountId=0L;

    private ScheduledExecutorService scheduleReflash = new ScheduledThreadPoolExecutor(1);

    private ScheduledExecutorService tradingSchedule = new ScheduledThreadPoolExecutor(1);

    private ScheduledExecutorService sellScheduleOrder = new ScheduledThreadPoolExecutor(1);
    private ScheduledExecutorService buyScheduleOrder = new ScheduledThreadPoolExecutor(1);

    private Executor executor = Executors.newFixedThreadPool(1);

    public Set<Integer> price_order = Collections.synchronizedSet(new HashSet());
    public Set<Integer> sell_order = Collections.synchronizedSet(new HashSet());


    //存储 order  和售卖价格  当确定售卖后 清除 price_order 以及sell_order
    public ConcurrentHashMap<Long,Integer> sellOrder = new ConcurrentHashMap();
    public ConcurrentHashMap<Long,Integer> buyOrder = new ConcurrentHashMap();

    public Object lock = new Object();

    private int startPrice;
    private int lastBuyPrice;
    private int lastSellPrice;


    public static int cash = 0 * 10000;



    /**
     * 老版本
     */
    public void startTradePrice(){
        scheduleReflash.scheduleWithFixedDelay(() -> {
            try{
                logger.info("开始执行价格刷新策略");
                BigDecimal currentPrice = httpHelper.getPrice(accountModel.getApiKey());
                if (currentPrice == null) {
                    logger.error("刷新价格失败");
                    return ;
                }
                PriceMemery.priceNow = currentPrice.intValue();
            }catch (Exception e){
                logger.error("刷新价格策略失败");
            }

        },100, 100, TimeUnit.MILLISECONDS);


        tradingSchedule.scheduleWithFixedDelay(()->{
            logger.info("进行买入执行");
            checkBuyMarket();
        },100,100,TimeUnit.MILLISECONDS);

        sellScheduleOrder.scheduleAtFixedRate(()->{
                logger.info("开始确认 sellOrder 是否成交信息");

                sellOrder.forEach((orderId,price)->{
                    try{
                        OrdersDetailResponse ordersDetail = apiClient
                            .ordersDetail(String.valueOf(orderId));
                        logger.info("确认清除订单号:"+orderId+"订单价格:"+price);
                        String state = (String)((Map)(ordersDetail.getData())).get("state");
                        if ("filled".equals(state)) {
                            logger.info("空单，价格约" + price + "点，订单号:" + orderId + ",完全成交");
                                strategyStatus.completeTrade();

                                sell_order.remove(price);
                                price_order.remove(price-100);
                                sellOrder.remove(orderId);
                            executor.execute(()->{buyMarket(price-100);});

                        }

                    }catch (Exception e){
                        logger.error("清除sellOrder 异常 订单:"+orderId,e);
                    }

                });

            }
            ,1000,50,TimeUnit.MILLISECONDS);


        buyScheduleOrder.scheduleAtFixedRate(()->{
                logger.info("开始确认 buyOrder 是否全部成交信息");

                buyOrder.forEach((orderId,price)->{
                    try{
                        OrdersDetailResponse ordersDetail = apiClient
                            .ordersDetail(String.valueOf(orderId));
                        logger.info("确认购买订单号:"+orderId+"订单价格:"+price);
                        String state = (String)((Map)(ordersDetail.getData())).get("state");
                        if ("filled".equals(state)) {
                            logger.info("多单，价格约" + price + "点，订单号:" + orderId + ",完全成交");
                            String filledAmount = (String) ((Map) (ordersDetail.getData()))
                                .get("field-amount");
                            if(Double.parseDouble(filledAmount)<0.1){
                                filledAmount="0.1";
                            }

                            BigDecimal bg = new BigDecimal(filledAmount).setScale(2, RoundingMode.DOWN);
                            filledAmount = bg.toString();
                                buyOrder.remove(orderId);
                            sell(price+strategyStatus.getFluctuation(),filledAmount);
                        }

                    }catch (Exception e){
                        logger.error("清除sellOrder 异常 订单:"+orderId,e);
                    }

                });

            }
            ,1000,400,TimeUnit.MILLISECONDS);

    }

    public  void checkBuyMarket() {
        int currentAppPrice = PriceMemery.priceNow;
        int step =strategyStatus.getFluctuation();
        int price = currentAppPrice / step * step;
        boolean access =currentAppPrice==price;
        logger.info("当前价格:"+currentAppPrice+",等比价格:"+price+"是否满足准入条件:"+access);
        if (access && price!=0) {
            if (!sell_order.contains(price + step) && !price_order.contains(price)) {
                logger.info("满足准入条件");

                if(!isOverHandLimit() && Diamond.canRunning){
                    buyMarket(price);
                }

            }
        }

    }
    /**
     * 对持有单数量的控制
     */

    public boolean isOverHandLimit(){
        int olderSize = price_order.size();
        if(olderSize>=strategyStatus.getMaxOrderSize()){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 此处的price 为35000的 price
     * @param price
     */
    public  void buyMarket(int price) {
        logger.info("进入buyMarket 购买价格:" + price);
        try {
            boolean flag =false;
            synchronized (lock){
                flag =!price_order.contains(price);
            }
            if (flag) {

                    lastBuyPrice = price;
                    cash -= lastBuyPrice * strategyStatus.getLotSize();

                    logger.warn("加入购买清单:"+price);
                    price_order.add(price);


                    // create order:
                    CreateOrderRequest createOrderReq = new CreateOrderRequest();
                    createOrderReq.accountId = String.valueOf(accountId);
                    createOrderReq.amount =Double.toString(strategyStatus.getLotSize() * (double) price / 10000);

                    createOrderReq.symbol = "htusdt";
                    createOrderReq.type = CreateOrderRequest.OrderType.BUY_MARKET;
                    createOrderReq.source = "api";
                    // -------------------------------------------------------
                    long orderId =apiClient.createOrder(createOrderReq);
                    String state ="";

                    if(priceUtil.isSatisfyTrading(PriceMemery.priceNow,price) && !priceUtil.isOverRishPriceOrLowPrice(price)){
                        String r = apiClient.placeOrder(orderId);
                    }else{
                        orderId=0L;
                    }

                    if(orderId==0L){
                        logger.warn("价格变动过快 期待价格:"+price+" 实际当前市价:"+PriceMemery.priceNow);
                        price_order.remove(price);
                        return;
                    }

                    // place order:

                    // ------------------------------------------------------ 执行订单
                    // -------------------------------------------------------
                    OrdersDetailResponse ordersDetail = apiClient
                        .ordersDetail(String.valueOf(orderId));
                    state = (String) ((Map) (ordersDetail.getData())).get("state");

                    logger.warn("购买价格返回值:"+ orderId+" 此订单状态"+state);
                    if ("filled".equals(state)) {
                        String filledAmount = (String) ((Map) (ordersDetail.getData()))
                            .get("field-amount");
                        if(Double.parseDouble(filledAmount)<0.1){
                            filledAmount="0.1";
                        }

                        BigDecimal bg = new BigDecimal(filledAmount).setScale(2, RoundingMode.DOWN);
                        filledAmount = bg.toString();

                        sell(price+strategyStatus.getFluctuation(),filledAmount);

                    }else if("submitted".equals(state) || "partial-filled".equals(state) ) {
                        logger.info("购买价格:"+price+"订单执行中");
                            buyOrder.put(orderId,price);
                    }else{
                        logger.error("购买价格:"+price+"订单执行中");
                            price_order.remove(price);
                    }




            }

        } catch (Exception e) {
            logger.error("buymarket异常 购买失败",e);
        }



    }

    public  void sell(int priceStep,String fillAmount) {



            try{
                if(sell_order.contains(priceStep)){
                    logger.warn("已经含有这个单子价格:"+priceStep);
                    return;
                }

                logger.warn("进入售卖流程: 价格:"+priceStep+" amount:"+ fillAmount);

                CreateOrderRequest createOrderReq = new CreateOrderRequest();
                createOrderReq.accountId = String.valueOf(accountId);
                createOrderReq.amount = fillAmount;
                createOrderReq.price = Double.valueOf((double)priceStep/10000.0).toString();
                createOrderReq.symbol = "htusdt";
                createOrderReq.type = CreateOrderRequest.OrderType.SELL_LIMIT;
                createOrderReq.source = "api";

                long orderId = apiClient.createOrder(createOrderReq);
                String r = apiClient.placeOrder(orderId);

                    sell_order.add(priceStep);
                    sellOrder.put(orderId,priceStep);

            }catch (Exception e){
                logger.error("售卖流程异常 price="+priceStep+"手数:"+fillAmount,e);
            }








    }



    /**
     * 执行策略
     */
    @Override
    public void execute() {

        init();
        /**
         * check accountId
         */
        if(accountId==0){
            throw new RuntimeException("accountId 为空");
        }
        startTradePrice();
    }

    /**
     * 初始化 accountId 信息
     */
    @Override
    public void init() {
        AccountsResponse<List<Accounts>> accounts = apiClient.accounts();
        Accounts account = accounts.getData().get(0);
        accountId =account.getId();
    }


}
