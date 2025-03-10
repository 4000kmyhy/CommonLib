package com.lib.lib_lyric.utils;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class AESUtils {

    private static final String KEY_ALGORITHM = "AES";
    private static final int SECRET_KEY_LENGTH = 32;
    private static final String DEFAULT_VALUE = "0";
    private static final String CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

    public static String decrypt(String content , String secretKey)
    {
        try {
            byte[] dataByteArray = Base64.decode(content, Base64.NO_WRAP);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(secretKey));
            byte[] result = cipher.doFinal(dataByteArray);
            return new String(result, StandardCharsets.UTF_8);

        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }

        return "";
    }

    private static SecretKeySpec getSecretKey(String secretKey)
    {
        String key = toMakeKey(secretKey, SECRET_KEY_LENGTH, DEFAULT_VALUE);
        return new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), KEY_ALGORITHM);
    }

    private static String toMakeKey(String secretKey, int length, String text) {
        int secretKeyLength = secretKey.length();
        String value;
        if (secretKeyLength < length) {
            StringBuilder builder = new StringBuilder();
            builder.append(secretKey);
            int var5 = 0;

            for(int var6 = length - secretKeyLength; var5 < var6; ++var5) {
                builder.append(text);
            }

            value = builder.toString();
        } else if (secretKeyLength > length) {
            value = secretKey.substring(0, length);
        } else {
            value = secretKey;
        }

        return value;
    }

}
