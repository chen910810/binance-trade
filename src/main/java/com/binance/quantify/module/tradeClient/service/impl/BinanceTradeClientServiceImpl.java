package com.binance.quantify.module.tradeClient.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.binance.quantify.module.member.domain.BinanceMember;
import com.binance.quantify.module.member.domain.BinanceMemberKey;
import com.binance.quantify.module.member.service.BinanceMemberService;
import com.binance.quantify.module.spot.result.BinanceOpenOrderResult;
import com.binance.quantify.module.tradeClient.dao.BinanceTradeClientDao;
import com.binance.quantify.module.tradeClient.service.BinanceTradeClientService;
import com.binance.quantify.utils.BinanceClient;
import com.binance.quantify.utils.BinanceConstants;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Log4j2
@Service
public class BinanceTradeClientServiceImpl implements BinanceTradeClientService {

    @Resource
    private BinanceClient binanceSpotClient;
    @Resource
    private BinanceTradeClientDao binanceTradeClientdao;
    @Resource
    private BinanceMemberService binanceMemberService;

    @Override
    public JSONObject sendSpotTradeOrder(Integer memberId,String apiUrl, String apiDataStr) {
        BinanceMemberKey memberKey = getMemberKey(memberId);
        if(ObjectUtils.isEmpty(memberKey)) return null;
        String apiResult = binanceSpotClient.postApi(apiUrl, memberKey.getAccessKey(), memberKey.getSecretKey(), apiDataStr);
        if(StringUtils.hasLength(apiResult) && apiResult.startsWith("{")){
            JSONObject apiResultTemp = JSONObject.parseObject(apiResult);
            if(apiResultTemp.containsKey("code") && apiResultTemp.containsKey("msg")) {
                log.debug("open order msg {} ",String.format("%s-%s", apiResultTemp.getString("code"), apiResultTemp.getString("msg")));
                saveApiErrorResult(memberId, BinanceConstants.SPOT, apiUrl, "POST", apiDataStr, apiResultTemp);
                return null;
            }
        }
        return JSONObject.parseObject(apiResult);
    }

    @Override
    public JSONObject getOpenOrder(Integer memberId, String apiUrl, String apiDataStr) {
        BinanceMemberKey memberKey = getMemberKey(memberId);
        if(ObjectUtils.isEmpty(memberKey)) return null;
        String apiResult = binanceSpotClient.getApi(apiUrl, memberKey.getAccessKey(), memberKey.getSecretKey(), apiDataStr);
        log.debug("get order data: {}, result: {}", apiDataStr,apiResult);
        if(StringUtils.hasLength(apiResult) && apiResult.startsWith("{")){
            JSONObject apiResultTemp = JSONObject.parseObject(apiResult);
            if(apiResultTemp.containsKey("code") && apiResultTemp.containsKey("msg")) {
                log.debug("open order msg {} ",String.format("%s-%s", apiResultTemp.getString("code"), apiResultTemp.getString("msg")));
                saveApiErrorResult(memberId, BinanceConstants.SPOT, apiUrl, "GET", apiDataStr, apiResultTemp);
                return null;
            }
        }
        return JSONObject.parseObject(apiResult);
    }

    @Override
    public List<BinanceOpenOrderResult> getOpenOrders(Integer memberId, String apiUrl, String apiDataStr) {
        BinanceMemberKey memberKey = getMemberKey(memberId);
        if(ObjectUtils.isEmpty(memberKey)) return null;
        String apiResult = binanceSpotClient.getApi(apiUrl, memberKey.getAccessKey(), memberKey.getSecretKey(), apiDataStr);
        log.debug("get open order data: {}, result: {}", apiDataStr,apiResult);
        if(apiResult.startsWith("[")){
            return JSONArray.parseArray(apiResult).toJavaList(BinanceOpenOrderResult.class);
        }else if(apiResult.startsWith("{")){
            JSONObject errorInfo = JSONObject.parseObject(apiResult);
            if(errorInfo.containsKey("code") && errorInfo.containsKey("msg")){
                saveApiErrorResult(memberId, BinanceConstants.SPOT, apiUrl, "GET", apiDataStr, errorInfo);
            }
        }
        return null;
    }

    @Override
    public JSONObject revokeOpenOrder(Integer memberId, String apiUrl, String apiDataStr) {
        BinanceMemberKey memberKey = getMemberKey(memberId);
        if(ObjectUtils.isEmpty(memberKey)) return null;
        String apiResult = binanceSpotClient.deleteApi(apiUrl,memberKey.getAccessKey(), memberKey.getSecretKey(), apiDataStr);
        if(StringUtils.hasLength(apiResult) && apiResult.startsWith("{")){
            JSONObject apiResultTemp = JSONObject.parseObject(apiResult);
            if(apiResultTemp.containsKey("code") && apiResultTemp.containsKey("msg")) {
                log.debug("revoke order msg {} ",String.format("%s-%s", apiResultTemp.getString("code"), apiResultTemp.getString("msg")));
                saveApiErrorResult(memberId, BinanceConstants.SPOT, apiUrl, "DELETE", apiDataStr, apiResultTemp);
                return null;
            }
        }
        return JSONObject.parseObject(apiResult);
    }

    private BinanceMemberKey getMemberKey(Integer memberId){
        BinanceMemberKey memberKey = binanceMemberService.getMemberKey(memberId);
        if(ObjectUtils.isEmpty(memberKey) || memberKey.getCode() == null){
            log.debug("用户id {} , accessKey secretKey is null", memberId);
            return null;
        }
        return memberKey;
    }

    private void saveApiErrorResult(Integer memberId, String marketType, String apiUrl, String apiMethod, String apiData, JSONObject apiResult){
        if(apiResult.getInteger("code").equals(200)) return;
        binanceTradeClientdao.saveApiErrorLog(memberId, null, marketType, apiUrl, apiMethod, apiData, apiResult.toJSONString(), null, new Date().getTime());
    }

}
