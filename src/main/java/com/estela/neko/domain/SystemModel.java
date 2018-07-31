package com.estela.neko.domain;

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

    public void setFloatStrategy(String floatStrategy) {
        this.floatStrategy = floatStrategy;
    }
}