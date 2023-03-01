package com.binance.quantify.module.spot;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.binance.quantify.config.annotation.BALock;
import com.binance.quantify.module.member.domain.BinanceMemberConfig;
import com.binance.quantify.module.spot.domain.BinanceOrderDo;
import com.binance.quantify.module.spot.param.BinanceApiSendOrderParam;
import com.binance.quantify.module.spot.result.BinanceOpenOrderResult;
import com.binance.quantify.module.spot.service.BinanceOrderService;
import com.binance.quantify.module.tradeClient.service.BinanceTradeClientService;
import com.binance.quantify.utils.BinanceCommonUtil;
import com.binance.quantify.utils.BinanceConstants;
import com.binance.quantify.utils.BinanceTradeServiceUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Component
public class SystemAutoPositionOrderProcess {

    @Resource
    private BinanceTradeClientService binanceTradeClientService;
    @Resource
    private BinanceOrderService binanceOrderService;
    @Resource
    private RedisTemplate db2RedisTemplate;
    @Resource
    private RedisTemplate db10RedisTemplate;

    @Async
    //@BALock
    public void processActivePositionOrderJob(BinanceMemberConfig memberConfig){
        String symbol = memberConfig.getSymbol().toUpperCase();
        Integer memberId = memberConfig.getMemberId();
        String side = memberConfig.getTradeType().toUpperCase();
        String currentNewPriceStr = (String) db2RedisTemplate.boundHashOps(BinanceConstants.BN_MARKET_PRICE).get(symbol);
        log.debug("品种 {} , 当前最新价格 {} ",symbol,currentNewPriceStr);
        if(StringUtils.isBlank(currentNewPriceStr)) return;
        //当前最新价格
        BigDecimal currentNewPrice = new BigDecimal(currentNewPriceStr);
        log.debug("processActivePositionOrderJob [{} - {} ] start...", memberId, symbol);
        BinanceOrderDo binanceOrder = binanceOrderService.getBinanceOrderByPrice(memberId, symbol, side, currentNewPrice.subtract(memberConfig.getTradeStep()),0);
        if(ObjectUtils.isEmpty(binanceOrder)){
            return;
        }
        //获取当前订单
        JSONObject orderParamJson = BinanceTradeServiceUtils.getDefaultPostData();
        orderParamJson.put("symbol",memberConfig.getSymbol());
        orderParamJson.put("orderId",binanceOrder.getBnOrderId());
        String orderParamDataStr = BinanceTradeServiceUtils.getQueryStr(orderParamJson);
        JSONObject openOrderJson = binanceTradeClientService.getOpenOrder(memberId, BinanceConstants.API_ORDER, orderParamDataStr);
        if(ObjectUtils.isEmpty(openOrderJson)){
            log.debug("get open order 查询订单失败!");
            return;
        }else if(openOrderJson.containsKey("orderId")){
            String status = openOrderJson.getString("status");
            BigDecimal executedQty = openOrderJson.getBigDecimal("executedQty");
            log.debug("查询订单状态 status {} , 累计成交的币种数量 {} , 订单委托的数量 {} ",status,executedQty,binanceOrder.getConsignAmount());
            if(status.equalsIgnoreCase("FILLED") && executedQty.compareTo(binanceOrder.getConsignVolume()) > -1){

                BigDecimal cummulativeQuoteQty = openOrderJson.getBigDecimal("cummulativeQuoteQty");
                long time = openOrderJson.getLong("time");
                //成交
                BinanceOrderDo newOrder = new BinanceOrderDo();
                newOrder.setTradeDate(new Date(time));
                newOrder.setTradeVolume(executedQty);
                newOrder.setTradeAmount(cummulativeQuoteQty);
                newOrder.setTradeAvgPrice(openOrderJson.getBigDecimal("price"));
                newOrder.setOpenFee(BigDecimal.ZERO);
                newOrder.setStatus(1);
                newOrder.setUpdateTime(new Date());
                newOrder.setOpenTradeMessage(openOrderJson.toJSONString());
                newOrder.setOrderId(openOrderJson.getString("clientOrderId"));
                newOrder.setMemberId(memberId);
                newOrder.setSymbol(symbol);
                newOrder.setStatusTag(0);
                binanceOrderService.updateBinanceOrderDo(newOrder);

                binanceOrder = binanceOrderService.getBinanceOrderByOrderId(binanceOrder.getOrderId(),binanceOrder.getMemberId(),1,binanceOrder.getSymbol(),binanceOrder.getBnOrderId());
                //去挂单
                doOrderSell(binanceOrder,memberConfig);
                //检测卖单挂单数量
                checkSellOrderNum(memberConfig);
            }
        }
    }
    //检测卖单挂单数量
    private void checkSellOrderNum(BinanceMemberConfig memberConfig){
        List<BinanceOpenOrderResult> sellOrderResult = getOpenOrders(memberConfig);
        if(!CollectionUtils.isEmpty(sellOrderResult)){
            List<BinanceOpenOrderResult> sellOpenOrderResult = sellOrderResult.stream().filter(item -> item.getSide().equalsIgnoreCase("SELL")).collect(Collectors.toList());
            log.debug("检查当前卖单数量 {} ",sellOpenOrderResult.size());
            if(sellOpenOrderResult.size() >= 80){ //若等于80
                BinanceOpenOrderResult maxBinanceOpenOrderResult = sellOpenOrderResult.stream().max(Comparator.comparing(BinanceOpenOrderResult::getPrice)).get();
                //取消挂单
                doCanceledSellOrder(maxBinanceOpenOrderResult,memberConfig);
            }else{//去库中检测是否有撤销的卖单
                BinanceOpenOrderResult maxBinanceOpenOrderResult = sellOpenOrderResult.stream().max(Comparator.comparing(BinanceOpenOrderResult::getPrice)).get();
                BinanceOrderDo binanceOrderDo = binanceOrderService.getBinanceOrderLimt(memberConfig.getMemberId(), memberConfig.getSymbol(),
                        memberConfig.getTradeType().toUpperCase(), maxBinanceOpenOrderResult.getPrice().subtract(memberConfig.getTradeStep()), 1);
                if(ObjectUtils.isNotEmpty(binanceOrderDo)){
                    log.debug("去库中检测是否有卖单撤销后订单，重新挂单.........................");
                    //去挂单
                    doOrderSell(binanceOrderDo,memberConfig);
                }
            }
        }
    }
    //取消卖单
    private void doCanceledSellOrder(BinanceOpenOrderResult maxBinanceOpenOrderResult,BinanceMemberConfig memberConfig){
        //取消最小价格的挂单
        String orderId = maxBinanceOpenOrderResult.getOrderId();
        String newClientOrderId = maxBinanceOpenOrderResult.getClientOrderId();
        String symbol = maxBinanceOpenOrderResult.getSymbol();
        BigDecimal cummulativeQuoteQty = maxBinanceOpenOrderResult.getCummulativeQuoteQty();
        if(cummulativeQuoteQty.compareTo(BigDecimal.ZERO) > 0)
            return;
        //获取所有挂单
        JSONObject orderParamJson = BinanceTradeServiceUtils.getDefaultPostData();
        orderParamJson.put("symbol",memberConfig.getSymbol());
        orderParamJson.put("orderId",orderId);
        orderParamJson.put("newClientOrderId",newClientOrderId);
        String revokeOrderParamDataStr = BinanceTradeServiceUtils.getQueryStr(orderParamJson);
        JSONObject resultJson = binanceTradeClientService.revokeOpenOrder(memberConfig.getMemberId(), BinanceConstants.API_ORDER, revokeOrderParamDataStr);
        if(org.springframework.util.ObjectUtils.isEmpty(resultJson)){
            log.debug("revoke sell order 撤销订单失败!");
            return;
        }else if(resultJson.containsKey("orderId")){
            //撤销订单结果
            if(resultJson.getString("status").equalsIgnoreCase("CANCELED")) { //撤销成功
                BinanceOrderDo sellBinanceOrder = binanceOrderService.getSellBinanceOrder(newClientOrderId, orderId, memberConfig.getMemberId(), 3, symbol);
                if(ObjectUtils.isNotEmpty(sellBinanceOrder)){
                    BinanceOrderDo canceledOrder = new BinanceOrderDo();
                    canceledOrder.setUpdateTime(new Date());
                    canceledOrder.setStatus(1);
                    canceledOrder.setOrderId(sellBinanceOrder.getOrderId());
                    canceledOrder.setMemberId(sellBinanceOrder.getMemberId());
                    canceledOrder.setSymbol(symbol);
                    canceledOrder.setStatusTag(0);
                    binanceOrderService.updateBinanceOrderDo(canceledOrder);
                    String REDIS_KEY = String.format("%s%s%s",sellBinanceOrder.getOrderId(),sellBinanceOrder.getBnOrderId(),sellBinanceOrder.getMemberId());
                    db10RedisTemplate.delete(REDIS_KEY);
                }
            }
        }
    }
    //获取所有挂单
    private List<BinanceOpenOrderResult> getOpenOrders(BinanceMemberConfig memberConfig){
        JSONObject orderParamJson = BinanceTradeServiceUtils.getDefaultPostData();
        orderParamJson.put("symbol",memberConfig.getSymbol());
        String orderParamDataStr = BinanceTradeServiceUtils.getQueryStr(orderParamJson);
        //获取当前所有的挂单
        List<BinanceOpenOrderResult> openOrderResultList = binanceTradeClientService.getOpenOrders(memberConfig.getMemberId(),BinanceConstants.API_OPEN_ORDERS, orderParamDataStr);
        return openOrderResultList;
    }


    private void doOrderSell(BinanceOrderDo binanceOrder, BinanceMemberConfig memberConfig){
        String REDIS_KEY = String.format("%s%s%s",binanceOrder.getOrderId(),binanceOrder.getBnOrderId(),binanceOrder.getMemberId());
        if(db10RedisTemplate.hasKey(REDIS_KEY)){
            return;
        }
        //生成订单ID
        String positionOrderId = String.format(BinanceCommonUtil.prefixRandomOrderId(),memberConfig.getMemberId());
        BinanceApiSendOrderParam sendOrderParam = getBinanceApiSendOrderParam(positionOrderId, binanceOrder, memberConfig);
        JSONObject postData = BinanceTradeServiceUtils.getDefaultPostData();
        postData.putAll(JSONUtil.parseObj(sendOrderParam));
        String sendDataStr = BinanceTradeServiceUtils.getQueryStr(postData);
        JSONObject sendOrderResult = binanceTradeClientService.sendSpotTradeOrder(memberConfig.getMemberId(),BinanceConstants.API_ORDER, sendDataStr);
        log.debug("position open order data: {}, result: {}", postData.toJSONString(), sendOrderResult);
        if(ObjectUtils.isEmpty(sendOrderResult)){
            log.debug("open order 订单交易失败!");
            return;
        }else if(sendOrderResult.containsKey("orderId")){
            db10RedisTemplate.opsForValue().set(REDIS_KEY,"1");
            BinanceOrderDo positionOrder = new BinanceOrderDo();
            positionOrder.setPositionOrderId(positionOrderId);
            positionOrder.setClosePrice(sendOrderResult.getBigDecimal("price"));
            positionOrder.setCloseDate(new Date());
            positionOrder.setCloseOrderId(sendOrderResult.getString("orderId"));
            positionOrder.setUpdateTime(new Date());
            positionOrder.setCloseMessage(sendOrderResult.toJSONString());
            positionOrder.setStatus(3);
            positionOrder.setMemberId(binanceOrder.getMemberId());
            positionOrder.setSymbol(binanceOrder.getSymbol());
            positionOrder.setStatusTag(1);
            positionOrder.setOrderId(binanceOrder.getOrderId());
            binanceOrderService.updateBinanceOrderDo(positionOrder);
        }
    }

    //平仓订单请求参数
    private BinanceApiSendOrderParam getBinanceApiSendOrderParam(String positionOrderId,BinanceOrderDo binanceOrder, BinanceMemberConfig memberConfig){
        BinanceApiSendOrderParam binanceApiSendOrderParam = new BinanceApiSendOrderParam();
        binanceApiSendOrderParam.setSymbol(memberConfig.getSymbol());
        binanceApiSendOrderParam.setSide("SELL");
        binanceApiSendOrderParam.setType(BinanceConstants.TRADE_TYPE_LIMIT);
        binanceApiSendOrderParam.setTimeInForce(BinanceConstants.TRADE_TIME_IN_FORCE);
        binanceApiSendOrderParam.setQuantity(binanceOrder.getTradeVolume());
        binanceApiSendOrderParam.setPrice(binanceOrder.getPrice().add(memberConfig.getProfit()));
        binanceApiSendOrderParam.setNewClientOrderId(positionOrderId);
        return binanceApiSendOrderParam;
    }

}
