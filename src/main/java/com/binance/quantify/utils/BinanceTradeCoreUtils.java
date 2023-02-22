package com.binance.quantify.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import org.springframework.util.StringUtils;

import java.util.Calendar;

public class BinanceTradeCoreUtils {

    private static final byte[] SECRET_KEY_PASS = Base64.decode("qNd3yKqaW7T3rqiTldIA5w==");

    public static String encodeSecretKey(String secretKey) {
        byte[] iv = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue()).getEncoded();
        AES aes = new AES(Mode.CBC, Padding.PKCS5Padding, SECRET_KEY_PASS, iv);
        return String.format("%s_%s", Base64.encode(iv), aes.encryptBase64(secretKey));
    }

    public static String decodeSecretKey(String enSecretKey){
        if(StringUtils.isEmpty(enSecretKey)) return null;
        String[] data = enSecretKey.split("_");
        if(data.length < 2) return null;
        AES aes = new AES(Mode.CBC, Padding.PKCS5Padding, SECRET_KEY_PASS, Base64.decode(data[0]));
        return aes.decryptStr(data[1], CharsetUtil.CHARSET_UTF_8);
    }

    public static String signUploadData(String uploadInfo){
        return SecureUtil.md5(String.format("body=%s&key=W40L4W38NOO0938I338B687TTPPAM4GT", uploadInfo));
    }

    public static String randomVisitCode(Integer randomCodeLength){
        return RandomUtil.randomString(randomCodeLength);
    }

    public static Long getCurrentMinuteTime(){
        DateTime currentTime = DateTime.now();
        currentTime.setField(Calendar.SECOND, 0);
        currentTime.setField(Calendar.MILLISECOND, 0);
        return currentTime.getTime();
    }

}
