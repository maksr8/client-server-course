package org.example.crypto;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;

import static org.example.crypto.CipherService.decryptMessage;
import static org.example.crypto.CipherService.encryptMessage;

public class EncryptionService {
    private static final byte MESSAGE_PACKAGE_START_BYTE = 0x13;

    public byte[] encrypt(Message message, String secretPassword) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        byte[] stringBytes = message.messageString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer payloadBuffer = ByteBuffer.allocate(4 + 4 + stringBytes.length);
        payloadBuffer.putInt(message.commandType());
        payloadBuffer.putInt(message.userID());
        payloadBuffer.put(stringBytes);
        byte[] encryptedPayload = encryptMessage(payloadBuffer.array(), secretPassword);

        ByteBuffer buffer = ByteBuffer.allocate(1 + 1 + 8 + 4 + 2 + encryptedPayload.length + 2);
        buffer.put(MESSAGE_PACKAGE_START_BYTE);
        buffer.put(message.clientAppNumber());
        buffer.putLong(message.messageID());
        buffer.putInt(encryptedPayload.length);

        byte[] header = new byte[14];
        buffer.get(0, header, 0, 14);
        buffer.putShort(Crc16.calculateCrc(header));

        buffer.put(encryptedPayload);
        buffer.putShort(Crc16.calculateCrc(encryptedPayload));

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

        byte[] encryptedPayload = new byte[dataLength];
        buffer.get(encryptedPayload);

        short payloadCRC = buffer.getShort();
        if (Crc16.calculateCrc(encryptedPayload) != payloadCRC) {
            throw new IllegalArgumentException("Payload CRC does not match");
        }

        byte[] decryptedPayload = decryptMessage(encryptedPayload, secretPassword);
        ByteBuffer payloadBuffer = ByteBuffer.wrap(decryptedPayload);
        int commandType = payloadBuffer.getInt();
        int userID = payloadBuffer.getInt();

        byte[] stringBytes = new byte[decryptedPayload.length - 8];
        payloadBuffer.get(stringBytes);
        String messageString = new String(stringBytes, StandardCharsets.UTF_8);

        return new Message(clientAppNumber, messageID, commandType, userID, messageString);
    }

}
