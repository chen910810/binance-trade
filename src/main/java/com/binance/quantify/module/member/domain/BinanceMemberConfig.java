package com.binance.quantify.module.member.domain;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author lw
 */
@Data
public class BinanceMemberConfig implements Serializable {

  private static final long serialVersionUID = 7994012425059397374L;
  /**
   * 用户ID
   */
  private Integer memberId;
  /**
   * 币种
   */
  private String symbol;
  /**
   * 开仓方向, buy/sell
   */
  private String tradeType;
  /**
   * 是否托管,0:关闭, 1:启动
   */
  private Integer isActive;

  private Integer isPosition;
  /**
   * 下单加仓步长 (USD)
   */
  private BigDecimal tradeStep;
  /**
   * 平仓盈利 (USD)
   */
  private BigDecimal profit;
  /**
   * 价格模式, 0:默认模式, 1:价格区间模式
   */
  private Integer priceModel;
  /**
   * 利润模式, 0:均价模式, 1:单笔模式
   */
  private Integer profitModel;
  /**
   * 预开仓模式，0关闭，1:开启
   */
  private Integer tradeModel;
  /**
   * 最小交易价格 (USD)
   */
  private BigDecimal minMoney;
  /**
   * 最大交易价格 (USD)
   */
  private BigDecimal maxMoney;
  /**
   * 最大持仓数量 (USD)
   */
  private BigDecimal positionMaxNumber;
  /**
   * 创建时间
   */
  private Date createTime;
  /**
   * 修改时间
   */
  private Date updateTime;

  /**
   * 以下虚拟字段 可以理解为单笔下单金额（USD）
   */
  private BigDecimal consignAmount;
  /**
   * 可以理解为单笔下单数量（USD）
   */
  private BigDecimal consignVolume;
  /**
   * 可以理解为当前订单是否为预开仓订单
   */
  private Integer expectedFlag;

}
