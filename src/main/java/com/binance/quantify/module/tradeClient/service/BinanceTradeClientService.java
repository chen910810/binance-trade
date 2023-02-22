package com.binance.quantify.module.tradeClient.service;

import com.alibaba.fastjson.JSONObject;
import com.binance.quantify.module.spot.result.BinanceOpenOrderResult;

import java.util.List;

public interface BinanceTradeClientService {

    JSONObject sendSpotTradeOrder(Integer memberId, String apiUrl, String apiDataStr);

    JSONObject getOpenOrder(Integer memberId, String apiUrl, String apiDataStr);

    List<BinanceOpenOrderResult> getOpenOrders(Integer memberId, String apiUrl, String apiDataStr);

    JSONObject revokeOpenOrder(Integer memberId, String apiUrl, String apiDataStr);
}
