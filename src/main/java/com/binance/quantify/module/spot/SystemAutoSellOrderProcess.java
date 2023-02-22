package com.binance.quantify.module.spot;

import com.alibaba.fastjson.JSONObject;
import com.binance.quantify.config.annotation.BALock;
import com.binance.quantify.module.member.domain.BinanceMemberConfig;
import com.binance.quantify.module.spot.domain.BinanceOrderDo;
import com.binance.quantify.module.spot.service.BinanceOrderService;
import com.binance.quantify.module.tradeClient.service.BinanceTradeClientService;
import com.binance.quantify.utils.BinanceConstants;
import com.binance.quantify.utils.BinanceTradeServiceUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;

@Log4j2
@Component
public class SystemAutoSellOrderProcess {

    @Resource
    private BinanceTradeClientService binanceTradeClientService;
    @Resource
    private BinanceOrderService binanceOrderService;
    @Resource
    private RedisTemplate db2RedisTemplate;
    @Resource
    private RedisTemplate db10RedisTemplate;

    @Async
    @BALock
    public void processActiveSellOrderJob(BinanceMemberConfig memberConfig){
        String symbol = memberConfig.getSymbol().toUpperCase();
        Integer memberId = memberConfig.getMemberId();
        String side = memberConfig.getTradeType().toUpperCase();
        String currentNewPriceStr = (String) db2RedisTemplate.boundHashOps(BinanceConstants.BN_MARKET_PRICE).get(symbol);
        log.debug("品种 {} , 当前最新价格 {} ",symbol,currentNewPriceStr);
        if(StringUtils.isBlank(currentNewPriceStr)) return;
        //当前最新价格
        BigDecimal currentNewPrice = new BigDecimal(currentNewPriceStr);
        log.debug("processActivePositionOrderJob [{} - {} ] start...", memberId, symbol);
        BinanceOrderDo binanceOrder = binanceOrderService.getBinanceOrderByPrice(memberId, symbol, side, currentNewPrice.add(memberConfig.getProfit()),3);
        if(ObjectUtils.isEmpty(binanceOrder)){
            return;
        }
        //获取当前订单
        JSONObject orderParamJson = BinanceTradeServiceUtils.getDefaultPostData();
        orderParamJson.put("symbol",memberConfig.getSymbol());
        orderParamJson.put("orderId",binanceOrder.getCloseOrderId());
        String orderParamDataStr = BinanceTradeServiceUtils.getQueryStr(orderParamJson);
        JSONObject openOrderJson = binanceTradeClientService.getOpenOrder(memberId, BinanceConstants.API_ORDER, orderParamDataStr);
        if(ObjectUtils.isEmpty(openOrderJson)){
            log.debug("get open order 查询订单失败!");
            return;
        }else if(openOrderJson.containsKey("orderId")){
            String status = openOrderJson.getString("status");
            BigDecimal executedQty = openOrderJson.getBigDecimal("executedQty");
            log.debug("查询订单状态 status {} , 累计成交的币种数量 {} , 订单委托的数量 {} ",status,executedQty,binanceOrder.getConsignAmount());
            if(status.equalsIgnoreCase("FILLED")){
                //成交
                BinanceOrderDo newOrder = new BinanceOrderDo();
                newOrder.setProfit(openOrderJson.getBigDecimal("price").subtract(binanceOrder.getPrice()));
                newOrder.setStatus(4);
                newOrder.setCloseAvgPrice(openOrderJson.getBigDecimal("price"));
                newOrder.setUpdateTime(new Date());
                newOrder.setOrderId(binanceOrder.getOrderId());
                newOrder.setMemberId(memberId);
                newOrder.setSymbol(symbol);
                newOrder.setStatusTag(3);
                binanceOrderService.updateBinanceOrderDo(newOrder);

                //remove 缓存价格
                removeCacheRedisSymbolPrice(memberConfig.getMemberId(),binanceOrder.getPrice(),binanceOrder.getSymbol());
                //缓存减去累计开仓量
                cacheSubRedisSymbolAmount(memberConfig.getMemberId(),binanceOrder.getConsignAmount(),binanceOrder.getSymbol());
                if(binanceOrder.getExpectedFlag().equals(1)){
                    String REDIS_KEY = String.format(BinanceConstants.BN_SYMBOL_EXPECTED_ORDER,memberConfig.getMemberId(),memberConfig.getSymbol());
                    db2RedisTemplate.delete(REDIS_KEY);
                }
                String REDIS_KEY = String.format("%s%s%s",binanceOrder.getOrderId(),binanceOrder.getBnOrderId(),binanceOrder.getMemberId());
                db10RedisTemplate.delete(REDIS_KEY);
            }
        }
    }

    //remove 缓存价格
    private void removeCacheRedisSymbolPrice(Integer memberId,BigDecimal orderPrice,String symbol){
        String REDIS_KEY = String.format(BinanceConstants.BN_SYMBOL_CONSIGN_PRICE,memberId,symbol);
        db2RedisTemplate.opsForSet().remove(REDIS_KEY,orderPrice);
    }
    //缓存减去累计开仓量
    private void cacheSubRedisSymbolAmount(Integer memberId,BigDecimal consignAmount,String symbol){
        String REDIS_KEY = String.format(BinanceConstants.BN_SYMBOL_CONSIGN_AMOUNT,memberId);
        String totalAmount = (String) db2RedisTemplate.boundHashOps(REDIS_KEY).get(symbol);
        if(StringUtils.isNotBlank(totalAmount)){//累计成交金额
            consignAmount = new BigDecimal(totalAmount).subtract(consignAmount);
        }
        db2RedisTemplate.boundHashOps(REDIS_KEY).put(symbol, consignAmount.toString());
    }

}
