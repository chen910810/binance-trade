package com.binance.quantify.module.symbol.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.binance.quantify.module.symbol.dao.BinanceSymbolDao;
import com.binance.quantify.module.symbol.service.BinanceSymbolService;
import com.binance.quantify.utils.BinanceClient;
import com.binance.quantify.utils.BinanceConstants;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

@Log4j2
@Service
public class BinanceSymbolServiceImpl implements BinanceSymbolService {

    @Resource
    private BinanceSymbolDao binanceSymbolDao;
    @Resource
    private BinanceClient binanceSpotClient;
    @Resource
    private RedisTemplate db2RedisTemplate;

    @Override
    @Cacheable(value = "binanceSymbol", key = "'binanceSymbol_All'")
    public List<String> getBinanceSymbol() {
        return binanceSymbolDao.getBinanceSymbol();
    }

    @Override
    public void cacheRedisSymbolPrice(String symbol) {
        try {
            String resultStr = binanceSpotClient.getPublicApi("/api/v3/ticker/price", "symbol=" + symbol);
            log.debug("symbol {} , resultStr {} ",symbol,resultStr);
            if(StringUtils.isNotBlank(resultStr)){
                JSONObject resultJson = JSONObject.parseObject(resultStr);
                BigDecimal price = resultJson.getBigDecimal("price");
                symbol = resultJson.getString("symbol");
                db2RedisTemplate.boundHashOps(BinanceConstants.BN_MARKET_PRICE).put(symbol, price.toString());
            }
        }catch (Exception e){
            e.getMessage();
            log.debug("marketPrice client error {}",e.getMessage());
        }
    }

}
