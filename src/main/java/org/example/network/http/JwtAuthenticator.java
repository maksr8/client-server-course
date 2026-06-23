package org.example.network.http;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import org.example.service.JwtService;

public class JwtAuthenticator extends Authenticator {
    private final JwtService jwtService;

    public JwtAuthenticator(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Result authenticate(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return new Failure(401);
        }

        //"Bearer "
        String token = authHeader.substring(7);
        String username = jwtService.validateTokenAndGetUsername(token);

        if (username == null) {
            return new Failure(401);
        }

        return new Success(new HttpPrincipal(username, "user"));
    }
}