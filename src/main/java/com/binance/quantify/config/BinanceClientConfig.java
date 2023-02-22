package com.binance.quantify.config;

import com.binance.quantify.utils.BinanceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import javax.annotation.Resource;

@Component
public class BinanceClientConfig {

    @Value("${binance-client.test-spot-api-url}")
    private String BINANCE_SPOT_URL;

    @Resource
    private RestTemplate restTemplate;

    @Bean(name = "binanceSpotClient")
    public BinanceClient getSpotClient(){
        return new BinanceClient(BINANCE_SPOT_URL, restTemplate);
    }

}
