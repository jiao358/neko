package com.estela.neko.core;

import com.estela.neko.Enutype.FloatEnu;
import com.estela.neko.Enutype.TradeModelType;
import com.estela.neko.api.NetTradeService;
import com.estela.neko.common.FundDomain;
import com.estela.neko.common.HttpHelper;
import com.estela.neko.domain.SystemModel;
import com.estela.neko.domain.TradeDimension;
import com.estela.neko.huobi.api.ApiNewClient;
import com.estela.neko.huobi.request.CreateOrderRequest;
import com.estela.neko.utils.CommonUtil;
import com.estela.neko.utils.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.util.MapUtils;
import org.thymeleaf.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author fuming.lj 2018/7/23 进行价格刷新的策略
 **/

public class PriceStrategy implements NetTradeService {
    private static final Logger logger = LoggerFactory.getLogger(PriceStrategy.class);
    private TradeDimension dimension ;

    ApiNewClient apiNewClient;

    HttpHelper httpHelper;

    CommonUtil commonUtil;

    private volatile String currentDate;

    public volatile  int priceNow;

    public volatile  BigDecimal cash ;
    BigDecimal rule =new BigDecimal("10");

    public void setApiNewClient(ApiNewClient apiNewClient,HttpHelper httpHelper,CommonUtil commonUtil){
        this.apiNewClient = apiNewClient;
        this.httpHelper = httpHelper;
        this.commonUtil = commonUtil;
    }

    public PriceStrategy(TradeDimension dimension){
        this.dimension= dimension;
    }

    /**
     * 缓存accountId
     */
    //private volatile long accountId = 4267079L;

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

    public  final int priceLimit = 300;

    public int logTime = 0;

    public int sellLogTime = 0;
    public volatile   int lastPrice= 0;

    private int priceBase=-1;

    Map<Integer, AtomicInteger> samePrice = new HashMap<>();

    public void addSellOrder(String price, String order) {
        sellOrder.put(Long.valueOf(price), Integer.valueOf(order));
        sell_order.add(Integer.parseInt(order));
        price_order.add(Integer.parseInt(order) - dimension.getStrategyStatus().getFluctuation());
    }

    public void addBuyOrder(String orderId, String price) {
        buyOrder.put(Long.valueOf(orderId), Integer.valueOf(price));
        price_order.add(Integer.parseInt(price));
    }

    public void deleteSellOrder(String order, String price) {
        sell_order.remove(price);
        price_order.remove(Integer.parseInt(price) - dimension.getStrategyStatus().getFluctuation());
        sellOrder.remove(Long.valueOf(order));
    }

    public void reflashOrder(String order,String price){
        reflashOrderFunction(Long.valueOf(order),Integer.valueOf(price));

    }

    /**
     *
     */
    public void startTradePrice() {

        SystemModel systemModel = commonUtil.generateCurrentModel(dimension.getTradeSemaphore());
        cash = systemModel.getUsdtNow();

        scheduleReflash.scheduleWithFixedDelay(() -> {
            try {
                if(dimension.isGhostFlag()){
                    logger.info(dimension.getTradeSemaphore()+" ghostFlag gogogo!");
                }

                BigDecimal currentPrice = httpHelper.getPrice(dimension.getApiKey(),dimension.getTradeSemaphore());
                if (currentPrice == null) {
                    return;
                }
                priceNow = currentPrice.intValue();


            } catch (Exception e) {
                logger.info("刷新价格策略失败");
            }

        }, 100, 35, TimeUnit.MILLISECONDS);

        tradingSchedule.scheduleWithFixedDelay(() -> {
            checkBuyMarket();
        }, 100, 40, TimeUnit.MILLISECONDS);

        int priceLimit = dimension.getStrategyStatus().getFluctuation()*3;
        sellScheduleOrder.scheduleAtFixedRate(() -> {
                sellLogTime++;

                sellOrder.forEach((orderId, price) -> {
                    if (priceNow + priceLimit < price) {

                        return;
                    }
                    reflashOrderFunction(orderId,price);



                });
                if (sellLogTime > 80) {
                    sellLogTime = 0;
                }

            }
            , 1000, 100, TimeUnit.MILLISECONDS);

        buyScheduleOrder.scheduleWithFixedDelay(() -> {

                buyOrder.forEach((orderId, price) -> {
                    try {
                        if (!dimension.getDiamond().canRunning) {
                            return;
                        }

                        Map<String, String> orderDetail = apiNewClient.getOrderInfoMap(String.valueOf(orderId),dimension);
                        if (MapUtils.isEmpty(orderDetail)) {
                            logger.info("买入订单获取异常:" + orderId);
                            return;
                        }

                        String state = orderDetail.get("state");
                        logger.info("确认购买订单号:" + orderId + "订单价格:" + price + "state:" + state);
                        if ("filled".equals(state)) {
                            logger.info("多单订单号:" + orderId + ",data=" + orderDetail.get("data"));
                            String filledAmount = orderDetail
                                .get("field-amount");
                            if (Double.parseDouble(filledAmount) < 0.1) {
                                filledAmount = "0.1";
                            }

                            BigDecimal bg = new BigDecimal(filledAmount).setScale(2, RoundingMode.DOWN);
                            filledAmount = bg.toString();
                            buyOrder.remove(orderId);
                            //   FundDomain buyerFundDomain = new FundDomain(orderId,orderDetail.get
                            // ("field-cash-amount"), orderDetail.get("field-fees"));
                            sell(price + dimension.getStrategyStatus().getFluctuation(), filledAmount, null);
                        } else if ("pre-submitted".equals(state)) {
                            logger.info("订单执行时发生失败,不进行进一步重试,剔除出买入订单");
                            buyOrder.remove(orderId);
                            price_order.remove(price);
                        }
                    } catch (Exception e) {
                        logger.error("清除buyOrder 异常 订单:" + orderId, e);
                    }

                });

            }
            , 1000, 500, TimeUnit.MILLISECONDS);

    }



    private void reflashOrderFunction(Long orderId, Integer price){
        try {


            long beginTime = System.currentTimeMillis();

            Map<String, String> orderDetail = apiNewClient.getOrderInfoMap(String.valueOf(orderId),dimension);
            long castTime = System.currentTimeMillis() - beginTime;
            if (sellLogTime > 80) {
                logger.info("SellOrder 查询订单信息需要:" + (castTime) + "ms  orderId:" + orderId + " price:" +
                    price);
            }
            if (MapUtils.isEmpty(orderDetail)) {
                logger.info("售出订单获取异常:" + orderId);
                return;
            }
            String state = orderDetail.get("state");
            if ("filled".equals(state)) {
                logger.info("空单，价格约" + price + "点，订单号:" + orderId + ",完全成交,data:" + orderDetail.get
                    ("data"));
                cash = cash.add(new BigDecimal(orderDetail.get
                    ("field-cash-amount")));
                dimension.getStrategyStatus().completeTrade();
                // final FundDomain sellFundomain = new FundDomain(orderId,orderDetail.get
                // ("field-cash-amount"), orderDetail.get("field-fees"));
                calTradeHandlers.execute(() -> {
                    try {
                        String time = getChinaTime();
                        if (currentDate.equals(time)) {
                            dimension.getStrategyStatus().todayCompleteTrade();
                        } else {
                            currentDate = time;
                            dimension.getStrategyStatus().todayCompleteTradeSetZero();
                            //report.cleanMap();;
                        }

                    } catch (Exception e) {
                        logger.error("统计交易信息详情失败:", e);
                    }

                });

                sell_order.remove(price);
                price_order.remove(price - dimension.getStrategyStatus().getFluctuation());
                sellOrder.remove(orderId);

                executor.execute(() -> {
                    buyMarket(price);
                });

            }

        } catch (Exception e) {
            logger.error("清除sellOrder 异常 订单:" + orderId, e);
        }

    }

    public void checkBuyMarket() {
        logTime++;

        int currentAppPrice = priceNow;
        //dimension.getStrategyStatus().getFluctuation();
        int step = 10;
        //当前price 为去除小数的价格
        int price = currentAppPrice / step * step;


        boolean access = canBuy(dimension.getTradeModelType(),priceNow);




        if (logTime > 120) {
            logger.warn(dimension.getTradeSemaphore()+" 当前价格:" + currentAppPrice + ",等比价格:" + price + "是否满足准入条件:" + access);
            logTime = 0;
            if(lastPrice!=currentAppPrice){
                lastPrice = currentAppPrice;
            }else{
                int occurTimes = 0;
                if(!dimension.getDiamond().HUOBILog){
                    if (samePrice.get(lastPrice) != null) {
                        occurTimes = samePrice.get(price).incrementAndGet();
                    } else {
                        samePrice.clear();
                        samePrice.put(price, new AtomicInteger(0));
                    }
                }
                if(occurTimes>80){
                    dimension.getDiamond().HUOBILog=true;
                }
            }

        }
        if (access&& price!=0) {
            if (!sell_order.contains(price + dimension.getStrategyStatus().getFluctuation()) && !price_order.contains(price)) {
                //logger.warn("满足准入条件:" + (!isOverHandLimit() && dimension.getDiamond().canRunning));

                if (!isOverHandLimit() && dimension.getDiamond().canRunning) {
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
        int sellSize = sell_order.size();
        if (olderSize >= dimension.getStrategyStatus().getMaxOrderSize() || sellSize >= dimension.getStrategyStatus().getMaxOrderSize()) {
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
        boolean flag = false;
        synchronized (lock) {
            flag = price_order.contains(price);
            if (flag) {
                logger.info("已经存在该价格,synchonized 同步模块作用显现: price:" + price);
                return;
            }
            // lastBuyPrice = price;
            // cash -= lastBuyPrice * strategyStatus.getLotSize();

            logger.info("加入购买清单:" + price);
            price_order.add(price);
        }

        try {

            // create order:
            long beginTime = System.currentTimeMillis();
            CreateOrderRequest createOrderReq = new CreateOrderRequest();
            createOrderReq.accountId = String.valueOf(dimension.getAccountId());
            createOrderReq.amount = Double.toString(dimension.getStrategyStatus().getLotSize() * (double)price / fluPlace());

            createOrderReq.symbol = dimension.getTradeSemaphore();//"htusdt";
            createOrderReq.type = CreateOrderRequest.OrderType.BUY_MARKET;
            createOrderReq.source = "api";
            // -------------------------------------------------------

            long orderId = apiNewClient.createOrder(createOrderReq,dimension);
            logger.info("创建buyer订单话费时间:" + (System.currentTimeMillis() - beginTime) + " ms");
            String state = "";
            boolean canTrade = true;

            canTrade = cash.compareTo(new BigDecimal(createOrderReq.amount)) > 0;

            if (!canTrade) {
                logger.info("钱不够,请等待 cash:" + cash);
                price_order.remove(price);
                return;
            }

            if (isSatisfyTrading(priceNow, price) && !isOverRishPriceOrLowPrice(
                price)) {
                beginTime = System.currentTimeMillis();
                apiNewClient.executeOrder(orderId,dimension);
                logger.info("执行订单花费时间:" + (System.currentTimeMillis() - beginTime) + " ms");
                cash = cash.subtract(new BigDecimal(createOrderReq.amount));
            } else {
                orderId = 0L;
            }

            if (orderId == 0L) {
                logger.info("价格变动过快 期待价格:" + price + " 实际当前市价:" + priceNow);
                price_order.remove(price);
                return;
            }

            buyOrder.put(orderId, price);

        } catch (Exception e) {
            logger.error("buymarket异常 购买失败", e);
        }

    }

    private double fluPlace(){
        int place = dimension.getLittlePrice();
        double cashPlace=10000.0;
        if(place!=-1){
            cashPlace=   rule.pow(5-place).doubleValue();
        }else{
            cashPlace = rule.pow(0).doubleValue();
        }
        return cashPlace;
    }

    //todo 需要重试机制
    public void sell(int priceStep, String fillAmount, FundDomain buyerFundDomain) {

        try {
            if (sell_order.contains(priceStep)) {
                logger.info("已经含有这个单子价格:" + priceStep);
                return;
            }

            logger.info("进入售卖流程: 价格:" + priceStep + " amount:" + fillAmount);



            CreateOrderRequest createOrderReq = new CreateOrderRequest();
            createOrderReq.accountId = String.valueOf(dimension.getAccountId());
            createOrderReq.amount = fillAmount;
            createOrderReq.price = Double.valueOf(((double)priceStep-2 )/ fluPlace()).toString();
            createOrderReq.symbol = dimension.getTradeSemaphore();//"htusdt";
            createOrderReq.type = CreateOrderRequest.OrderType.SELL_LIMIT;
            createOrderReq.source = "api";

            long orderId = apiNewClient.createOrder(createOrderReq,dimension);
            apiNewClient.executeOrder(orderId,dimension);
            sell_order.add(priceStep);
            //report.setFundReportUnit(buyerFundDomain,orderId);
            //增加每次新增空单 都需要有相应的日志变化
            LoggerUtil.loggerSellInfo(dimension.getTradeSemaphore(),sellOrder);
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
        if (StringUtils.isEmpty(dimension.getAccountId()) ) {
            throw new RuntimeException("accountId 为空");
        }
        startTradePrice();
    }

    /**
     * 初始化 accountId 信息
     */
    @Override
    public void init() {

        currentDate = getChinaTime();
    }

    public String getChinaTime() {
        Calendar cal = Calendar.getInstance();
        // 设置格式化的SimpleDateFormat对象，指定中国语言环境
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
        // 创建时区（TimeZone）对象，设置时区为“亚洲/重庆"
        TimeZone TZ = TimeZone.getTimeZone("Asia/Chongqing");
        // 将SimpleDateFormat强制转换为DateFormat
        DateFormat df = null;
        try {
            df = (DateFormat)sdf;
        } catch (Exception E) {
            E.printStackTrace();
        }
        // 为DateFormat对象设置时区
        df.setTimeZone(TZ);
        // 获取时间表达式
        String cdate = df.format(cal.getTime());

        return cdate;

    }

    /**
     * 判断当前的价格是否超过预定阈值 或者低于预定阈值
     * 新版 直接停止掉交易
     */
    private  boolean isOverRishPriceOrLowPrice(int priceNow){
        if(priceNow<dimension.getStrategyStatus().getHighriskPrice()&& priceNow>dimension.getStrategyStatus().getLowRisiPrice()){
            return false;
        }else {
            dimension.getDiamond().canRunning=false;
            return true;
        }
    }


    /**
     * 判断当前价格是否 满足交易策略
     *
     * @param currentPrice  当前价格
     * @return
     */
    public  boolean isSatisfyTrading (int currentPrice, int zhengdianPrice) {
        int result = currentPrice-zhengdianPrice;
        int diff = dimension.getStrategyStatus().getDiffPrice();
        if(currentPrice==0){
            return false;
        }

        if(dimension.getDiamond().floatStrategy.equals(FloatEnu.ALL_FLOAT)){
            if(result<=0 && result>= (-1*diff)){
                return true;
            }
            if(result>=0 && result<=diff){
                return true;
            }
        }

        if(dimension.getDiamond().floatStrategy.equals(FloatEnu.DOWN_FLOAT)){
            if(result<=0 && result>= (-1*diff)){
                return true;
            }
        }
        if(dimension.getDiamond().floatStrategy.equals(FloatEnu.UP_FLOAT)){

            if(result>=0 && result<=diff){
                return true;
            }
        }



        return false;
    }


    public  boolean canBuy(TradeModelType type, int priceNow) {
        boolean access = false;
        //100的基数
        if (type.equals(TradeModelType.QUA_MODEL100)) {
            int currentAppPrice = priceNow;
            int step = 100;
            int price = currentAppPrice / step * step;
            access = currentAppPrice == price;
        }else if(type.equals(TradeModelType.QUA_MODEL)){
            //150基数
            int currentAppPrice = priceNow;
            //dimension.getStrategyStatus().getFluctuation();
            int step = 10;
            //当前price 为去除小数的价格
            int price = currentAppPrice / step * step;
            int flu =dimension.getStrategyStatus().getFluctuation();
            access = (currentAppPrice == price ) &&(price%50==0) ;
            if(sell_order.isEmpty()&& priceBase==-1){
                //第一种情况 无售卖订单,重新开始系统启动
                if(access){
                    priceBase=currentAppPrice%flu;
                }

            }else if(!sell_order.isEmpty() && priceBase==-1){
                //有留存空单 重新开始启动系统
                for(Integer ode:sell_order){
                    priceBase = ode%flu;
                    break;
                }
            }
             access = (currentAppPrice%flu)==priceBase&&access;
        }else if(type.equals(TradeModelType.QUA_MODEL200)){
            int currentAppPrice = priceNow;
            int step = 100;
            int price = currentAppPrice / step * step;
            access = currentAppPrice == price;
            if(access){
                //为100的整数, 那么第二步判别 是否为200的运动幅度
                if(!sell_order.isEmpty()){
                    for(int sellPrice:sell_order){
                        access = (sellPrice-currentAppPrice)%200==0;
                        break;
                    }
                }

            }

        }else if(type.equals(TradeModelType.QUA_MODEL300)){
            int currentAppPrice = priceNow;
            int step = 100;
            int price = currentAppPrice / step * step;
            access = currentAppPrice == price;
            if(access){
                //为100的整数, 那么第二步判别 是否为300的运动幅度
                if(!sell_order.isEmpty()){
                    for(int sellPrice:sell_order){
                        access = (sellPrice-currentAppPrice)%300==0;
                        break;
                    }
                }

            }
        }

        return access;

    }
}
