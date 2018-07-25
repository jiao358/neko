package com.estela.neko.common;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author fuming.lj 2018/7/23
 **/
@Service
public class StrategyStatus {

    private Boolean trading = false;

    private BigDecimal tradingPosition;
    /**
     * 需要是乘以10000的价格
     */
    private BigDecimal riskPrice = new BigDecimal(0);

    private BigDecimal money;

    /**
     * 最多持有订单数
     */
    private int maxOrderSize = 20;
    /**
     * 交易手数
     */
    private double lotSize = 1;


    public int getMaxOrderSize() {
        return maxOrderSize;
    }

    public void setMaxOrderSize(int maxOrderSize) {
        this.maxOrderSize = maxOrderSize;
    }

    /**
     * 计算误差  3个点以内
     */
    private int diffPrice =3;
    /**
     * 已经完成的卖单
     */
    private AtomicInteger completeTrade= new AtomicInteger();
    /**
     * 浮动交易点数
     */
    private Integer fluctuation =100;

    /**
     * 起始执行价格  会以这个价格为基础 向上 或者向下增加 浮动交易点数策略
     */
    private BigDecimal startOrder  =null;

    private String sysMsg;





    /**
     * 判断当前价格是否 满足交易策略
     *
     * @param currentPrice  当前价格
     * @param lastPrice    最近一次交易价格
     * @return
     */
    public boolean isSatisfyTrading (int currentPrice, int lastPrice) {
        int lot = diffPrice;
        int result = (currentPrice -lastPrice)%100;
        if(result<0 &&  ((result+100)<=lot || result >= (-1*lot))){
            System.out.println("在范围内 负值");
            return true;
        }
        if(result>=0 && (result<=lot || result -100 >=(-1*lot)) ){
            System.out.println("在范围内");
            return true;
        }

        return false;
    }



    public int getDiffPrice() {
        return diffPrice;
    }

    public void setDiffPrice(int diffPrice) {
        this.diffPrice = diffPrice;
    }

    public String getSysMsg() {
        return sysMsg;
    }

    public void setSysMsg(String sysMsg) {
        this.sysMsg = sysMsg;
    }

    public Boolean getTrading() {
        return trading;
    }

    public void setTrading(Boolean trading) {
        this.trading = trading;
    }

    public BigDecimal getTradingPosition() {
        return tradingPosition;
    }

    public BigDecimal getMoney() {
        return money;
    }

    public void setMoney(BigDecimal money) {
        this.money = money;
    }

    public Integer getFluctuation() {
        return fluctuation;
    }

    public void setFluctuation(Integer fluctuation) {
        this.fluctuation = fluctuation;
    }

    public BigDecimal getStartOrder() {
        return startOrder;
    }

    public void setStartOrder(BigDecimal startOrder) {
        this.startOrder = startOrder;
    }

    public void setTradingPosition(BigDecimal tradingPosition) {
        this.tradingPosition = tradingPosition;
    }

    public BigDecimal getRiskPrice() {
        return riskPrice;
    }

    public void setRiskPrice(BigDecimal riskPrice) {
        this.riskPrice = riskPrice;
    }

    public BigDecimal getMemory() {
        return money;
    }

    public void setMemory(BigDecimal memory) {
        this.money = memory;
    }


    public void completeTrade(){
        completeTrade.incrementAndGet();
    }

    public int getCompleteTrade(){
        return completeTrade.get();
    }

    public double getLotSize() {
        return lotSize;
    }

    public void setLotSize(double lotSize) {
        this.lotSize = lotSize;
    }
}
