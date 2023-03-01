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
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Component
public class SystemAutoOpenTradeProcess {

    @Resource
    private BinanceTradeClientService binanceTradeClientService;
    @Resource
    private BinanceOrderService binanceOrderService;
    @Resource
    private RedisTemplate db2RedisTemplate;

    @Async
    //@BALock
    public void processActiveMemberOpenJob(BinanceMemberConfig memberConfig){
        String symbol = memberConfig.getSymbol().toUpperCase();
        Integer memberId = memberConfig.getMemberId();
        log.debug("processActiveMemberOpenJob [{} - {} ] start...", memberId, symbol);
        //true 价格区间
        boolean priceRangeModelFlag = isPriceRangeModel(memberConfig);
        String currentNewPriceStr = (String) db2RedisTemplate.boundHashOps(BinanceConstants.BN_MARKET_PRICE).get(symbol);
        log.debug("品种 {} , 当前最新价格 {} ",symbol,currentNewPriceStr);
        if(StringUtils.isBlank(currentNewPriceStr)) return;
        //获取累计委托+持仓金额
        String AMOUNT_REDIS_KEY = String.format(BinanceConstants.BN_SYMBOL_CONSIGN_AMOUNT,memberId);
        String totalAmount = (String) db2RedisTemplate.boundHashOps(AMOUNT_REDIS_KEY).get(symbol);
        log.debug("memberId {} , 品种 {} ,累计挂单金额 {} ",memberId, symbol,totalAmount);
        //是否需要补仓
        boolean needMakeUpPosition = true;
        if(StringUtils.isNotBlank(totalAmount) && new BigDecimal(totalAmount).compareTo(memberConfig.getPositionMaxNumber()) > -1){
            log.debug("不满足开仓条件, 已达到最大开仓数量.....");
            needMakeUpPosition = false;
        }
        //当前最新价格
        BigDecimal currentNewPrice = new BigDecimal(currentNewPriceStr);
        //开仓价格
        BigDecimal orderPrice = currentNewPrice;
        //是否开仓
        boolean needOrder = true;
        //获取当前所有挂单集合
        List<BinanceOpenOrderResult> openOrderResultList = getOpenOrders(memberConfig);
        //获取挂单价格集合
        String PRICE_REDIS_KEY = String.format(BinanceConstants.BN_SYMBOL_CONSIGN_PRICE,memberId,symbol);
        Set priceSet = db2RedisTemplate.opsForSet().members(PRICE_REDIS_KEY);
        List<BigDecimal> priceList = new ArrayList<>(priceSet);
        if(!CollectionUtils.isEmpty(priceList)){
            if(needMakeUpPosition){ //开仓
                orderPrice = getOrderPrice(priceList,currentNewPrice,memberConfig);
            }else{ //检测是否需要取消旧的挂单
                needOrder = false;
                log.debug("不满足开仓条件, 当前系统累计挂单量 {} , 系统维护最大挂单量 {} ",totalAmount,memberConfig.getPositionMaxNumber());
                orderPrice = getOrderPrice(priceList,currentNewPrice,memberConfig);
                checkOldOrder(orderPrice,memberConfig,openOrderResultList);
            }
        }
        if(needOrder && orderPrice.compareTo(BigDecimal.ZERO) > 0){
            if(priceRangeModelFlag && (memberConfig.getMinMoney().compareTo(orderPrice) > 0 || memberConfig.getMaxMoney().compareTo(orderPrice) < 0)){ //区间范围
                log.debug("当前价格{}不满足价格交易区间[{} - {}]", orderPrice, memberConfig.getMinMoney(), memberConfig.getMaxMoney());
                return;
            }
            //计算单笔下单金额  最大持仓量 /（最大交易价格-最小交易价格）/ 步长
            BigDecimal subtract = memberConfig.getMaxMoney().subtract(memberConfig.getMinMoney());
            BigDecimal openOrderNumber = subtract.divide(memberConfig.getTradeStep(),6, BigDecimal.ROUND_HALF_UP);
            BigDecimal consignAmount = memberConfig.getPositionMaxNumber().divide(openOrderNumber,6, BigDecimal.ROUND_HALF_UP);
            log.debug("当前开仓金额 {} ",consignAmount);
            memberConfig.setExpectedFlag(0);//预开仓订单默认为 0
            String EXPECTED_ORDER_REDIS_KEY = String.format(BinanceConstants.BN_SYMBOL_EXPECTED_ORDER,memberConfig.getMemberId(),memberConfig.getSymbol());
            String openExpectedFlagStr = (String) db2RedisTemplate.opsForValue().get(EXPECTED_ORDER_REDIS_KEY);
            if(memberConfig.getTradeModel().equals(1) && StringUtils.isBlank(openExpectedFlagStr)){ //预开仓
                subtract = memberConfig.getMaxMoney().subtract(orderPrice);
                BigDecimal expectedOpenOrderNumber = subtract.divide(memberConfig.getTradeStep(),6, BigDecimal.ROUND_HALF_UP);
                consignAmount = consignAmount.multiply(expectedOpenOrderNumber);
                memberConfig.setExpectedFlag(1);//预开仓订单
                log.debug("预开仓金额 {} ",consignAmount);
            }
            memberConfig.setConsignAmount(consignAmount);//单笔下单金额
            //单笔下单量
            BigDecimal consignVolume = consignAmount.divide(orderPrice,6, BigDecimal.ROUND_HALF_UP);
            //检测下单数量
            consignVolume = checkTradeVolume(consignVolume);
            memberConfig.setConsignVolume(consignVolume);
            //检查挂单数量
            checkOpenOrderNum(orderPrice,memberConfig,openOrderResultList);
            //建仓
            openTradeOrder(orderPrice,memberConfig);
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

    //检测是否需要取消旧的挂单
    private void checkOldOrder(BigDecimal orderPrice,BinanceMemberConfig memberConfig,List<BinanceOpenOrderResult> openOrderResultList){
        //获取所有挂单
        if(CollectionUtils.isEmpty(openOrderResultList)){
           return;
        }
        BinanceOpenOrderResult minBinanceOpenOrderResult = openOrderResultList.stream().max(Comparator.comparing(BinanceOpenOrderResult::getOrigQty)).get();
        log.debug("最小价格 {} , 订单ID {} , 币安订单ID {} ",minBinanceOpenOrderResult.getPrice(),minBinanceOpenOrderResult.getClientOrderId(),minBinanceOpenOrderResult.getOrderId());
        if(ObjectUtils.isEmpty(minBinanceOpenOrderResult)){
            return;
        }
        if(orderPrice.compareTo(minBinanceOpenOrderResult.getPrice().add(memberConfig.getTradeStep())) < 0){
            log.debug("检测是否需要旧的挂单.......最小挂单价格和当前需要挂单的价格 {} ",orderPrice);
            return;
        }
        doCanceledOrder(minBinanceOpenOrderResult,memberConfig);
    }
    //取消挂单
    private void doCanceledOrder(BinanceOpenOrderResult binanceOpenOrderResult,BinanceMemberConfig memberConfig){
        //取消最小价格的挂单
        String orderId = binanceOpenOrderResult.getOrderId();
        String newClientOrderId = binanceOpenOrderResult.getClientOrderId();
        String symbol = binanceOpenOrderResult.getSymbol();
        String side = binanceOpenOrderResult.getSide();
        BigDecimal cummulativeQuoteQty = binanceOpenOrderResult.getCummulativeQuoteQty();
        if(cummulativeQuoteQty.compareTo(BigDecimal.ZERO) > 0)
            return;
        //获取所有挂单
        JSONObject orderParamJson = BinanceTradeServiceUtils.getDefaultPostData();
        orderParamJson.put("symbol",memberConfig.getSymbol());
        orderParamJson.put("orderId",orderId);
        orderParamJson.put("newClientOrderId",newClientOrderId);
        String revokeOrderParamDataStr = BinanceTradeServiceUtils.getQueryStr(orderParamJson);
        JSONObject resultJson = binanceTradeClientService.revokeOpenOrder(memberConfig.getMemberId(), BinanceConstants.API_ORDER, revokeOrderParamDataStr);
        if(ObjectUtils.isEmpty(resultJson)){
            log.debug("revoke order 撤销订单失败!");
            return;
        }else if(resultJson.containsKey("orderId")){
            //撤销订单结果
            if(resultJson.getString("status").equalsIgnoreCase("CANCELED")){ //撤销成功

                BinanceOrderDo binanceOrder = binanceOrderService.getBinanceOrderByOrderId(newClientOrderId, memberConfig.getMemberId(), 0,
                        symbol, orderId);
                if(ObjectUtils.isEmpty(binanceOrder)){
                    log.debug(".......根据查询的挂单列表，没从系统检测出挂单的订单............");
                    return;
                }
                BinanceOrderDo canceledOrder = new BinanceOrderDo();
                canceledOrder.setUpdateTime(new Date());
                canceledOrder.setPrice(BigDecimal.ZERO);
                canceledOrder.setTradeVolume(BigDecimal.ZERO);
                canceledOrder.setTradeAmount(BigDecimal.ZERO);
                canceledOrder.setOpenFee(BigDecimal.ZERO);
                canceledOrder.setProfit(BigDecimal.ZERO);
                canceledOrder.setStatus(2);
                canceledOrder.setCancelMessage(resultJson.toJSONString());
                canceledOrder.setOrderId(binanceOrder.getOrderId());
                canceledOrder.setStatusTag(0);
                canceledOrder.setMemberId(memberConfig.getMemberId());
                canceledOrder.setSymbol(symbol);
                binanceOrderService.updateBinanceOrderDo(canceledOrder);
                //remove 缓存价格
                removeCacheRedisSymbolPrice(memberConfig.getMemberId(),binanceOrder.getPrice(),binanceOrder.getSymbol());
                //缓存减去累计开仓量
                cacheSubRedisSymbolAmount(memberConfig.getMemberId(),binanceOrder.getConsignAmount(),binanceOrder.getSymbol());
                if(binanceOrder.getExpectedFlag().equals(1)){
                    String REDIS_KEY = String.format(BinanceConstants.BN_SYMBOL_EXPECTED_ORDER,memberConfig.getMemberId(),memberConfig.getSymbol());
                    db2RedisTemplate.delete(REDIS_KEY);
                }
            }
        }
    }
    //检查当前挂单数量
    private void checkOpenOrderNum(BigDecimal orderPrice,BinanceMemberConfig memberConfig,List<BinanceOpenOrderResult> openOrderResultList){
        if(!CollectionUtils.isEmpty(openOrderResultList)){
            List<BinanceOpenOrderResult> buyOpenOrderResult = openOrderResultList.stream().filter(item -> item.getSide().equalsIgnoreCase("BUY")).collect(Collectors.toList());
            log.debug("检查当前挂单数量 {} ",buyOpenOrderResult.size());
            if(buyOpenOrderResult.size() >= 80){ //若等于80
                BinanceOpenOrderResult minBinanceOpenOrderResult = buyOpenOrderResult.stream().min(Comparator.comparing(BinanceOpenOrderResult::getPrice)).get();
                if(orderPrice.compareTo(minBinanceOpenOrderResult.getPrice()) < 0){
                    log.debug(" 当前开仓价格若小于最低挂单价格，取消挂单.... ");
                    return;
                }
                //取消挂单
                doCanceledOrder(minBinanceOpenOrderResult,memberConfig);
            }
        }
    }
    //调用币安API创建订单
    private void openTradeOrder(BigDecimal orderPrice,BinanceMemberConfig memberConfig){
        //生成订单ID
        String orderId = String.format(BinanceCommonUtil.prefixRandomOrderId(),memberConfig.getMemberId());
        BinanceApiSendOrderParam binanceApiSendOrderParam = getBinanceApiSendOrderParam(orderPrice, orderId, memberConfig);
        log.debug(" {} - {} 开仓交易参数 : {}", memberConfig.getMemberId(), memberConfig.getSymbol(), binanceApiSendOrderParam);
        JSONObject postData = BinanceTradeServiceUtils.getDefaultPostData();
        postData.putAll(JSONUtil.parseObj(binanceApiSendOrderParam));
        String sendDataStr = BinanceTradeServiceUtils.getQueryStr(postData);
        JSONObject sendOrderResult = binanceTradeClientService.sendSpotTradeOrder(memberConfig.getMemberId(),BinanceConstants.API_ORDER, sendDataStr);
        log.debug("open order data: {}, result: {}", binanceApiSendOrderParam, sendOrderResult);

        if(ObjectUtils.isEmpty(sendOrderResult)){
            log.debug("open order 订单交易失败!");
            return;
        }else if(sendOrderResult.containsKey("orderId")){
            /**
             * {"symbol":"BTCUSDT","orderId":7157795,"orderListId":-1,"clientOrderId":"CAM88C20230217115955333","transactTime":1676606396412,"price":"10000.00000000",
             * "origQty":"0.01000000","executedQty":"0.00000000","cummulativeQuoteQty":"0.00000000","status":"NEW","timeInForce":"GTC","type":"LIMIT","side":"BUY",
             * "workingTime":1676606396412,"fills":[],"selfTradePreventionMode":"NONE"}
             */
            //保存订单
            saveTradeOrder(orderId,memberConfig,sendOrderResult);
            log.debug("open order 创建订单成功!");
        }
    }
    //生成订单
    private void saveTradeOrder(String orderId, BinanceMemberConfig memberConfig, JSONObject sendOrderResult){
        BinanceOrderDo binanceOrderDo = new BinanceOrderDo();
        binanceOrderDo.setOrderId(orderId);
        binanceOrderDo.setMemberId(memberConfig.getMemberId());
        binanceOrderDo.setSymbol(memberConfig.getSymbol());
        binanceOrderDo.setBnOrderId(sendOrderResult.getString("orderId"));
        binanceOrderDo.setTransactTime(sendOrderResult.getLong("transactTime").toString());
        binanceOrderDo.setSide(sendOrderResult.getString("side"));
        binanceOrderDo.setType(sendOrderResult.getString("type"));
        binanceOrderDo.setConsignVolume(sendOrderResult.getBigDecimal("origQty"));
        binanceOrderDo.setConsignAmount(memberConfig.getConsignAmount());
        binanceOrderDo.setPrice(sendOrderResult.getBigDecimal("price"));
        binanceOrderDo.setStatus(0);
        binanceOrderDo.setCreateTime(new Date());
        binanceOrderDo.setOpenMessage(sendOrderResult.toJSONString());
        binanceOrderDo.setExpectedFlag(memberConfig.getExpectedFlag());
        binanceOrderService.saveBinanceOrderDo(binanceOrderDo);
        //缓存累计挂单金额
        cacheRedisSymbolAmount(memberConfig.getMemberId(),memberConfig.getConsignAmount(),memberConfig.getSymbol().toUpperCase());
        //缓存挂单价格、及持仓的价格
        cacheRedisSymbolPrice(memberConfig.getMemberId(),sendOrderResult.getBigDecimal("price"),memberConfig.getSymbol().toUpperCase());
        //是否为预开仓订单
        if(memberConfig.getExpectedFlag().equals(1)){
            cacheRedisSymbolExpected(memberConfig);
        }

    }
    //是否为预开仓订单
    private void cacheRedisSymbolExpected(BinanceMemberConfig memberConfig){
        String REDIS_KEY = String.format(BinanceConstants.BN_SYMBOL_EXPECTED_ORDER,memberConfig.getMemberId(),memberConfig.getSymbol());
        db2RedisTemplate.opsForValue().set(REDIS_KEY,memberConfig.getExpectedFlag()+"");
    }
    //缓存挂单价格、及持仓的价格
    private void cacheRedisSymbolPrice(Integer memberId,BigDecimal orderPrice,String symbol){
        String REDIS_KEY = String.format(BinanceConstants.BN_SYMBOL_CONSIGN_PRICE,memberId,symbol);
        db2RedisTemplate.opsForSet().add(REDIS_KEY,orderPrice);
    }
    //缓存累计挂单金额
    private void cacheRedisSymbolAmount(Integer memberId,BigDecimal consignAmount,String symbol){
        String REDIS_KEY = String.format(BinanceConstants.BN_SYMBOL_CONSIGN_AMOUNT,memberId);
        String totalAmount = (String) db2RedisTemplate.boundHashOps(REDIS_KEY).get(symbol);
        if(StringUtils.isNotBlank(totalAmount)){//累计成交金额
            consignAmount = new BigDecimal(totalAmount).add(consignAmount);
        }
        db2RedisTemplate.boundHashOps(REDIS_KEY).put(symbol, consignAmount.toString());
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

    //开仓订单参数
    private BinanceApiSendOrderParam getBinanceApiSendOrderParam(BigDecimal orderPrice,String orderId,BinanceMemberConfig memberConfig){
        BinanceApiSendOrderParam binanceApiSendOrderParam = new BinanceApiSendOrderParam();
        binanceApiSendOrderParam.setSymbol(memberConfig.getSymbol());
        binanceApiSendOrderParam.setSide(memberConfig.getTradeType().toUpperCase());
        binanceApiSendOrderParam.setType(BinanceConstants.TRADE_TYPE_LIMIT);
        binanceApiSendOrderParam.setTimeInForce(BinanceConstants.TRADE_TIME_IN_FORCE);
        binanceApiSendOrderParam.setQuantity(memberConfig.getConsignVolume());
        binanceApiSendOrderParam.setPrice(orderPrice);
        binanceApiSendOrderParam.setNewClientOrderId(orderId);
        return binanceApiSendOrderParam;
    }
    //是否为区间模式
    private boolean isPriceRangeModel(BinanceMemberConfig memberConfig){
        boolean priceRangeModelFlag = false;
        if(!ObjectUtils.isEmpty(memberConfig) && !ObjectUtils.isEmpty(memberConfig.getPriceModel()) && memberConfig.getPriceModel().equals(1)){
            priceRangeModelFlag = true;
        }
        return priceRangeModelFlag;
    }
    //获取开仓价格
    public BigDecimal getOrderPrice(List<BigDecimal> priceList,BigDecimal currentNewPrice,BinanceMemberConfig memberConfig){
        Collections.sort(priceList,Collections.reverseOrder());
        BigDecimal maxPrice = Collections.max(priceList);
        BigDecimal tradeStep = memberConfig.getTradeStep();
        if(currentNewPrice.subtract(maxPrice).compareTo(tradeStep) > -1){
            return currentNewPrice;
        }
        BigDecimal resultPrice = BigDecimal.ZERO;
        for(int i=0;i<priceList.size();i++){
            BigDecimal currentPrice = priceList.get(i);
            BigDecimal afterPrice = (i+1 < priceList.size()) ? priceList.get(i+1) : BigDecimal.ZERO;
            if(currentPrice.subtract(afterPrice).compareTo(tradeStep) > -1){
                if(currentPrice.subtract(tradeStep).compareTo(currentNewPrice) > -1 && currentNewPrice.compareTo(afterPrice.add(tradeStep)) > -1){
                    resultPrice = currentNewPrice;
                    break;
                }
                resultPrice = currentPrice.subtract(tradeStep);
                if(currentNewPrice.compareTo(resultPrice) > -1 && resultPrice.subtract(afterPrice).compareTo(tradeStep) > -1)
                    break;
            }else{
                resultPrice = (currentPrice.subtract(tradeStep).compareTo(currentNewPrice) > -1) ? currentNewPrice : currentPrice.subtract(tradeStep);
            }
        }
        return resultPrice;
    }

    //检测下单数量
    private BigDecimal checkTradeVolume(BigDecimal consignVolume){
        BigDecimal stepSize = new BigDecimal("0.00000100");
        BigDecimal divide = consignVolume.divide(stepSize, 0, BigDecimal.ROUND_DOWN);
        BigDecimal tradeVolume = divide.multiply(stepSize);
        return tradeVolume.setScale(5,BigDecimal.ROUND_DOWN);
    }

}
