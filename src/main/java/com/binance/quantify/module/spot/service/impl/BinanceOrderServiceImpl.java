package com.binance.quantify.module.spot.service.impl;

import com.binance.quantify.module.spot.dao.BinanceOrderDao;
import com.binance.quantify.module.spot.domain.BinanceOrderDo;
import com.binance.quantify.module.spot.service.BinanceOrderService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.math.BigDecimal;

@Service
public class BinanceOrderServiceImpl implements BinanceOrderService {

    @Resource
    private BinanceOrderDao binanceOrderDao;

    @Override
    public void saveBinanceOrderDo(BinanceOrderDo binanceOrderDo) {
        binanceOrderDao.saveBinanceOrderDo(binanceOrderDo);
    }

    @Override
    public void updateBinanceOrderDo(BinanceOrderDo binanceOrderDo) {
        binanceOrderDao.updateBinanceOrderDo(binanceOrderDo);
    }

    @Override
    public BinanceOrderDo getBinanceOrderByOrderId(String orderId, Integer memberId, Integer status, String symbol, String bnOrderId) {
        return binanceOrderDao.getBinanceOrderByOrderId(orderId, memberId, status, symbol, bnOrderId);
    }

    @Override
    public BinanceOrderDo getBinanceOrderByPrice(Integer memberId, String symbol, String side, BigDecimal price,Integer status) {
        return binanceOrderDao.getBinanceOrderByPrice(memberId, symbol, side, price,status);
    }

    @Override
    public BinanceOrderDo getBinanceOrderLimt(Integer memberId, String symbol, String side, BigDecimal price, Integer status) {
        return binanceOrderDao.getBinanceOrderLimt(memberId, symbol, side, price, status);
    }

    @Override
    public BinanceOrderDo getSellBinanceOrder(String positionOrderId, String closeBnOrderId, Integer memberId, Integer status, String symbol) {
        return binanceOrderDao.getSellBinanceOrder(positionOrderId, closeBnOrderId, memberId, status, symbol);
    }

}
