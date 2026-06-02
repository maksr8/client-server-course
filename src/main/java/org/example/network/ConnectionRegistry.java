package org.example.network;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConnectionRegistry {
    private final ConcurrentMap<Byte, ClientSession> sessions = new ConcurrentHashMap<>();

    public void register(byte clientId, ClientSession session) {
        sessions.put(clientId, session);
    }

    public ClientSession getSession(byte clientId) {
        return sessions.get(clientId);
    }
    
    public void remove(byte clientId) {
        sessions.remove(clientId);
    }
}