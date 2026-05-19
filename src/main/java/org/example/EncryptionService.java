package org.example;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;

public class EncryptionService {
    private static final byte MESSAGE_PACKAGE_START_BYTE = 0x13;
    public static final int IV_LENGTH = 12;

    public byte[] encrypt(Message message, String secretPassword) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        byte[] encryptedMessage = encryptMessage(message.messageString(), secretPassword);

        int dataLength = 4 + 4 + encryptedMessage.length;
        ByteBuffer buffer = ByteBuffer.allocate(1 + 1 + 8 + 4 + 2 + dataLength + 2);
        buffer.put(MESSAGE_PACKAGE_START_BYTE);
        buffer.put(message.clientAppNumber());
        buffer.putLong(message.messageID());
        buffer.putInt(dataLength);

        byte[] header = new byte[14];
        buffer.get(0, header, 0, 14);
        buffer.putShort(Crc16.calculateCrc(header));

        buffer.putInt(message.commandType());
        buffer.putInt(message.userID());
        buffer.put(encryptedMessage);

        byte[] payload = new byte[dataLength];
        buffer.get(16, payload, 0, payload.length);
        buffer.putShort(Crc16.calculateCrc(payload));

        return buffer.array();
    }

    public Message decrypt(byte[] encryptedMessage, String secretPassword) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        ByteBuffer buffer = ByteBuffer.wrap(encryptedMessage);

        if(buffer.get() != MESSAGE_PACKAGE_START_BYTE) {
            throw new IllegalArgumentException("Encrypted message should start with magic byte: " + MESSAGE_PACKAGE_START_BYTE);
        }

        byte clientAppNumber = buffer.get();
        long messageID = buffer.getLong();
        int dataLength = buffer.getInt();
        short headerCRC = buffer.getShort();

        byte[] header = new byte[14];
        buffer.get(0, header, 0, 14);
        if (Crc16.calculateCrc(header) != headerCRC) {
            throw new IllegalArgumentException("Header CRC does not match");
        }

        int commandType = buffer.getInt();
        int userID = buffer.getInt();

        int messageLength = dataLength - 4 - 4;
        byte[] message = new byte[messageLength];
        buffer.get(message);
        ByteBuffer payloadBuffer = ByteBuffer.allocate(dataLength);
        payloadBuffer.putInt(commandType);
        payloadBuffer.putInt(userID);
        payloadBuffer.put(message);

        short payloadCRC = buffer.getShort();
        if (Crc16.calculateCrc(payloadBuffer.array()) != payloadCRC) {
            throw new IllegalArgumentException("Payload CRC does not match");
        }

        String messageString = decryptMessage(message, messageLength, secretPassword);

        return new Message(clientAppNumber, messageID, commandType, userID, messageString);
    }


    private byte[] encryptMessage(String message, String secretPassword) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
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

    private String decryptMessage(byte[] encryptedMessage, int messageLength, String secretPassword) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
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

    public byte[] generateSHA256FromPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        return sha256.digest(password.getBytes(StandardCharsets.UTF_8));
    }
}
