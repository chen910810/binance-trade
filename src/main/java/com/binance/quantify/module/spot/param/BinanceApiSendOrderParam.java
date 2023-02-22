package com.binance.quantify.module.spot.param;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
public class BinanceApiSendOrderParam {
    //品种
    private String symbol;
    // BUY  SELL
    private String side;
    //LIMIT MARKET STOP_LOSS STOP_LOSS_LIMIT TAKE_PROFIT  TAKE_PROFIT_LIMIT LIMIT_MAKER
    private String type;
    //GTC 成交为止 订单会一直有效，直到被成交或者取消
    //IOC 无法立即成交的部分就撤销 订单在失效前会尽量多的成交。
    //FOK	无法全部立即成交就撤销 如果无法全部成交，订单会失效
    private String timeInForce;
    //数量
    private BigDecimal quantity;
    //quoteOrderQty指定买入或卖出的报价数量，不可与 quantity(数量)同时使用
    private BigDecimal quoteOrderQty;
    //价格
    private BigDecimal price;
    //客户自定义的唯一订单ID。 如果未发送，则自动生成
    private String newClientOrderId;
    //仅 STOP_LOSS, STOP_LOSS_LIMIT, TAKE_PROFIT 和 TAKE_PROFIT_LIMIT 需要此参数。
    private BigDecimal stopPrice;
    //用于 STOP_LOSS, STOP_LOSS_LIMIT, TAKE_PROFIT 和 TAKE_PROFIT_LIMIT 类型的订单
    private String trailingDelta;
    //仅使用 LIMIT, STOP_LOSS_LIMIT, 和 TAKE_PROFIT_LIMIT 创建新的 iceberg 订单时需要此参数。
    private BigDecimal icebergQty;
    //设置响应JSON。ACK，RESULT 或 FULL；MARKET 和 LIMIT 订单类型默认为 FULL，所有其他订单默认为 ACK。
    private String newOrderRespType;
    //允许的 ENUM 取决于交易对的配置。支持的值有 EXPIRE_TAKER，EXPIRE_MAKER，EXPIRE_BOTH，NONE。
    private String selfTradePreventionMode;
    //
    private Integer strategyId;
    //不能低于 1000000
    private Integer strategyType;

}
