package org.example.crypto;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.ByteBuffer;
import java.security.*;

import static org.example.crypto.CipherService.decryptMessage;
import static org.example.crypto.CipherService.encryptMessage;

public class EncryptionService {
    private static final byte MESSAGE_PACKAGE_START_BYTE = 0x13;

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

}
