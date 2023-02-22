package com.binance.quantify.module.member.service.impl;

import com.binance.quantify.module.member.dao.BinanceMemberDao;
import com.binance.quantify.module.member.domain.BinanceMember;
import com.binance.quantify.module.member.domain.BinanceMemberConfig;
import com.binance.quantify.module.member.domain.BinanceMemberKey;
import com.binance.quantify.module.member.service.BinanceMemberService;
import com.binance.quantify.utils.BinanceTradeCoreUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import javax.annotation.Resource;
import java.util.List;

@Service
public class BinanceMemberServiceImpl implements BinanceMemberService {

    @Resource
    private BinanceMemberDao binanceMemberDao;

    @Override
    public BinanceMember getMemberByUsername(String username) {
        return binanceMemberDao.getMemberByUsername(username);
    }

    @Override
    public BinanceMember findMemberById(Integer memberId) {
        return binanceMemberDao.findMemberById(memberId);
    }

    @Override
    @Cacheable(value = "memberKey", key = "'memberKey_' + #memberId")
    public BinanceMemberKey getMemberKey(Integer memberId) {
        return getMemberKeyInfo(memberId);
    }

    private BinanceMemberKey getMemberKeyInfo(Integer memberId){
        BinanceMemberKey memberKey = binanceMemberDao.findMemberKey(memberId);
        if(ObjectUtils.isEmpty(memberKey)){
            return new BinanceMemberKey();
        }
        memberKey.setCode(1);
        return this.decryptMemberKey(memberKey);
    }

    private BinanceMemberKey decryptMemberKey(BinanceMemberKey memberKey){
        String secretKey = BinanceTradeCoreUtils.decodeSecretKey(memberKey.getSecretKey());
        memberKey.setSecretKey(secretKey);
        return memberKey;
    }

    @Override
    public BinanceMemberConfig getTradeConfig(Integer memberId, String symbol) {
        return binanceMemberDao.getTradeConfig(memberId, symbol);
    }

    @Override
    @Cacheable(value = "binanceMemberConfig", key = "'AutoTradeConfigMember'")
    public List<BinanceMemberConfig> getAutoTradeConfigMember() {
        return binanceMemberDao.findAutoTradeMember();
    }

    @Override
    public List<BinanceMemberConfig> findAutoMember() {
        return binanceMemberDao.findAutoMember();
    }

    @Override
    public List<BinanceMemberConfig> getSystemStatus(Integer memberId) {
        return binanceMemberDao.getSystemStatus(memberId);
    }
}
