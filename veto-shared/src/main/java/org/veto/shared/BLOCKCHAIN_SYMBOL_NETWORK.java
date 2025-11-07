package org.veto.shared;

import java.util.HashMap;
import java.util.Map;

public enum BLOCKCHAIN_SYMBOL_NETWORK {
    SOLANA("SOLANA", "SOL", "SPL-TOKEN"),
    ETHEREUM("ETHEREUM", "ETH", "ERC20"),
    BITCOIN("BITCOIN", "BTC", "BTC"),
    ;

    private final String symbol;

    private final String shortName;

    private final String network;

    BLOCKCHAIN_SYMBOL_NETWORK(String symbol, String shortName, String network) {
        this.symbol = symbol;
        this.shortName = shortName;
        this.network = network;
    }

    public String getNetwork() {
        return network;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getShortName() {
        return shortName;
    }

    public static BLOCKCHAIN_SYMBOL_NETWORK fromString(String value) {
        if (value == null){
            return null;
        }
        for (BLOCKCHAIN_SYMBOL_NETWORK blockchainSymbolNetwork : BLOCKCHAIN_SYMBOL_NETWORK.values()) {
            if (blockchainSymbolNetwork.name().equalsIgnoreCase(value)) {
                return  blockchainSymbolNetwork;
            }else if (blockchainSymbolNetwork.shortName.equalsIgnoreCase(value)) {
                return   blockchainSymbolNetwork;
            }else if (blockchainSymbolNetwork.getSymbol().equalsIgnoreCase(value)) {
                return   blockchainSymbolNetwork;
            }else if (blockchainSymbolNetwork.getNetwork().equalsIgnoreCase(value)) {
                return  blockchainSymbolNetwork;
            }
        }

        return null;
    }

    public enum TOKEN{
        USDC,
        USDT,
        DAI,
        WBTC,
        LINK,
        SRM,
        RAY
    }
}
