package com.estela.neko.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author fuming.lj 2018/8/11
 * 用于记录 当天金额
 **/
@Component
public class FundReport {
    private static final Logger logger = LoggerFactory.getLogger(FundReport.class);
    /**
     * sell 的手续费
      */
    private BigDecimal sellFee = new BigDecimal("0");
    /**
     * buyer 的手续费
     */
    private BigDecimal buyFee = new BigDecimal("0");

    private BigDecimal profit = new BigDecimal("0");


    /**
     * key  sellIrderUd
     * value  buyFundReport
     */
    private Map<Long,FundDomain> orderMapTemp = new ConcurrentHashMap<>();



    public void setFundReportUnit(FundDomain fundDomain ,Long sellOrderId){
        orderMapTemp.put(sellOrderId,fundDomain);

    }

    /**
     * 当sellOrder 完全成交  则进行 feeUsdt的计算
     * @param sellDomain
     */
    public synchronized void calculate(FundDomain sellDomain){
        try{
            if(!orderMapTemp.containsKey(sellDomain.getOrderId())){
                return ;
            }

            FundDomain buyerDomain  = orderMapTemp.get(sellDomain.getOrderId());
            BigDecimal sellCash = sellDomain.getRealCash();
            this.sellFee = this.sellFee.add(sellDomain.getFee());


            this.buyFee = this.buyFee.add(buyerDomain.getFee());
            BigDecimal buyCash = buyerDomain.getRealCash();

            this.profit = this.profit.add(sellCash.subtract(buyCash));

        }catch (Exception e){
            logger.error("进行利润计算失败: sellDomain:"+sellDomain ,e );
        }finally {
            orderMapTemp.remove(sellDomain.getOrderId());
        }



    }

    /**
     * 整点的时候清除当天记录
     */
    public void cleanMap(){
       profit = new BigDecimal("0");
       sellFee = new BigDecimal("0");
       buyFee = new BigDecimal("0");
    }

    public BigDecimal getSellFee() {
        return sellFee;
    }

    public BigDecimal getBuyFee() {
        return buyFee;
    }

    public BigDecimal getProfit() {
        return profit;
    }
}
