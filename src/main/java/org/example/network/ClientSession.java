package org.example.network;

public interface ClientSession {
    void sendData(byte[] data) throws Exception;
}