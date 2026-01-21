package com.fomo.backend;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList; // Thread safe list

/**
 * Fomo Backend Web Server (Javalin)
 * Handles Login and Signup requests.
 */
public class FomoBackend {

    // In-memory storage (Data is lost on server restart)
    // Using Thread-Safe list for concurrency
    private static final List<User> users = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        // Pre-register test user
        users.add(new User("testuser", "test@example.com", "password123"));

        // Get Port from Env (Render sets this) or default to 8080
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        Javalin app = Javalin.create(config -> {
            config.plugins.enableCors(cors -> cors.add(it -> {
                it.anyHost(); // Allow frontend to call this
            }));
        }).start(port);

        // Routes
        app.get("/", ctx -> ctx.result("Fomo Backend is Running! 🚀"));
        app.post("/signup", FomoBackend::handleSignup);
        app.post("/login", FomoBackend::handleLogin);
    }

    private static void handleSignup(Context ctx) {
        try {
            User newUser = ctx.bodyAsClass(User.class);

            // Check existence
            boolean exists = users.stream().anyMatch(
                    u -> u.email.equalsIgnoreCase(newUser.email) || u.username.equalsIgnoreCase(newUser.username));

            if (exists) {
                ctx.status(409).json("User already exists");
                return;
            }

            users.add(newUser);
            System.out.println("New User Registered: " + newUser.username);
            ctx.status(201).json(newUser); // Return created user
        } catch (Exception e) {
            ctx.status(400).result("Invalid Data");
        }
    }

    private static void handleLogin(Context ctx) {
        try {
            User credentials = ctx.bodyAsClass(User.class); // Reusing User class for payload

            User foundUser = users.stream()
                    .filter(u -> (u.email.equalsIgnoreCase(credentials.email)
                            || u.username.equalsIgnoreCase(credentials.email))
                            && u.password.equals(credentials.password))
                    .findFirst()
                    .orElse(null);

            if (foundUser != null) {
                ctx.status(200).json(foundUser);
            } else {
                ctx.status(401).json("Invalid Credentials");
            }
        } catch (Exception e) {
            ctx.status(400).result("Invalid Data");
        }
    }

    // Include User class locally for simplicity
    public static class User {
        public String username; // Public for Jackson serialization
        public String email;
        public String password;

        // Default constructor needed for Jackson
        public User() {
        }

        public User(String username, String email, String password) {
            this.username = username;
            this.email = email;
            this.password = password;
        }
    }
}
