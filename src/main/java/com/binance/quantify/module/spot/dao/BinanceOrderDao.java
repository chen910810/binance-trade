package com.binance.quantify.module.spot.dao;

import com.binance.quantify.module.spot.domain.BinanceOrderDo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface BinanceOrderDao {

    @Insert("INSERT INTO `bo_order`" +
            "(`order_id`, `member_id`, `symbol`, `bn_order_id`, `transactTime`, `side`, `type`, `consign_volume`, `consign_amount`, " +
            " `price`, `status`, `create_time`, `open_message`, `expected_flag`) " +
            "VALUES " +
            "(#{orderId}, #{memberId}, #{symbol}, #{bnOrderId}, #{transactTime}, #{side}, #{type}, #{consignVolume}, #{consignAmount}, " +
            " #{price}, #{status}, #{createTime}, #{openMessage}, #{expectedFlag})")
    void saveBinanceOrderDo(BinanceOrderDo binanceOrderDo);

    @Update("<script>" +
            "update bo_order set " +
            "<if test=\"profit != null \">" +
            " profit = #{profit} ," +
            "</if>" +
            "<if test=\"tradeVolume != null \">" +
            " trade_volume = #{tradeVolume} ," +
            "</if>" +
            "<if test=\"tradeAmount != null \">" +
            " trade_amount = #{tradeAmount} ," +
            "</if>" +
            "<if test=\"tradeDate != null \">" +
            " trade_date = #{tradeDate} ," +
            "</if>" +
            "<if test=\"tradeAvgPrice != null \">" +
            " trade_avg_price = #{tradeAvgPrice} ," +
            "</if>" +
            "<if test=\"status != null \">" +
            " status = #{status} ," +
            "</if>" +
            "<if test=\"returnStatus != null \">" +
            " return_status = #{returnStatus} ," +
            "</if>" +
            "<if test=\"openFee != null \">" +
            " open_fee = #{openFee} ," +
            "</if>" +
            "<if test=\"positionOrderId != null \">" +
            " position_order_id = #{positionOrderId} ," +
            "</if>" +
            "<if test=\"closePrice != null \">" +
            " close_price = #{closePrice} ," +
            "</if>" +
            "<if test=\"closeAvgPrice != null \">" +
            " close_avg_price = #{closeAvgPrice} ," +
            "</if>" +
            "<if test=\"closeDate != null \">" +
            " close_date = #{closeDate} ," +
            "</if>" +
            "<if test=\"closeOrderId != null \">" +
            " close_order_id = #{closeOrderId} ," +
            "</if>" +
            "<if test=\"closeFee != null \">" +
            " close_fee = #{closeFee} ," +
            "</if>" +
            "<if test=\"closeMessage != null \">" +
            " close_message = #{closeMessage} ," +
            "</if>" +
            "<if test=\"updateTime != null \">" +
            " update_time = #{updateTime} " +
            "</if>" +
            " where `order_id` = #{orderId} AND member_id = #{memberId} AND symbol = #{symbol} AND `status` = #{statusTag}" +
            "</script>")
    void updateBinanceOrderDo(BinanceOrderDo binanceOrderDo);

    @Select("SELECT * FROM bo_order WHERE order_id = #{orderId} AND member_id = #{memberId} AND `status` = #{status} AND " +
            "symbol = #{symbol} AND side = #{side} AND bn_order_id = #{bnOrderId}")
    BinanceOrderDo getBinanceOrderByOrderId(String orderId,Integer memberId,Integer status,String symbol,String side,String bnOrderId);

    @Select("SELECT * FROM bo_order WHERE member_id = #{memberId} AND symbol = #{symbol} AND `status` = #{status} AND side = #{side} AND price >= #{price} ORDER BY price DESC LIMIT 1 ")
    BinanceOrderDo getBinanceOrderByPrice(Integer memberId, String symbol, String side, BigDecimal price,Integer status);

}
