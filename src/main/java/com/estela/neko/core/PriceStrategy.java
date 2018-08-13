package com.estela.neko.core;

import com.estela.neko.api.NetTradeService;
import com.estela.neko.common.*;
import com.estela.neko.config.Diamond;
import com.estela.neko.huobi.api.ApiClient;
import com.estela.neko.huobi.api.ApiNewClient;
import com.estela.neko.huobi.request.CreateOrderRequest;
import com.estela.neko.huobi.response.Accounts;
import com.estela.neko.huobi.response.AccountsResponse;
import com.estela.neko.huobi.response.OrdersDetailResponse;
import com.estela.neko.utils.PriceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.expression.Maps;
import org.thymeleaf.util.MapUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author fuming.lj 2018/7/23 进行价格刷新的策略
 **/
@Service
public class PriceStrategy implements NetTradeService {
    private static final Logger logger = LoggerFactory.getLogger(PriceStrategy.class);
    @Autowired
    PriceMemery priceMemery;
    @Autowired
    AccountModel accountModel;
    @Autowired
    ApiNewClient apiNewClient;
    @Autowired
    StrategyStatus strategyStatus;
    @Autowired
    HttpHelper httpHelper;
    @Autowired
    PriceUtil priceUtil;
    @Autowired
    ApiClient apiClient;


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
    private Executor reportHandler = Executors.newFixedThreadPool(1);

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


    public void addSellOrder(String price,String order){
        sellOrder.put(Long.valueOf(price),Integer.valueOf(order));
        sell_order.add(Integer.parseInt(order));
        price_order.add(Integer.parseInt(order)-100);
    }


    public void addBuyOrder(String orderId,String price){
        buyOrder.put(Long.valueOf(orderId),Integer.valueOf(price));
        price_order.add(Integer.parseInt(price));
    }

    /**
     * 老版本
     */
    public void startTradePrice() {

        scheduleReflash.scheduleWithFixedDelay(() -> {
            try {
                logger.info("开始执行价格刷新策略");
                BigDecimal currentPrice = httpHelper.getPrice(accountModel.getApiKey());
                if (currentPrice == null) {
                    logger.error("刷新价格失败");
                    return;
                }
                PriceMemery.priceNow = currentPrice.intValue();
            } catch (Exception e) {
                logger.error("刷新价格策略失败");
            }

        }, 100, 35, TimeUnit.MILLISECONDS);

        tradingSchedule.scheduleWithFixedDelay(() -> {
            logger.info("进行买入执行");
            checkBuyMarket();
        }, 100, 70, TimeUnit.MILLISECONDS);

        sellScheduleOrder.scheduleAtFixedRate(() -> {
                logger.info("开始确认 sellOrder 是否成交信息");

                sellOrder.forEach((orderId, price) -> {
                    try {

                        Map<String,String> orderDetail =apiNewClient.getOrderInfoMap(String.valueOf(orderId));
                        if(MapUtils.isEmpty(orderDetail)){
                            logger.error("售出订单获取异常:"+orderId);
                            return;
                        }
                        logger.info("确认清除订单号:" + orderId + "订单价格:" + price);
                        String state = orderDetail.get("state");
                        if ("filled".equals(state)) {
                            logger.error("空单，价格约" + price + "点，订单号:" + orderId + ",完全成交,data:"+orderDetail.get("data"));
                            strategyStatus.completeTrade();
                           // final FundDomain sellFundomain = new FundDomain(orderId,orderDetail.get("field-cash-amount"), orderDetail.get("field-fees"));
                            calTradeHandlers.execute(()->{
                                try{
                                    String time = getChinaTime();
                                    if(currentDate.equals(time)){
                                        strategyStatus.todayCompleteTrade();
                                    }else{
                                        currentDate= time;
                                        strategyStatus.todayCompleteTradeSetZero();
                                        //report.cleanMap();;
                                    }
                                }catch (Exception e){
                                    logger.error("统计交易信息详情失败:",e);
                                }

                            });
                     /*       reportHandler.execute(()->{
                                try{
                                    report.calculate(sellFundomain);
                                }catch (Exception e){}
                            });*/
                            sell_order.remove(price);
                            price_order.remove(price - 100);
                            sellOrder.remove(orderId);

                            executor.execute(() -> {
                                buyMarket(price);
                            });

                        }

                    } catch (Exception e) {
                        logger.error("清除sellOrder 异常 订单:" + orderId, e);
                    }

                });

            }
            , 1000, 50, TimeUnit.MILLISECONDS);

        buyScheduleOrder.scheduleAtFixedRate(() -> {
                logger.info("开始确认 buyOrder 是否全部成交信息");

                buyOrder.forEach((orderId, price) -> {
                    try {
                        Map<String,String> orderDetail =apiNewClient.getOrderInfoMap(String.valueOf(orderId));
                        if(MapUtils.isEmpty(orderDetail)){
                            logger.error("买入订单获取异常:"+orderId);
                            return;
                        }
                        logger.info("确认购买订单号:" + orderId + "订单价格:" + price);
                        String state = orderDetail.get("state");
                        if ("filled".equals(state)) {
                            logger.warn("多单订单号:" + orderId +",data="+orderDetail.get("data"));
                            String filledAmount = orderDetail
                                .get("field-amount");
                            if (Double.parseDouble(filledAmount) < 0.1) {
                                filledAmount = "0.1";
                            }

                            BigDecimal bg = new BigDecimal(filledAmount).setScale(2, RoundingMode.DOWN);
                            filledAmount = bg.toString();
                            buyOrder.remove(orderId);
                         //   FundDomain buyerFundDomain = new FundDomain(orderId,orderDetail.get("field-cash-amount"), orderDetail.get("field-fees"));
                            sell(price + strategyStatus.getFluctuation(), filledAmount,null);
                        }

                    } catch (Exception e) {
                        logger.error("清除buyOrder 异常 订单:" + orderId, e);
                    }

                });

            }
            , 1000, 100, TimeUnit.MILLISECONDS);

    }

    public void checkBuyMarket() {
        int currentAppPrice = PriceMemery.priceNow;
        int step = strategyStatus.getFluctuation();
        int price = currentAppPrice / step * step;
        boolean access = currentAppPrice == price;
        logger.info("当前价格:" + currentAppPrice + ",等比价格:" + price + "是否满足准入条件:" + access);
        if (access && price != 0) {
            if (!sell_order.contains(price + step) && !price_order.contains(price)) {
                logger.info("满足准入条件");

                if (!isOverHandLimit() && Diamond.canRunning) {
                    buyMarket(price);
                }

            }
        }

    }

    /**
     * 对持有单数量的控制
     */

    public boolean isOverHandLimit() {
        int olderSize = price_order.size();
        if (olderSize >= strategyStatus.getMaxOrderSize()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 此处的price 为35000的 price
     *
     * @param price
     */
    public void buyMarket(int price) {
        logger.info("进入buyMarket 购买价格:" + price);
        boolean flag = false;
        synchronized (lock) {
            flag = price_order.contains(price);
            if (flag) {
                logger.warn("已经存在该价格,synchonized 同步模块作用显现: price:" + price);
                return;
            }
            lastBuyPrice = price;
            cash -= lastBuyPrice * strategyStatus.getLotSize();

            logger.warn("加入购买清单:" + price);
            price_order.add(price);
        }

        try {


            // create order:
            CreateOrderRequest createOrderReq = new CreateOrderRequest();
            createOrderReq.accountId = String.valueOf(accountId);
            createOrderReq.amount = Double.toString(strategyStatus.getLotSize() * (double)price / 10000);

            createOrderReq.symbol = "htusdt";
            createOrderReq.type = CreateOrderRequest.OrderType.BUY_MARKET;
            createOrderReq.source = "api";
            // -------------------------------------------------------

            long orderId = apiNewClient.createOrder(createOrderReq);
            String state = "";

            if (priceUtil.isSatisfyTrading(PriceMemery.priceNow, price) && !priceUtil.isOverRishPriceOrLowPrice(
                price)) {
                apiNewClient.executeOrder(orderId);
            } else {
                orderId = 0L;
            }

            if (orderId == 0L) {
                logger.warn("价格变动过快 期待价格:" + price + " 实际当前市价:" + PriceMemery.priceNow);
                price_order.remove(price);
                return;
            }

            buyOrder.put(orderId, price);

        } catch (Exception e) {
            logger.error("buymarket异常 购买失败", e);
        }

    }
    //todo 需要重试机制
    public void sell(int priceStep, String fillAmount, FundDomain buyerFundDomain ) {

        try {
            if (sell_order.contains(priceStep)) {
                logger.warn("已经含有这个单子价格:" + priceStep);
                return;
            }

            logger.warn("进入售卖流程: 价格:" + priceStep + " amount:" + fillAmount);

            CreateOrderRequest createOrderReq = new CreateOrderRequest();
            createOrderReq.accountId = String.valueOf(accountId);
            createOrderReq.amount = fillAmount;
            createOrderReq.price = Double.valueOf((double)priceStep / 10000.0).toString();
            createOrderReq.symbol = "htusdt";
            createOrderReq.type = CreateOrderRequest.OrderType.SELL_LIMIT;
            createOrderReq.source = "api";

            long orderId = apiNewClient.createOrder(createOrderReq);
            apiNewClient.executeOrder(orderId);
            sell_order.add(priceStep);
            //report.setFundReportUnit(buyerFundDomain,orderId);
            sellOrder.put(orderId, priceStep);

        } catch (Exception e) {
            logger.error("售卖流程异常 price=" + priceStep + "手数:" + fillAmount, e);
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
        if (accountId == 0) {
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
        accountId = account.getId();
        currentDate=getChinaTime();
    }

    public String getChinaTime(){
        Calendar cal = Calendar.getInstance();
        // 设置格式化的SimpleDateFormat对象，指定中国语言环境
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
        // 创建时区（TimeZone）对象，设置时区为“亚洲/重庆"
        TimeZone TZ = TimeZone.getTimeZone("Asia/Chongqing");
        // 将SimpleDateFormat强制转换为DateFormat
        DateFormat df = null;
        try
        {
            df = (DateFormat)sdf;
        }
        catch(Exception E)
        {
            E.printStackTrace();
        }
        // 为DateFormat对象设置时区
        df.setTimeZone(TZ);
        // 获取时间表达式
        String cdate = df.format(cal.getTime());

        return cdate;

    }

}
