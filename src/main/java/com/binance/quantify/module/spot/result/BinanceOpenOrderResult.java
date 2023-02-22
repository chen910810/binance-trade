package com.binance.quantify.module.spot.result;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class BinanceOpenOrderResult {

    private String symbol;

    private Integer orderId;

    private Integer orderListId;

    private String clientOrderId;

    private BigDecimal price;

    private BigDecimal origQty;

    private BigDecimal executedQty;

    private BigDecimal cummulativeQuoteQty;

    private String status;

    private String timeInForce;

    private String type;

    private String side;

    private BigDecimal stopPrice;

    private BigDecimal icebergQty;

    private long time;

    private long updateTime;

    private boolean isWorking;

    private long workingTime;

    private BigDecimal origQuoteOrderQty;

    private String selfTradePreventionMode;

}
