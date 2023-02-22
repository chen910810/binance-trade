package com.binance.quantify.module.symbol.domain;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class BinanceSymbol implements Serializable {

    private static final long serialVersionUID = -8499606556393883691L;

    private String symbol;

    private String status;

    private Date createTime;
}
