package com.binance.quantify;

import com.alibaba.fastjson.JSONObject;
import com.binance.quantify.module.member.domain.BinanceMember;
import com.binance.quantify.module.member.domain.BinanceMemberConfig;
import com.binance.quantify.module.member.service.BinanceMemberService;
import com.binance.quantify.module.spot.SystemAutoOpenTradeProcess;
import com.binance.quantify.utils.BinanceTradeCoreUtils;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@SpringBootTest
public class BinanceTradeApplicationTest {

    @Resource
    private BinanceMemberService binanceMemberService;
    @Resource
    private SystemAutoOpenTradeProcess systemAutoOpenTradeProcess;

    @Test
    void contextLoads() {

        String secretKey = BinanceTradeCoreUtils.encodeSecretKey("ogx2O527ZbVubA9jrwKyNQEOqqWhUm4A0ozfscP6NEM729OCPMrPNBDRlqUYE5xh");
        log.debug("{}",secretKey);
        /*List<BigDecimal> setPrice = new ArrayList<>();
        setPrice.add(new BigDecimal("100"));
        setPrice.add(new BigDecimal("95"));
        setPrice.add(new BigDecimal("88"));
        BinanceMemberConfig memberConfig = new BinanceMemberConfig();
        memberConfig.setTradeStep(BigDecimal.valueOf(5));
        BigDecimal orderPrice = systemAutoOpenTradeProcess.getOrderPrice(setPrice, BigDecimal.valueOf(70), memberConfig);
        System.out.println(orderPrice);*/
    }

}
