package org.veto.shared.wallet;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.veto.shared.COIN_TYPE;

@Setter
@Getter
public  class LocalWallet {
    private String mnemonic;
    private String privateKey; // hex
    private String publicKey;  // hex
    private String address;    // ETH hex address or SOL base58
    private COIN_TYPE chain;
    private String xpub;
}