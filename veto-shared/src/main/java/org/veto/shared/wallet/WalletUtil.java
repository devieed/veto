package org.veto.shared.wallet;

import lombok.Data;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.DeterministicSeed;
import org.veto.shared.COIN_TYPE;
import org.web3j.crypto.*;
import org.web3j.utils.Numeric;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

public class WalletUtil {
    private static final String MNEMONIC_PASSPHRASE = "";
    private static final int HARDENED_BIT = 0x80000000;

    // ========== 对外统一接口 ==========
    public LocalWallet generate(COIN_TYPE type) {
        return switch (type.getBlockchainSymbolNetwork()) {
            case ETHEREUM -> generateETHWallet();
            case SOLANA -> generateSOLWallet();
            default -> null;
        };
    }

    /**
     * 生成派生地址（userId 映射到 index；index = (int)(userId % Integer.MAX_VALUE)）
     * ETH: 使用路径 m/44'/60'/0'/0/index
     * SOL: 使用 SLIP-0010 派生 m/44'/501'/0'/index' （index 硬化）
     */
    public String generateDerivedAddress(COIN_TYPE type, String mnemonic, Long userId) {
        int index = 0;
        if (userId != null) {
            index = (int) (userId % Integer.MAX_VALUE);
            if (index < 0) index = -index;
        }

        return switch (type.getBlockchainSymbolNetwork()) {
            case ETHEREUM -> generateEthDerivedAddress(mnemonic, index);
            case SOLANA -> generateSolDerivedAddress(mnemonic, index);
            default -> null;
        };
    }

    // ========== ETH ==========
    private LocalWallet generateETHWallet() {
        String mnemonic = generateMnemonic();
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, MNEMONIC_PASSPHRASE);
        Bip32ECKeyPair master = Bip32ECKeyPair.generateKeyPair(seed);

        // 标准 ETH 路径 m/44'/60'/0'/0/0
        final int[] path = {
                44 | Bip32ECKeyPair.HARDENED_BIT,
                60 | Bip32ECKeyPair.HARDENED_BIT,
                0  | Bip32ECKeyPair.HARDENED_BIT,
                0,
                0
        };
        Bip32ECKeyPair derived = Bip32ECKeyPair.deriveKeyPair(master, path);
        Credentials credentials = Credentials.create(derived);

        LocalWallet wallet = new LocalWallet();
        wallet.setMnemonic(mnemonic);
        wallet.setPrivateKey(Numeric.toHexStringNoPrefix(credentials.getEcKeyPair().getPrivateKey()));
        wallet.setPublicKey(Numeric.toHexStringNoPrefix(credentials.getEcKeyPair().getPublicKey()));
        wallet.setAddress(credentials.getAddress());
        wallet.setChain(COIN_TYPE.ETH);
        try {
            wallet.setXpub(getXPubFromMnemonic(mnemonic, MNEMONIC_PASSPHRASE, false, COIN_TYPE.ETH));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return wallet;
    }

    private String generateEthDerivedAddress(String mnemonic, int index) {
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, MNEMONIC_PASSPHRASE);
        Bip32ECKeyPair master = Bip32ECKeyPair.generateKeyPair(seed);

        final int[] path = {
                44 | Bip32ECKeyPair.HARDENED_BIT,
                60 | Bip32ECKeyPair.HARDENED_BIT,
                0  | Bip32ECKeyPair.HARDENED_BIT,
                0,
                index
        };
        Bip32ECKeyPair derived = Bip32ECKeyPair.deriveKeyPair(master, path);
        Credentials credentials = Credentials.create(derived);

        BigInteger privateKey = derived.getPrivateKey();

        // 3. 转换为十六进制字符串（MetaMask等工具需要的格式，不带 "0x" 前缀）
        String privateKeyHex = Numeric.toHexStringNoPrefix(privateKey);

        System.out.println(privateKeyHex);

        return credentials.getAddress();
    }

    // ========== SOL (SLIP-0010 + Ed25519) ==========
    private LocalWallet generateSOLWallet() {
        String mnemonic = generateMnemonic();
        return fromSolMnemonic(mnemonic);
    }

    private LocalWallet fromSolMnemonic(String mnemonic) {
        // 使用 web3j 的 BIP39 -> seed（PBKDF2）
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, MNEMONIC_PASSPHRASE);

        // 使用 SLIP-0010 (ed25519) 从 seed 派生默认路径 m/44'/501'/0'/0'
        int[] path = new int[] {44, 501, 0, 0};
        Slip10.Slip10Key node = Slip10.derivePath(seed, path); // 每一层内部会做 hardened

        byte[] privSeed = node.getKey(); // 32 bytes (IL)
        // 用 BouncyCastle 构造 ed25519 私钥
        Ed25519PrivateKeyParameters priv = new Ed25519PrivateKeyParameters(privSeed, 0);
        Ed25519PublicKeyParameters pub = priv.generatePublicKey();

        LocalWallet wallet = new LocalWallet();
        wallet.setMnemonic(mnemonic);
        wallet.setPrivateKey(bytesToHex(priv.getEncoded()));
        wallet.setPublicKey(bytesToHex(pub.getEncoded()));
        wallet.setAddress(Base58.encode(pub.getEncoded())); // Solana address = base58(pubkey)
        wallet.setChain(COIN_TYPE.SOL);
        return wallet;
    }

    private String generateSolDerivedAddress(String mnemonic, int index) {
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, MNEMONIC_PASSPHRASE);

        // 标准（广泛使用的）Solana 路径： m/44'/501'/0'/index'  （注意 index 也 hardened）
        int[] path = new int[] {44, 501, 0, index};
        Slip10.Slip10Key node = Slip10.derivePath(seed, path); // 内部全部 hardened

        byte[] privSeed = node.getKey(); // 32 bytes
        Ed25519PrivateKeyParameters priv = new Ed25519PrivateKeyParameters(privSeed, 0);
        Ed25519PublicKeyParameters pub = priv.generatePublicKey();

        return Base58.encode(pub.getEncoded());
    }

    // ========== 公共/辅助方法 ==========
    private String generateMnemonic() {
        byte[] entropy = new byte[16]; // 128-bit -> 12 words
        new SecureRandom().nextBytes(entropy);
        return MnemonicUtils.generateMnemonic(entropy);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    // ========== SLIP-0010 for Ed25519 (minimal implementation) ==========
    private static final class Slip10 {
        private static final String HMAC_KEY = "ed25519 seed";

        static final class Slip10Key {
            private final byte[] key;       // IL (32)
            private final byte[] chainCode; // IR (32)
            Slip10Key(byte[] key, byte[] chainCode) {
                this.key = key;
                this.chainCode = chainCode;
            }
            byte[] getKey() { return key; }
            byte[] getChainCode() { return chainCode; }
        }

        // master node from seed
        static Slip10Key deriveMasterKey(byte[] seed) {
            byte[] I = hmacSha512(HMAC_KEY.getBytes(StandardCharsets.UTF_8), seed);
            byte[] IL = Arrays.copyOfRange(I, 0, 32);
            byte[] IR = Arrays.copyOfRange(I, 32, 64);
            return new Slip10Key(IL, IR);
        }

        // hardened child derivation (SLIP-0010 ed25519 requires only hardened)
        static Slip10Key deriveChildHardened(Slip10Key parent, int index) {
            // index should be non-negative and < 2^31. We'll set hardened by ORing with HARDENED_BIT
            int idx = index | HARDENED_BIT;
            byte[] data = new byte[1 + parent.key.length + 4];
            data[0] = 0x00;
            System.arraycopy(parent.key, 0, data, 1, parent.key.length);
            byte[] idxBytes = ByteBuffer.allocate(4).putInt(idx).array();
            System.arraycopy(idxBytes, 0, data, 1 + parent.key.length, 4);

            byte[] I = hmacSha512(parent.chainCode, data);
            byte[] IL = Arrays.copyOfRange(I, 0, 32);
            byte[] IR = Arrays.copyOfRange(I, 32, 64);
            return new Slip10Key(IL, IR);
        }

        // derive path: each element in path[] is treated as non-hardened index and will be hardened internally
        static Slip10Key derivePath(byte[] seed, int[] path) {
            Slip10Key node = deriveMasterKey(seed);
            for (int p : path) {
                node = deriveChildHardened(node, p);
            }
            return node;
        }

        private static byte[] hmacSha512(byte[] key, byte[] data) {
            try {
                Mac mac = Mac.getInstance("HmacSHA512");
                mac.init(new SecretKeySpec(key, "HmacSHA512"));
                return mac.doFinal(data);
            } catch (Exception e) {
                throw new RuntimeException("HmacSHA512 error", e);
            }
        }
    }

    // ========== Base58 (简洁实现，用于 Solana 地址) ==========
    private static final char[] ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    public static final class Base58 {
        public static String encode(byte[] input) {
            if (input.length == 0) return "";
            // count leading zeros
            int zeros = 0;
            while (zeros < input.length && input[zeros] == 0) zeros++;
            // convert
            byte[] copy = Arrays.copyOf(input, input.length);
            StringBuilder sb = new StringBuilder();
            BigInteger bi = new BigInteger(1, copy);
            BigInteger base = BigInteger.valueOf(58);
            while (bi.compareTo(BigInteger.ZERO) > 0) {
                BigInteger[] divmod = bi.divideAndRemainder(base);
                bi = divmod[0];
                int digit = divmod[1].intValue();
                sb.append(ALPHABET[digit]);
            }
            // leading zeros
            for (int i = 0; i < zeros; i++) sb.append(ALPHABET[0]);
            return sb.reverse().toString();
        }
    }

    public static String getXPubFromMnemonic(String mnemonic, String passphrase, boolean isTestNet, COIN_TYPE type) throws Exception {
        NetworkParameters params = isTestNet ? TestNet3Params.get() : MainNetParams.get();

        // 1. 生成种子
        DeterministicSeed seed = new DeterministicSeed(mnemonic, null, passphrase, 0L);
        byte[] seedBytes = seed.getSeedBytes();
        if (seedBytes == null) throw new Exception("无效种子");

        // 2. 生成根密钥
        DeterministicKey rootKey = HDKeyDerivation.createMasterPrivateKey(seedBytes);
        DeterministicHierarchy hierarchy = new DeterministicHierarchy(rootKey);

        // 3. BIP44 路径 m / 44' / coinType' / 0'
        ChildNumber purpose = new ChildNumber(44, true);
        int coinType;
        switch (type) {
            case ETH -> coinType = 60;
            case SOL -> coinType = 501;
            default -> throw new IllegalArgumentException("不支持的币种");
        }

        ChildNumber cType = new ChildNumber(coinType, true);
        ChildNumber account = new ChildNumber(0, true);
        List<ChildNumber> path = List.of(purpose, cType, account);

        DeterministicKey accountKey = hierarchy.get(path, true, true);

        // 4. 返回 xPub
        return accountKey.serializePubB58(params);
    }

    public static void main(String[] args) {
        WalletUtil walletUtil = new WalletUtil();

        walletUtil.generateDerivedAddress(COIN_TYPE.USDT_ETH, "cup venue flip sort wild climb film ladder demand scorpion welcome pottery", 103287835435601920L);
    }
}
