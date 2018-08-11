package com.estela.neko.common;

import java.math.BigDecimal;

/**
 * @author fuming.lj 2018/8/11
 * 交易金额模型
 **/
public class FundDomain {

    private Long orderId;

    private BigDecimal realCash ;

    private BigDecimal fee;

    public FundDomain(Long orderId, String realCash, String fee) {
        this.orderId = orderId;
        this.realCash = new BigDecimal(realCash);
        this.fee = new BigDecimal(fee);
    }

    public Long getOrderId() {
        return orderId;
    }

    public BigDecimal getRealCash() {
        return realCash;
    }

    public BigDecimal getFee() {
        return fee;
    }

    @Override
    public String toString() {
        return "FundDomain{" +
            "orderId=" + orderId +
            ", realCash=" + realCash +
            ", fee=" + fee +
            '}';
    }
}
