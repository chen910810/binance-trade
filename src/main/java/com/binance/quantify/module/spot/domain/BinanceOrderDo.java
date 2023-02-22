package com.binance.quantify.module.spot.domain;

import lombok.Data;
import lombok.ToString;
import java.math.BigDecimal;
import java.util.Date;

@Data
@ToString
public class BinanceOrderDo {

    private static final long serialVersionUID = 8735966143766816781L;

    //系统订单id
    private String orderId;
    //用户id
    private Integer memberId;
    //交易品种
    private String symbol;
    //币安建仓订单id
    private String bnOrderId;
    //交易的时间戳
    private String transactTime;
    //订单方向 buy买,sell卖
    private String side;
    //订单类型: MARKET、LIMIT
    private String type;
    //委托订单数量
    private BigDecimal consignVolume;
    //委托订单金额
    private BigDecimal consignAmount;
    //委托价格
    private BigDecimal price;
    //收益
    private BigDecimal profit;
    //交易成交数量
    private BigDecimal tradeVolume;
    //交易订单金额(累计交易的金额)
    private BigDecimal tradeAmount;
    //成交时间
    private Date tradeDate;
    //建仓成交均价
    private BigDecimal tradeAvgPrice;
    //0开仓委托,1全部成交,2开仓撤单,3平仓委托,4完成
    private Integer status;
    //1正常0开仓委托异常2平仓委托异常
    private Integer returnStatus;
    //开仓手续费
    private BigDecimal openFee;
    //平仓订单id
    private String positionOrderId;
    //委托平仓价格
    private BigDecimal closePrice;
    //委托成交均价
    private BigDecimal closeAvgPrice;
    //委托成交时间
    private Date closeDate;
    //订单创建时间
    private Date createTime;
    //委托订单id
    private String closeOrderId;
    //平仓手续费
    private BigDecimal closeFee;
    //更新时间
    private Date updateTime;
    //开仓反馈消息
    private String openMessage;
    //平仓反馈消息
    private String closeMessage;
    //当前订单是否为预开仓订单
    private Integer expectedFlag;

    //虚拟字段
    private Integer statusTag;
}
