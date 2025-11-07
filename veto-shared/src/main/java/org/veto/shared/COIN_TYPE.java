package org.veto.shared;

// 币种
public enum COIN_TYPE {
    USDC_SOL(BLOCKCHAIN_SYMBOL_NETWORK.SOLANA, BLOCKCHAIN_SYMBOL_NETWORK.TOKEN.USDC, "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v", true, true),
    USDT_SOL(BLOCKCHAIN_SYMBOL_NETWORK.SOLANA, BLOCKCHAIN_SYMBOL_NETWORK.TOKEN.USDT, "Es9vMFrzaCERfH9f3WXg6PvjZKq6yueC3A4w2r6P4vKx", true, true),
    SRM_SOL(BLOCKCHAIN_SYMBOL_NETWORK.SOLANA, BLOCKCHAIN_SYMBOL_NETWORK.TOKEN.SRM, "9p2bVLQXb1vnxJ6VSt1VLvZfSXk7rL7aef3VQp3dUoPU", true, true),
    RAY_SOL(BLOCKCHAIN_SYMBOL_NETWORK.SOLANA, BLOCKCHAIN_SYMBOL_NETWORK.TOKEN.RAY, "4k3Dyjzvzp8e1xXK7yQsw5zxy1n3xTvq4TkgGRb4D7dE", true, true),
    SOL(BLOCKCHAIN_SYMBOL_NETWORK.SOLANA, null, null, false, true),
    USDC_ETH(BLOCKCHAIN_SYMBOL_NETWORK.ETHEREUM, BLOCKCHAIN_SYMBOL_NETWORK.TOKEN.USDC, "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48", true, true),
    // 真实的usdt合约地址
    USDT_ETH(BLOCKCHAIN_SYMBOL_NETWORK.ETHEREUM, BLOCKCHAIN_SYMBOL_NETWORK.TOKEN.USDT, "0xdac17f958d2ee523a2206206994597c13d831ec7", true, true),

//    USDT_ETH(BLOCKCHAIN_SYMBOL_NETWORK.ETHEREUM, BLOCKCHAIN_SYMBOL_NETWORK.TOKEN.USDT, "0x7CB0680cBEEd18D34237A54CB609Dd3668A1cE9A", true, true),
    DAI_ETH(BLOCKCHAIN_SYMBOL_NETWORK.ETHEREUM, BLOCKCHAIN_SYMBOL_NETWORK.TOKEN.DAI, "0x6B175474E89094C44Da98b954EedeAC495271d0F", true, true),
    LINK_ETH(BLOCKCHAIN_SYMBOL_NETWORK.ETHEREUM, BLOCKCHAIN_SYMBOL_NETWORK.TOKEN.LINK, "0x514910771AF9Ca656af840dff83E8264EcF986CA", true, true),
    WBTC_ETH(BLOCKCHAIN_SYMBOL_NETWORK.ETHEREUM, BLOCKCHAIN_SYMBOL_NETWORK.TOKEN.WBTC, "0x2260FAC5E5542a773Aa44fBCfeDf7C193bc2C599", true, true),
    ETH(BLOCKCHAIN_SYMBOL_NETWORK.ETHEREUM, null, null, false, true),
    RMB(null, null, null, false, false),
    USD(null, null, null, false, false),
    ;

    private final BLOCKCHAIN_SYMBOL_NETWORK blockchainSymbolNetwork;

    private final BLOCKCHAIN_SYMBOL_NETWORK.TOKEN token;

    private final String contractAddress;

    private final boolean isToken;

    private final boolean isBlockchain;

    COIN_TYPE(BLOCKCHAIN_SYMBOL_NETWORK blockchainSymbolNetwork, BLOCKCHAIN_SYMBOL_NETWORK.TOKEN token, String contractAddress, boolean isToken, boolean isBlockchain) {
        this.blockchainSymbolNetwork = blockchainSymbolNetwork;
        this.token = token;
        this.contractAddress = contractAddress;
        this.isToken = isToken;
        this.isBlockchain = isBlockchain;
    }

    public BLOCKCHAIN_SYMBOL_NETWORK getBlockchainSymbolNetwork() {
        return blockchainSymbolNetwork;
    }

    public BLOCKCHAIN_SYMBOL_NETWORK.TOKEN getToken() {
        return token;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public boolean isToken() {
        return isToken;
    }

    public boolean isBlockchain() {
        return isBlockchain;
    }

    public static COIN_TYPE auto(String str){
        if (str == null){
            return null;
        }

        for (COIN_TYPE value : COIN_TYPE.values()) {
            if (value.name().equalsIgnoreCase(str)){
                return value;
            }else if (value.getBlockchainSymbolNetwork() != null){
                if (value.getBlockchainSymbolNetwork().name().equalsIgnoreCase(str) || value.getBlockchainSymbolNetwork().getShortName().equalsIgnoreCase(str)) {
                    return value;
                }
            }else if (value.getContractAddress() != null && value.getContractAddress().equalsIgnoreCase(str)){
                return value;
            }
        }

        return null;
    }

    public static COIN_TYPE byName(String str){

        if (str != null){
            str = str.replaceAll("\\s", "").replaceAll("\\p{Punct}", "");
            for (COIN_TYPE value : COIN_TYPE.values()) {
                if (value.name().replace("_", "").equalsIgnoreCase(str)) {
                    return value;
                }
            }
        }

        return null;
    }
}
