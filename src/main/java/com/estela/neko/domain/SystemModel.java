package com.estela.neko.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author fuming.lj 2018/7/31
 * 页面展示逻辑
 **/
public class SystemModel {
    /**当前价格**/
    private int currentPrice;
    /**当前系统运行性状态*/
    private String running;

    /**当前系统交易手数*/
    private double lotSize;
    //系统最高风险控价
    private int highriskPrice = 99999;
    //系统最低风险控价
    private int lowRisiPrice=  0 ;
    /**
     * 计算误差  5个点以内
     */
    private int diffPrice = 5;
    /**
     * 最多持有订单数
     */
    private int maxOrderSize = 20;

    /**
     * 浮动交易点数
     */
    private Integer fluctuation = 100;

    /**
     * 点差计算取舍逻辑
     */
    private String floatStrategy ;

    private List<Integer> buyOrder;

    private List<Integer> sellOrder;

    private String buyAndOrder;
    private String sellAndOrder;


    private int alreadyDual;

    private int todayTrade;

    private BigDecimal profit;

    private BigDecimal sellFee;
    private BigDecimal buyFee;

    public int getTodayTrade() {
        return todayTrade;
    }

    public void setTodayTrade(int todayTrade) {
        this.todayTrade = todayTrade;
    }

    public String getBuyAndOrder() {
        return buyAndOrder;
    }

    public void setBuyAndOrder(String buyAndOrder) {
        this.buyAndOrder = buyAndOrder;
    }

    public String getSellAndOrder() {
        return sellAndOrder;
    }

    public void setSellAndOrder(String sellAndOrder) {
        this.sellAndOrder = sellAndOrder;
    }

    public List<Integer> getBuyOrder() {
        return buyOrder;
    }

    public void setBuyOrder(List<Integer> buyOrder) {
        this.buyOrder = buyOrder;
    }

    public List<Integer> getSellOrder() {
        return sellOrder;
    }

    public void setSellOrder(List<Integer> sellOrder) {
        this.sellOrder = sellOrder;
    }

    public int getAlreadyDual() {
        return alreadyDual;
    }

    public void setAlreadyDual(int alreadyDual) {
        this.alreadyDual = alreadyDual;
    }

    public int getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(int currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getRunning() {
        return running;
    }

    public void setRunning(String running) {
        this.running = running;
    }

    public double getLotSize() {
        return lotSize;
    }

    public void setLotSize(double lotSize) {
        this.lotSize = lotSize;
    }

    public int getHighriskPrice() {
        return highriskPrice;
    }

    public void setHighriskPrice(int highriskPrice) {
        this.highriskPrice = highriskPrice;
    }

    public int getLowRisiPrice() {
        return lowRisiPrice;
    }

    public void setLowRisiPrice(int lowRisiPrice) {
        this.lowRisiPrice = lowRisiPrice;
    }

    public int getDiffPrice() {
        return diffPrice;
    }

    public void setDiffPrice(int diffPrice) {
        this.diffPrice = diffPrice;
    }

    public int getMaxOrderSize() {
        return maxOrderSize;
    }

    public void setMaxOrderSize(int maxOrderSize) {
        this.maxOrderSize = maxOrderSize;
    }

    public Integer getFluctuation() {
        return fluctuation;
    }

    public void setFluctuation(Integer fluctuation) {
        this.fluctuation = fluctuation;
    }

    public String getFloatStrategy() {
        return floatStrategy;
    }

    public BigDecimal getProfit() {
        return profit;
    }

    public void setProfit(BigDecimal profit) {
        this.profit = profit;
    }

    public BigDecimal getSellFee() {
        return sellFee;
    }

    public void setSellFee(BigDecimal sellFee) {
        this.sellFee = sellFee;
    }

    public BigDecimal getBuyFee() {
        return buyFee;
    }

    public void setBuyFee(BigDecimal buyFee) {
        this.buyFee = buyFee;
    }

    public void setFloatStrategy(String floatStrategy) {
        this.floatStrategy = floatStrategy;
    }
}
