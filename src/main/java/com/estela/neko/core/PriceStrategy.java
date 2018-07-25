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
import java.util.List;
import java.util.Map;
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

    private ScheduledExecutorService tradingSchedule = new ScheduledThreadPoolExecutor(15);

    private ExecutorService buyAccessPool = Executors.newFixedThreadPool(10);
    private ExecutorService sellAccessPool = Executors.newFixedThreadPool(5);



    /**
     * 如果刷新程序没有停止, 则以每500毫秒刷新一次最近的价格字段
     */
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

            }, 100, 500, TimeUnit.MILLISECONDS);
        }



        tradingSchedule.scheduleAtFixedRate(() -> {

            BigDecimal currentPrice = priceMemery.getCurrentPrice();
            autoMartket(currentPrice);
        }, 200, 250, TimeUnit.MILLISECONDS);
    }

    /**
     * 自动进行验证交易
     * @Param currentPrice  应该乘以10000的价格
     */
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

    }


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
     */
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
                    /*      double parseDouble = Double.parseDouble(amount);
                          if (parseDouble < 0.1) {
                              amount = "0.1";
                          } else {
                              BigDecimal bg = new BigDecimal(amount).setScale(2, RoundingMode.UP);
                              amount = bg.toString();
                          }
*/
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


    }

    /**
     * 限价卖出
     * @param price  10000的价格
     * @param amount
     */
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

}
