package com.estela.neko.domain;

import com.estela.neko.Enutype.TradeModelType;
import com.estela.neko.common.StrategyStatus;
import com.estela.neko.config.Diamond;
import com.estela.neko.core.PriceStrategy;

/**
 * @author fuming.lj 2018/9/29
 * 多重交易模型
 **/
public class TradeDimension {

    private String apiKey;;
    private String securityKey;
    //交易货币
    private String tradeSemaphore;
    //交易模型
    private TradeModelType tradeModelType;
    //交易系统控制配置
    private Diamond diamond = new Diamond();

    //数据层 系统模型配置
    private StrategyStatus strategyStatus = new StrategyStatus();

    private String accountId ;

    private String currency;




    private PriceStrategy priceStrategy;

    public void setPriceStrategy(PriceStrategy priceStrategy){
        this.priceStrategy = priceStrategy;
    }

    public PriceStrategy getPriceStrategy() {
        return priceStrategy;
    }

    public StrategyStatus getStrategyStatus() {
        return strategyStatus;
    }

    public void setFluFromDimension(int flu){
        strategyStatus.setFluctuation(flu);
    }

    public Diamond getDiamond() {
        return diamond;
    }


    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecurityKey() {
        return securityKey;
    }

    public void setSecurityKey(String securityKey) {
        this.securityKey = securityKey;
    }

    public String getTradeSemaphore() {
        return tradeSemaphore;
    }

    public void setTradeSemaphore(String tradeSemaphore) {
        this.tradeSemaphore = tradeSemaphore;
    }

    public TradeModelType getTradeModelType() {
        return tradeModelType;
    }

    public void setTradeModelType(TradeModelType tradeModelType) {
        this.tradeModelType = tradeModelType;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        return "TradeDimension{" +
            "apiKey='" + apiKey + '\'' +
            ", securityKey='" + securityKey + '\'' +
            ", tradeSemaphore='" + tradeSemaphore + '\'' +
            ", tradeModelType=" + tradeModelType +
            ", diamond=" + diamond +
            ", strategyStatus=" + strategyStatus +
            ", accountId='" + accountId + '\'' +
            ", currency='" + currency + '\'' +
            ", priceStrategy=" + priceStrategy +
            '}';
    }
}
