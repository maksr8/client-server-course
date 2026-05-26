package org.example;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;

public class CipherService {
    public static final int IV_LENGTH = 12;

    public static byte[] encryptMessage(String message, String secretPassword) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec secretKey = new SecretKeySpec(generateSHA256FromPassword(secretPassword), "AES");
        byte[] iv = new byte[IV_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
        byte[] encryptedMessage = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
        byte[] combinedIvAndCipherText = new byte[iv.length + encryptedMessage.length];
        System.arraycopy(iv, 0, combinedIvAndCipherText, 0, iv.length);
        System.arraycopy(encryptedMessage, 0, combinedIvAndCipherText, iv.length, encryptedMessage.length);

        return combinedIvAndCipherText;
    }

    public static String decryptMessage(byte[] encryptedMessage, int messageLength, String secretPassword) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec secretKey = new SecretKeySpec(generateSHA256FromPassword(secretPassword), "AES");
        byte[] iv = new byte[IV_LENGTH];
        byte[] cipherBytes = new byte[messageLength - IV_LENGTH];
        System.arraycopy(encryptedMessage, 0, iv, 0, iv.length);
        System.arraycopy(encryptedMessage, iv.length, cipherBytes, 0, messageLength - IV_LENGTH);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

        byte[] decryptedBytes = cipher.doFinal(cipherBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    public static byte[] generateSHA256FromPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        return sha256.digest(password.getBytes(StandardCharsets.UTF_8));
    }
}
