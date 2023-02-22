package com.binance.quantify.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;

import java.util.Date;

public class BinanceTradeClientUtils {

    public static String getHMacStr(String signInfoStr, String secretKey){
        byte[] key = secretKey.getBytes();
        HMac mac =new HMac(HmacAlgorithm.HmacSHA256, key);
        return mac.digestHex(signInfoStr);
    }

    public static String getCurrentMinuteStr(){
        Date currentTime = new Date();
        return DateUtil.format(currentTime, "yyyyMMddHHmm");
    }

}
