package com.binance.quantify.utils;

public class BinanceConstants {

    /**
     * redis 缓存 key
     */
    public static final String BN_MARKET_PRICE = "BN_MARKET_PRICE";

    public static final String BN_SYMBOL_CONSIGN_AMOUNT = "BN_SYMBOL_CONSIGN_AMOUNT:%s";

    public static final String BN_SYMBOL_CONSIGN_PRICE = "BN_SYMBOL_CONSIGN_PRICE:%s:%s";

    public static final String BN_SYMBOL_EXPECTED_ORDER = "BN_SYMBOL_EXPECTED_ORDER:%s:%s";

    public static final String SPOT = "spot";

    public static final String TRADE_TYPE_LIMIT = "LIMIT";

    //public static final String TRADE_TYPE_MARKET = "MARKET";

    public static final String TRADE_TIME_IN_FORCE = "GTC";

    /**
     * 用户下单接口 测试
     */
    public static final String API_ORDER_TEST = "/api/v3/order/test";

    /**
     * 用户下单接口
     */
    public static final String API_ORDER = "/api/v3/order";

    /**
     * 当前挂单
     */
    public static final String API_OPEN_ORDERS = "/api/v3/openOrders";

    /**
     * 查询所有订单
     */
    public static final String API_ALL_ORDERS = "/api/v3/allOrders";

    /**
     * 查询账户成交历史
     */
    public static final String API_ALL_TRADES = "/api/v3/myTrades";

}
