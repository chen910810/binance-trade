package com.binance.quantify.module.symbol.service;

import java.util.List;

public interface BinanceSymbolService {

    List<String> getBinanceSymbol();

    void cacheRedisSymbolPrice(String symbol);

}
