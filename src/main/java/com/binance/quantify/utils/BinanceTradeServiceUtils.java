package com.binance.quantify.utils;

import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public class BinanceTradeServiceUtils {

    public static String getQueryStr(JSONObject signInfo){
        UrlQuery query = new UrlQuery();
        query.addAll(signInfo);
        return query.build(StandardCharsets.UTF_8);
    }

    public static JSONObject getDefaultPostData(){
        JSONObject postData = new JSONObject();
        postData.put("recvWindow", 5000);
        postData.put("timestamp", new Date().getTime());
        return postData;
    }

}
