package com.binance.quantify.utils;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Log4j2
public class BinanceClient {

    private String baseUrl;
    private RestTemplate restTemplate;

    private BinanceClient(){}

    public BinanceClient(String baseUrl, RestTemplate restTemplate){
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
    }

    public String getApi(String apiUrl, String apiKey, String secretKey, String apiDataStr){
        return exchangeApi(apiUrl, HttpMethod.GET, apiKey, secretKey, apiDataStr);
    }

    public String postApi(String apiUrl, String apiKey, String secretKey, String apiDataStr){
        return exchangeApi(apiUrl, HttpMethod.POST, apiKey, secretKey, apiDataStr);
    }

    public String deleteApi(String apiUrl, String apiKey, String secretKey, String apiDataStr){
        return exchangeApi(apiUrl, HttpMethod.DELETE, apiKey, secretKey, apiDataStr);
    }

    private String exchangeApi(String apiUrl, HttpMethod method, String apiKey, String secretKey, String apiDataStr){
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MBX-APIKEY", apiKey);
        HttpEntity<String> httpEntity = new HttpEntity<>(null, headers);
        apiDataStr = apiDataStr.replaceAll("%5B", "[").replaceAll("%5D", "]");
        String signDataStr = BinanceTradeClientUtils.getHMacStr(apiDataStr, secretKey);
        String finalApiUrl = String.format("%s%s?%s&signature=%s", this.baseUrl, apiUrl, apiDataStr, signDataStr);
        log.debug("{} api url: {}", method, finalApiUrl);
        try {
            URI finalUri = URI.create(finalApiUrl);
            ResponseEntity<String> serverDataResponse = this.restTemplate.exchange(finalUri, method, httpEntity, String.class);
            return serverDataResponse.getBody();
        }catch (Exception getApiDataError){
            log.error("{} {} api error: {}", method, apiUrl, getApiDataError.getMessage());
        }
        return null;
    }

    public String getPublicApi(String apiUrl, String apiData) {
        String finalApiUrl = String.format("%s%s", this.baseUrl, apiUrl);
        if(StringUtils.hasLength(apiData)){
            finalApiUrl = String.format("%s?%s", finalApiUrl, apiData);
        }
        try {
            ResponseEntity<String> serverDataResponse = this.restTemplate.getForEntity(finalApiUrl, String.class);
            return serverDataResponse.getBody();
        }catch (Exception getApiDataError){
            log.error("get public api {} error: {}", apiUrl, getApiDataError.getMessage());
        }
        return null;
    }
}
