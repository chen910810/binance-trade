package com.binance.quantify.utils;

import cn.hutool.core.util.RandomUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BinanceCommonUtil {

    public static String prefixRandomOrderId(){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String currentTimeStr =  formatter.format(new Date());
        return RandomUtil.randomString(6).toUpperCase() + currentTimeStr;
    }

}
