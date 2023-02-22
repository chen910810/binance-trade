package com.binance.quantify.module.member.service;

import com.binance.quantify.module.member.domain.BinanceMember;
import com.binance.quantify.module.member.domain.BinanceMemberConfig;
import com.binance.quantify.module.member.domain.BinanceMemberKey;
import java.util.List;

public interface BinanceMemberService {

    BinanceMember getMemberByUsername(String username);

    BinanceMember findMemberById(Integer memberId);

    BinanceMemberKey getMemberKey(Integer memberId);

    BinanceMemberConfig getTradeConfig(Integer memberId, String symbol);

    List<BinanceMemberConfig> getAutoTradeConfigMember();

    List<BinanceMemberConfig> findAutoMember();

    List<BinanceMemberConfig> getSystemStatus(Integer memberId);

}
