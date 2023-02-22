package com.binance.quantify.api;

import com.binance.quantify.module.member.domain.BinanceMemberConfig;
import com.binance.quantify.module.member.service.BinanceMemberService;
import com.binance.quantify.module.spot.SystemAutoOpenTradeProcess;
import com.binance.quantify.module.spot.SystemAutoPositionOrderProcess;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RestController
@RequestMapping("/api/sos")
public class ApiSosController {

    @Resource
    private BinanceMemberService binanceMemberService;
    @Resource
    private SystemAutoOpenTradeProcess systemAutoOpenTradeProcess;
    @Resource
    private SystemAutoPositionOrderProcess systemAutoPositionOrderProcess;

    @RequestMapping(value = "/check", method = RequestMethod.GET)
    public Object check(){
        return "success";
    }

    @RequestMapping(value = "/test/order", method = RequestMethod.GET)
    public Object testOrder(){

        List<BinanceMemberConfig> autoTradeInfo = binanceMemberService.getAutoTradeConfigMember();
        if(CollectionUtils.isEmpty(autoTradeInfo)){
            return "error";
        }
        List<BinanceMemberConfig> activeMember = autoTradeInfo.stream().filter(item -> item.getIsActive() == 1 && item.getProfitModel() == 1).collect(Collectors.toList());
        activeMember.forEach(memberConfig->systemAutoOpenTradeProcess.processActiveMemberOpenJob(memberConfig));

        return "success";
    }

    @RequestMapping(value = "/position/order", method = RequestMethod.GET)
    public Object positionOrder(){
        List<BinanceMemberConfig> autoTradeInfo = binanceMemberService.getAutoTradeConfigMember();
        if(CollectionUtils.isEmpty(autoTradeInfo)){
            return "error";
        }
        List<BinanceMemberConfig> activeMember = autoTradeInfo.stream().filter(item -> item.getIsActive() == 1 && item.getProfitModel() == 1).collect(Collectors.toList());
        activeMember.forEach(memberConfig->systemAutoPositionOrderProcess.processActivePositionOrderJob(memberConfig));
        return "success";
    }

}
