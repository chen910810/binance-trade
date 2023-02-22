package com.binance.quantify.module.member.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author lw
 */
@Data
public class BinanceMemberKey implements Serializable {

  private static final long serialVersionUID = 5908868654698672092L;
  /**
   * ID
   */
  private Integer id;
  /**
   * 用户ID
   */
  private Integer memberId;
  /**
   * Access Key
   */
  private String accessKey;
  /**
   * Secret Key
   */
  private String secretKey;
  /**
   * 创建时间
   */
  private Date createTime;

  private Integer code;
}
