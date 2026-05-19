package org.example;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


class EncryptionServiceTest {
    private static final EncryptionService SUT = new EncryptionService();

    @Test
    void testEncryptsWithoutExceptions() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Message message = new Message((byte) 0x02, 21, 5, 89, "my important message");
        String password = "My SUPER secret cybersecure password! cyberops analyst!";

        SUT.encrypt(message, password);
    }

    @Test
    void testDecryptedMessageEqualsOriginal() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Message message = new Message((byte) 0x02, 21, 5, 89, "my important повідомлення");
        String password = "My SUPER secret cybersecure password! cyberops analyst!";
        byte[] encryptedMessage = SUT.encrypt(message, password);
        Message decryptedMessage = SUT.decrypt(encryptedMessage, password);
        System.out.println(decryptedMessage.messageString());

        Assertions.assertThat(decryptedMessage).isEqualTo(message);
    }

    @Test
    void testDecryptWithInvalidMagicByteShouldThrow() {
        Assertions.assertThatThrownBy(() ->
                        SUT.decrypt(new byte[]{(byte) 0x88, (byte) 0xff, (byte) 0x56}, "password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Encrypted message should start with magic byte:");
    }

    @Test
    void testDecryptWithModifiedEncryptedMessageHeaderShouldThrow() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Message message = new Message((byte) 0x02, 21, 5, 89, "my important message");
        String password = "My SUPER secret cybersecure password! cyberops analyst!";
        byte[] encryptedMessage = SUT.encrypt(message, password);
        encryptedMessage[4] = (byte) (encryptedMessage[4] + 2);

        Assertions.assertThatThrownBy(() ->
                        SUT.decrypt(encryptedMessage, password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Header CRC does not match");
    }

    @Test
    void testDecryptWithModifiedEncryptedMessagePayloadShouldThrow() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Message message = new Message((byte) 0x02, 21, 5, 89, "my important message");
        String password = "My SUPER secret cybersecure password! cyberops analyst!";
        byte[] encryptedMessage = SUT.encrypt(message, password);
        encryptedMessage[40] = (byte) (encryptedMessage[40] + 2);

        Assertions.assertThatThrownBy(() ->
                        SUT.decrypt(encryptedMessage, password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Payload CRC does not match");
    }

    @Test
    void testDecryptWithWrongPasswordShouldThrow() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        Message message = new Message((byte) 0x02, 21, 5, 89, "my important message");
        String password = "My SUPER secret cybersecure password! cyberops analyst!";
        byte[] encryptedMessage = SUT.encrypt(message, password);

        Assertions.assertThatThrownBy(() ->
                        SUT.decrypt(encryptedMessage, "NOT My SUPER password :( "))
                .isInstanceOf(AEADBadTagException.class);
    }
}