# 关于服务器接口加密与解密
- 加密配置需要首先开启服务器加密 ```org.veto.core.common.ServiceConfig```下的SERVICE_API_ENCRYPT为true
- 为保证服务器数据安全，所有需要登录的接口，返回结果为```org.veto.shared.Response```的数据都会经过一层加密
- 加密主要以AES256为标准，用户可自己定义新的加密方法，但需与前端协商好解密规则，具体加密逻辑参考 ```org.veto.api.context.EncryptionResponseAdvice#aes256Encrypt```

当前加密方法前端解密示例 `注意： 不要让前端使用未混肴之前的解密明文js，否则将直接暴露服务器加密解密`
```javascript
/**
 * 封装的解密工具类
 *
 * 该工具类需要 Crypto-js 库支持。
 * 在实际项目中，可以通过 npm install crypto-js 安装，或通过 <script> 标签引入。
 */
const AesDecryptor = {

        /**
         * 根据 token, timestamp 和 nonce 生成 AES256 密钥
         *
         * @param {string} token     - 用户的认证令牌
         * @param {string} timestamp - 请求的时间戳
         * @param {string} nonce     - 请求的唯一随机数
         * @returns {string}         - Base64 编码的密钥字符串
         */
        generateAesKey: function(token, timestamp, nonce) {
            // 1. 获取 token, timestamp, nonce 的 SHA-256 哈希值
            const tokenHash = CryptoJS.SHA256(token);
            const timestampHash = CryptoJS.SHA256(timestamp);
            const nonceHash = CryptoJS.SHA256(nonce);

            // 2. 对三个哈希值进行异或运算
            const combinedHash = CryptoJS.lib.WordArray.create();
            for (let i = 0; i < 8; i++) { // SHA-256 哈希值由8个32位字组成
                combinedHash.words[i] = tokenHash.words[i] ^ timestampHash.words[i] ^ nonceHash.words[i];
            }

            // 3. 返回 Base64 编码的密钥字符串
            return CryptoJS.enc.Base64.stringify(combinedHash);
        },

        /**
         * 使用生成的密钥解密密文
         *
         * @param {string} encryptedBase64Data - Base64 编码的密文
         * @param {string} token             - 认证令牌
         * @param {string} timestamp         - 时间戳
         * @param {string} nonce             - 唯一随机数
         * @returns {string}                 - 解密后的明文
         */
        decrypt: function(encryptedBase64Data, token, timestamp, nonce) {
            try {
                // 1. 生成密钥
                const keyBase64 = this.generateAesKey(token, timestamp, nonce);
                const key = CryptoJS.enc.Base64.parse(keyBase64);

                // 2. 解密
                // 这里的模式和填充方式必须与后端保持一致
                // 示例使用 ECB 模式和 PKCS7 填充（CryptoJS默认）
                const decrypted = CryptoJS.AES.decrypt(
                    encryptedBase64Data,
                    key,
                    {
                        mode: CryptoJS.mode.ECB,
                        padding: CryptoJS.pad.Pkcs7
                    }
                );

                // 3. 将解密后的数据转换为 UTF-8 字符串
                return decrypted.toString(CryptoJS.enc.Utf8);
            } catch (error) {
                console.error("解密失败:", error);
                return null;
            }
        }
    };
```