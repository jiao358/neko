package com.estela.neko.core;

import com.estela.neko.common.AccountModel;
import com.estela.neko.common.HttpHelper;
import com.estela.neko.common.StrategyStatus;
import com.estela.neko.huobi.api.ApiClient;
import com.estela.neko.huobi.request.CreateOrderRequest;
import com.estela.neko.huobi.response.Accounts;
import com.estela.neko.huobi.response.AccountsResponse;
import com.estela.neko.huobi.response.OrdersDetailResponse;
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
public class PriceStrategy {
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
    /**
     * 缓存accountId
     */
    private volatile long accountId;

    private ScheduledExecutorService scheduleReflash = new ScheduledThreadPoolExecutor(1);

    private ScheduledExecutorService tradingSchedule = new ScheduledThreadPoolExecutor(1);

    private ScheduledExecutorService sellScheduleOrder = new ScheduledThreadPoolExecutor(1);
    private ScheduledExecutorService buyScheduleOrder = new ScheduledThreadPoolExecutor(1);


    ExecutorService markPool = Executors.newFixedThreadPool(10);
    ExecutorService sellForLimit = Executors.newFixedThreadPool(10);

    private ExecutorService buyAccessPool = Executors.newFixedThreadPool(10);
    private ExecutorService sellAccessPool = Executors.newFixedThreadPool(10);

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
                                sell_order.remove(price);
                                price_order.remove(price-100);
                                sellOrder.remove(orderId);

                        }

                    }catch (Exception e){
                        logger.error("清除sellOrder 异常 订单:"+orderId,e);
                    }

                });

            }
            ,1000,400,TimeUnit.MILLISECONDS);


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
                            sell(price+step,filledAmount);
                        }

                    }catch (Exception e){
                        logger.error("清除sellOrder 异常 订单:"+orderId,e);
                    }

                });

            }
            ,1000,400,TimeUnit.MILLISECONDS);

    }
    public Set<Integer> price_order = Collections.synchronizedSet(new HashSet());
    public Set<Integer> sell_order = Collections.synchronizedSet(new HashSet());
    public Set<Integer> fullBuy_order = Collections.synchronizedSet(new HashSet());
    //存储 order  和售卖价格  当确定售卖后 清除 price_order 以及sell_order
    public ConcurrentHashMap<Long,Integer> sellOrder = new ConcurrentHashMap();
    public ConcurrentHashMap<Long,Integer> buyOrder = new ConcurrentHashMap();

    public Object lock = new Object();

    private int startPrice;
    private int lastBuyPrice;
    private int lastSellPrice;

    int step = 100;

    public static int cash = 0 * 10000;
    public static  double amount =10;
    public  void checkBuyMarket() {
        int currentAppPrice = PriceMemery.priceNow;
        int price = currentAppPrice / step * step;
        boolean access =currentAppPrice==price;
        logger.info("当前价格:"+currentAppPrice+",等比价格:"+price+"是否满足准入条件:"+access);
        if (access && price!=0) {
            if (!sell_order.contains(price + step) && !price_order.contains(price)) {
                logger.info("满足准入条件");
                buyMarket(price);
             /*   if(price_order.size()<strategyStatus.getMaxOrderSize()){

                }*/


            }
        }

    }
    public  void buyMarket(int price) {
        logger.info("进入buyMarket 购买价格:" + price);

            if (!price_order.contains(price)) {
                try {
                    lastBuyPrice = price;
                    cash -= lastBuyPrice * amount;

                    logger.warn("加入购买清单:"+price);
                    price_order.add(price);

                    AccountsResponse<List<Accounts>> accounts = apiClient.accounts();
                    Accounts account = accounts.getData().get(0);
                    long accountId = account.getId();
                    // create order:
                    CreateOrderRequest createOrderReq = new CreateOrderRequest();
                    createOrderReq.accountId = String.valueOf(accountId);
                    createOrderReq.amount =Double.toString(amount * (double) price / 10000);

                    createOrderReq.symbol = "htusdt";
                    createOrderReq.type = CreateOrderRequest.OrderType.BUY_MARKET;
                    createOrderReq.source = "api";
                    // -------------------------------------------------------
                    long orderId = 0L;
                    String state ="";
                    synchronized (lock){
                        if(isSatisfyTrading(PriceMemery.priceNow,price)){

                            orderId = apiClient.createOrder(createOrderReq);
                            String r = apiClient.placeOrder(orderId);
                        }
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

                        sell(price+step,filledAmount);

                    }else if("submitted".equals(state) || "partial-filled".equals(state) ) {
                        logger.info("购买价格:"+price+"订单执行中");
                        synchronized (lock){
                            buyOrder.put(orderId,price);
                        }
                    }else{
                        logger.error("购买价格:"+price+"订单执行中");
                        synchronized (lock){
                            price_order.remove(price);
                        }
                    }



                } catch (Exception e) {
                    logger.error("buymarket异常 购买失败",e);
                }
            }





    }

    public  void sell(int priceStep,String fillAmount) {



            try{
                if(sell_order.contains(priceStep)){
                    logger.warn("已经含有这个单子价格:"+priceStep);
                    return;
                }

                logger.warn("进入售卖流程: 价格:"+priceStep+" amount:"+ fillAmount);
                AccountsResponse<List<Accounts>> accounts = apiClient.accounts();
                Accounts account = accounts.getData().get(0);
                long accountId = account.getId();
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
     * 判断当前价格是否 满足交易策略
     *
     * @param currentPrice  当前价格
     * @return
     */
    public boolean isSatisfyTrading (int currentPrice, int zhengdianPrice) {
        double lot = amount;
        int result = currentPrice-zhengdianPrice;
        int diff = strategyStatus.getDiffPrice();
        if(currentPrice==0){
            return false;
        }
        if(result>=0 && result<=diff){
            return true;
        }
        if(result<0 && result<= (-1*diff)){
            return true;
        }





        return false;
    }




    /**
     * 如果刷新程序没有停止, 则以每500毫秒刷新一次最近的价格字段

     public void startReflashPrice() {
     if (!scheduleReflash.isShutdown()) {
     scheduleReflash.scheduleWithFixedDelay(() -> {
     logger.info("开始执行价格刷新策略");
     if (!accountModel.hasAccountKey()) {
     return;
     }

     BigDecimal currentPrice = httpHelper.getPrice(accountModel.getApiKey());
     if (currentPrice == null) {
     logger.error("刷新价格失败");
     }
     priceMemery.reflashPrice(currentPrice);
     markPool.execute(()->{
     logger.info("自动交易开始执行");
     autoMartket(priceMemery.getCurrentPrice());

     });
     }, 100, 500, TimeUnit.MILLISECONDS);
     }




     }
     */
    /**
     * 自动进行验证交易
     * @Param currentPrice  应该乘以10000的价格

    private void autoMartket( BigDecimal currentPrice){

    if (currentPrice == null) {
    logger.warn("获取不到当前价格,执行订单处理失败");
    return;
    }

    BigDecimal riskPrice = strategyStatus.getRiskPrice();
    if (riskPrice.compareTo(currentPrice) > 0) {
    logger.warn("当前价格超过风险设定阈值,停止买入交易 当前价格:" + currentPrice + ",阈值价格:" + riskPrice);
    return;
    }
    logger.info("自动行情交易,当前市价:"+ currentPrice.intValue()  + " 当前是否满足准入:"+( currentPrice.intValue()%strategyStatus.getFluctuation()==0));
    if (strategyStatus.getStartOrder() != null &&  strategyStatus.isSatisfyTrading(currentPrice.intValue(),strategyStatus.getStartOrder().intValue())) {
    logger.info("执行第一次StartOrder: 当前价格:"+currentPrice.intValue());
    //指定开始执行交易价格,如果没有这个交易价格则等待
    BigDecimal price = strategyStatus.getStartOrder();
    strategyStatus.setStartOrder(null);
    buyOrder(price);

    } else {
    //直接将当前价格作为基准价格
    if (priceMemery.noAnyTradeOrder() && currentPrice.intValue()%strategyStatus.getFluctuation()==0) {
    logger.info("以基准价格执行作为价格原型 (100) : 当前价格:"+currentPrice.intValue());
    buyOrder(currentPrice);
    } else {

    if (priceMemery.currentHandleOrder() >= strategyStatus.getMaxOrderSize()) {
    logger.warn("当前持有单数达到最大持单水位 停止多单交易 maxOrderSize:" + strategyStatus.getMaxOrderSize());
    return;
    }
    int lastPrice = priceMemery.getPreOrderPrice();
    //如果当前价格满足交易点数策略  并且满足同一位置只有一个多单  才能执行交易
    if(strategyStatus.isSatisfyTrading(currentPrice.intValue(),lastPrice) && !priceMemery.hasSameOrder(currentPrice.intValue())){

    if(!priceMemery.hasSameOrder(fillPrice(currentPrice.intValue(),lastPrice))){
    buyOrder(new BigDecimal(fillPrice(currentPrice.intValue(),lastPrice)));
    }
    }else{
    logger.info("当前价格不满足准入模型 价格:"+currentPrice.intValue() +" 上次交易价格:"+lastPrice +"当前持单价格:"+priceMemery.getOrderLists());
    }

    }

    }

    }  */


    /**
     * 补全满足策略的价格
     * @param currentPrice 当前价格
     * @return
     */
    public int fillPrice(int currentPrice,int lastPrice){
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
     * 购买订单
     *
     * @param price

    private void buyOrder(final BigDecimal price) {


    //添加最新购买价格
    priceMemery.addOrder(price);
    double lotSize = strategyStatus.getLotSize();

    AccountsResponse<List<Accounts>> accounts = apiClient.accounts();
    Accounts account = accounts.getData().get(0);
    long accountId = account.getId();
    // create order:
    CreateOrderRequest createOrderReq = new CreateOrderRequest();
    createOrderReq.accountId = String.valueOf(accountId);
    createOrderReq.amount = Double.toString(lotSize* ( price.divide(new BigDecimal(10000)).doubleValue()) );

    createOrderReq.symbol = "htusdt";
    createOrderReq.type = CreateOrderRequest.OrderType.BUY_MARKET;
    createOrderReq.source = "api";

    // ------------------------------------------------------ 创建订单
    // -------------------------------------------------------
    long orderId = apiClient.createOrder(createOrderReq);
    // place order:

    // ------------------------------------------------------ 执行订单
    // -------------------------------------------------------
    final String r = apiClient.placeOrder(orderId);
    logger.info("执行多单:" + createOrderReq + "结果:" + r);


    buyAccessPool.execute(()->{
    final int buyPrice= price.intValue();
    boolean tradeOver = false;
    int sellPrice = buyPrice  + strategyStatus.getFluctuation();


    while(!tradeOver){
    try {
    OrdersDetailResponse ordersDetail = apiClient
    .ordersDetail(String.valueOf(orderId));
    String state = (String) ((Map) (ordersDetail.getData())).get("state");
    if ("filled".equals(state)) {
    logger.info("多单，订单号:" + orderId + ",完全成交");

    String amount = (String) ((Map) (ordersDetail.getData()))
    .get("field-amount");
    logger.info("准备挂空单，售出价格:" + sellPrice + " ,数量: " + amount);

    sell(sellPrice, amount);
    priceMemery.cleanPrice(buyPrice);
    tradeOver = true;
    }
    Thread.sleep(500);
    } catch (Exception e) {
    logger.error("轮训验证多单信息异常: 订单号:"+orderId+",买入价格:"+buyPrice,e);
    }
    }




    });


    }  */

    /**
     * 限价卖出
     * @param price  10000的价格
     * @param amount

    private void sell(final int price, String amount){
    String truePrice = Double.toString((double) price / 10000);

    AccountsResponse<List<Accounts>> accounts = apiClient.accounts();
    Accounts account = accounts.getData().get(0);
    long accountId = account.getId();
    CreateOrderRequest createOrderReq = new CreateOrderRequest();
    createOrderReq.accountId = String.valueOf(accountId);
    createOrderReq.amount = String.valueOf(strategyStatus.getLotSize());
    createOrderReq.price = truePrice;
    createOrderReq.symbol = "htusdt";
    createOrderReq.type = CreateOrderRequest.OrderType.SELL_LIMIT;
    createOrderReq.source = "api";

    // ------------------------------------------------------ 创建订单
    // -------------------------------------------------------
    long orderId = apiClient.createOrder(createOrderReq);

    // ------------------------------------------------------ 执行订单
    // -------------------------------------------------------
    String r = apiClient.placeOrder(orderId);
    logger.info("执行挂空单:" + createOrderReq + "结果:" + r);
    priceMemery.sellOrder(new BigDecimal(price));
    //验证空单结果
    sellAccessPool.execute(()->{
    boolean sellSuccess = false;

    while(!sellSuccess){
    try{

    OrdersDetailResponse ordersDetail = apiClient
    .ordersDetail(String.valueOf(orderId));
    String state = (String) ((Map) (ordersDetail.getData())).get("state");
    if ("filled".equals(state)) {
    logger.info("空单，价格约" + price + "点，订单号:" + orderId + ",完全成交");
    priceMemery.cleanPrice(price);

    sellSuccess =true;
    }
    Thread.sleep(100);


    }catch (Exception e){
    logger.error("轮训验证空单信息异常: 订单价格:"+price+"手数"+amount,e);
    }
    }
    });
    }
     */
}
