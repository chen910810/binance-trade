package com.binance.quantify.module.spot.service;

import com.binance.quantify.module.spot.domain.BinanceOrderDo;

import java.math.BigDecimal;

public interface BinanceOrderService {

    void saveBinanceOrderDo(BinanceOrderDo binanceOrderDo);

    void updateBinanceOrderDo(BinanceOrderDo binanceOrderDo);

    BinanceOrderDo getBinanceOrderByOrderId(String orderId,Integer memberId,Integer status,String symbol,String bnOrderId);

    BinanceOrderDo getBinanceOrderByPrice(Integer memberId, String symbol, String side, BigDecimal price,Integer status);

    BinanceOrderDo getBinanceOrderLimt(Integer memberId, String symbol, String side, BigDecimal price,Integer status);

    BinanceOrderDo getSellBinanceOrder(String positionOrderId,String closeBnOrderId,Integer memberId,Integer status,String symbol);
}
