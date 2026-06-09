package org.example.dto;

public record Message(byte clientAppNumber, long messageID, int commandType, int userID, String messageString) {

}
