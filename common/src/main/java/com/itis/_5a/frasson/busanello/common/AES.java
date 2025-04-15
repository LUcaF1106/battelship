package com.itis._5a.frasson.busanello.common;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public class AES {
    private SecretKeySpec secretKey;
    private IvParameterSpec iv;
    private final String MODE="AES/CBC/PKCS5Padding";


    public void setupAESKeys(byte[] sharedSecret)  {
        byte[] key = Arrays.copyOfRange(sharedSecret, 0, 32);
        byte[] ivBytes = Arrays.copyOfRange(sharedSecret, 32, 48);
        secretKey = new SecretKeySpec(key, "AES");
        iv = new IvParameterSpec(ivBytes);
    }

    public byte[] encrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance(MODE);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        return cipher.doFinal(data);
    }

    public byte[] decrypt(byte[] encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance(MODE);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
        return cipher.doFinal(encryptedData);
    }
}