package com.binance.quantify.module.symbol.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface BinanceSymbolDao {

    @Select("SELECT symbol FROM bo_symbol WHERE `status` = 1")
    List<String> getBinanceSymbol();

}
