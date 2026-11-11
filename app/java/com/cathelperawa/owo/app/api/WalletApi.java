package com.cathelperawa.owo.app.api;

import com.cathelperawa.owo.app.bridge.TimWallet;

import java.util.Map;

public final class WalletApi {

    private WalletApi() {}

    public static String encrypt(String uin, String salt, int randomInt,
                                 String formString, String psKey, String hexGuid) {
        return TimWallet.encrypt(uin, salt, randomInt, formString, psKey, hexGuid);
    }

    public static String decrypt(String uin, String cgiName, int randomInt, String encStr) {
        return TimWallet.decrypt(uin, cgiName, randomInt, encStr);
    }

    public static Map<String, String> process(String url, String secCmd, Map<String, String> form) {
        return TimWallet.walletEncrypt(url, secCmd, form);
    }

    public static String psKey(String url) {
        return TimWallet.walletPsKey(url);
    }

    public static String formString(Map<String, String> map) {
        return TimWallet.formString(map);
    }

    public static byte[] add4Byte(byte[] body) {
        return TimWallet.add4Byte(body);
    }
}
