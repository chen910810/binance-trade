package com.binance.quantify.module.member.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author lw
 */
@Data
public class BinanceMember implements Serializable {

  private static final long serialVersionUID = -8499606556393883691L;
  /**
   * ID
   */
  private Integer id;
  /**
   * 用户登录名
   */
  private String username;
  /**
   * 用户密码
   */
  private String password;
  /**
   * 上次登录时间
   */
  private Date lastLoginTime;
  /**
   * 账号激活, 0: 未激活, 1: 已激活
   */
  private Integer isActive;
  /**
   * 绑定授权, 0: 未绑定, 1: 已绑定
   */
  private Integer isAuth;
  /**
   * 谷歌授权码
   */
  private String authCode;
  /**
   * 创建时间
   */
  private Date createTime;
  /**
   * 修改时间
   */
  private Date updateTime;

}
