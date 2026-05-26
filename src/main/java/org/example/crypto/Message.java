package org.example.crypto;

public record Message(byte clientAppNumber, long messageID, int commandType, int userID, String messageString) {

}
