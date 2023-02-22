package com.binance.quantify.module.member.dao;

import com.binance.quantify.module.member.domain.BinanceMember;
import com.binance.quantify.module.member.domain.BinanceMemberConfig;
import com.binance.quantify.module.member.domain.BinanceMemberKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface BinanceMemberDao {

    @Select("select * from bo_member where username = #{username} limit 0, 1")
    BinanceMember getMemberByUsername(String username);

    @Select("select * from bo_member where id = #{memberId}")
    BinanceMember findMemberById(Integer memberId);

    @Select("select * from bo_member_key where member_id = #{memberId} limit 0, 1")
    BinanceMemberKey findMemberKey(Integer memberId);

    @Select("select * from bo_member_config where member_id = #{memberId} and symbol = #{symbol} limit 0, 1")
    BinanceMemberConfig getTradeConfig(Integer memberId, String symbol);

    @Select("select * from bo_member_config where is_active = 1")
    List<BinanceMemberConfig> findAutoTradeMember();

    @Select("select * from bo_member_config")
    List<BinanceMemberConfig> findAutoMember();

    @Select("select * FROM `bo_member_config` WHERE member_id = #{memberId}")
    List<BinanceMemberConfig> getSystemStatus(Integer memberId);

}
