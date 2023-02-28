package com.binance.quantify.module.job;

import com.binance.quantify.module.member.domain.BinanceMemberConfig;
import com.binance.quantify.module.member.service.BinanceMemberService;
import com.binance.quantify.module.spot.SystemAutoOpenTradeProcess;
import com.binance.quantify.module.spot.SystemAutoPositionOrderProcess;
import com.binance.quantify.module.spot.SystemAutoSellOrderProcess;
import com.binance.quantify.module.symbol.service.BinanceSymbolService;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Log4j2
public class SystemTradeJob {

    @Resource
    private BinanceSymbolService binanceSymbolService;
    @Resource
    private BinanceMemberService binanceMemberService;
    @Resource
    private SystemAutoOpenTradeProcess systemAutoOpenTradeProcess;
    @Resource
    private SystemAutoPositionOrderProcess systemAutoPositionOrderProcess;
    @Resource
    private SystemAutoSellOrderProcess systemAutoSellOrderProcess;

    @Scheduled(cron = "* * * * * ?")
    public void binanceSymbolPriceJob(){
        log.debug("------获取品种价格开始------");
        List<String> binanceSymbol = binanceSymbolService.getBinanceSymbol();
        if(CollectionUtils.isEmpty(binanceSymbol)){
            return;
        }
        binanceSymbol.forEach(symbol->binanceSymbolService.cacheRedisSymbolPrice(symbol));
    }

    @Scheduled(cron = "* * * * * ?")
    public void getAutoOrderInfo(){
        log.debug("------系统自动检测开仓定时启动------");
        List<BinanceMemberConfig> autoTradeInfo = binanceMemberService.getAutoTradeConfigMember();
        if(CollectionUtils.isEmpty(autoTradeInfo)){
            return;
        }
        List<BinanceMemberConfig> activeMember = autoTradeInfo.stream().filter(item -> item.getIsActive() == 1).collect(Collectors.toList());
        activeMember.forEach(memberConfig->systemAutoOpenTradeProcess.processActiveMemberOpenJob(memberConfig));
    }

    @Scheduled(cron = "* * * * * ?")
    public void processActivePositionOrderJob(){
        log.debug("------系统自动检测成交定时启动------");
        List<BinanceMemberConfig> autoTradeInfo = binanceMemberService.getAutoTradeConfigMember();
        if(CollectionUtils.isEmpty(autoTradeInfo)){
            return;
        }
        List<BinanceMemberConfig> activeMember = autoTradeInfo.stream().filter(item -> item.getIsPosition() == 1).collect(Collectors.toList());
        activeMember.forEach(memberConfig->systemAutoPositionOrderProcess.processActivePositionOrderJob(memberConfig));
    }

    @Scheduled(cron = "* * * * * ?")
    public void processActiveSellOrderJob(){
        log.debug("------系统自动检测卖单定时启动------");
        List<BinanceMemberConfig> autoTradeInfo = binanceMemberService.getAutoTradeConfigMember();
        if(CollectionUtils.isEmpty(autoTradeInfo)){
            return;
        }
        List<BinanceMemberConfig> activeMember = autoTradeInfo.stream().filter(item -> item.getIsPosition() == 1).collect(Collectors.toList());
        activeMember.forEach(memberConfig->systemAutoSellOrderProcess.processActiveSellOrderJob(memberConfig));
    }

}
