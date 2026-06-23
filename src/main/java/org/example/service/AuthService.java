package org.example.service;

import org.example.crypto.CipherService;
import org.example.model.User;
import org.example.repository.UserRepository;

import java.util.HexFormat;

public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public void registerUser(String username, String rawPassword) {
        try {
            byte[] hashedBytes = CipherService.generateSHA256FromPassword(rawPassword);
            String passwordHash = HexFormat.of().formatHex(hashedBytes);
            userRepository.createUser(new User(null, username, passwordHash));
        } catch (Exception e) {
            throw new RuntimeException("Error registering user", e);
        }
    }

    public String login(String username, String password) {
        User user = userRepository.findByUsername(username);
        
        if (user == null) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        try {
            byte[] hashedBytes = CipherService.generateSHA256FromPassword(password);
            String inputPasswordHash = HexFormat.of().formatHex(hashedBytes);
            
            if (!user.passwordHash().equals(inputPasswordHash)) {
                throw new IllegalArgumentException("Invalid credentials");
            }
            
            return jwtService.generateToken(username);
        } catch (Exception e) {
            throw new RuntimeException("Error during login process", e);
        }
    }
}