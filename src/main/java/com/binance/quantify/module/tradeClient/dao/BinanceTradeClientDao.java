package com.binance.quantify.module.tradeClient.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BinanceTradeClientDao {

    @Update("insert into bo_api_error_logs(member_id, remark, api_market, api_url, api_method, api_info, api_result, api_server, api_time, create_time) " +
            "values(#{memberId}, #{remark}, #{marketType}, #{apiUrl}, #{apiMethod}, #{apiData}, #{apiResult}, #{clientIp}, #{apiTime}, NOW())")
    void saveApiErrorLog(Integer memberId, String remark, String marketType, String apiUrl, String apiMethod, String apiData,
                          String apiResult, String clientIpAddress, Long apiTime);

}
