package com.itis._5a.frasson.busanello.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.password4j.Argon2Function;
import com.password4j.Password;
import com.password4j.SecureString;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.password4j.types.Argon2;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class Auth {

    private static final Logger LOGGER = LogManager.getLogger(Auth.class);
    private static final String USER_FILE = "users.json";
    private final UserDatabase userDb;
    private final Gson gson;

    private String getUserFilePath() {
        try {
            return getClass().getClassLoader().getResource(USER_FILE).toURI().getPath();
        } catch (Exception e) {
            throw new RuntimeException("File users.json non trovato in resources!", e);
        }
    }
    public Auth() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        UserDatabase loadedDb = loadUserDatabase();

        if (loadedDb == null) {
            Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.INFO);
            LOGGER.info("Creating new user database");
            this.userDb = new UserDatabase();
            this.userDb.setAlgoritmoHash("Argon2");
            this.userDb.setUsers(new ArrayList<>());
            // Add default admin user if database was newly created
            addUser("admin", "admin123");
            saveUserDatabase();
        } else {
            this.userDb = loadedDb;
        }
    }

    public boolean authenticate(String username, String password) {

        if (username == null || password == null) {
            LOGGER.warn("Authentication attempt with null username or password");
            return false;
        }

        Optional<User> userOpt = findUserByUsername(username);
        if (userOpt.isEmpty()) {
            LOGGER.info("Authentication failed: User not found: " + username);
            return false;
        }

        User user = userOpt.get();
        SecureString securePassword = new SecureString(password.toCharArray());
        try {

            Argon2Function argon2 = Argon2Function.getInstance(16384, 3, 1, 256, Argon2.ID);
            boolean result = Password.check(securePassword, user.getPassword()).with(argon2);
            LOGGER.debug("Verifica password per " + username +
                    "Hash salvato: " + user.getPassword() +
                    "Hash calcolato: " + Password.hash(password).with(argon2).getResult());
            LOGGER.info("Authentication for user " + username + ": " + (result ? "successful" : "failed"));
            return result;
        } finally {
            securePassword.clear(); // Manually clear the sensitive data
        }
    }

    public boolean addUser(String username, String password) {
        // Check if user already exists
        if (findUserByUsername(username).isPresent()) {
            LOGGER.warn("Failed to add user: Username already exists: " + username);
            return false;
        }

        // Create secure hash of password
        SecureString securePassword = new SecureString(password.toCharArray());
        try  {
            Argon2Function argon2 = Argon2Function.getInstance(16384, 3, 1, 256, Argon2.ID);

            String hashedUsername = Password.hash(username).with(argon2).getResult();
            String hashedPassword = Password.hash(securePassword).with(argon2).getResult();

            // Create new user and add to database
            User newUser = new User();
            newUser.setUsername(hashedUsername);
            newUser.setPassword(hashedPassword);

            // In production, you would NOT store cleartext credentials
            // This is just for demonstration/development
            Cleartext cleartext = new Cleartext();
            cleartext.setUsername(username);
            cleartext.setPassword(password);
            newUser.setCleartext(cleartext);

            userDb.getUsers().add(newUser);
            saveUserDatabase();

            LOGGER.info("Added new user: " + username);
            return true;
        } catch (Exception e) {
            LOGGER.error("Error adding user: " + username, e);
            return false;
        } finally {
            securePassword.clear();
        }
    }

    public boolean removeUser(String username) {
        Optional<User> userOpt = findUserByUsername(username);
        if (userOpt.isEmpty()) {
            LOGGER.warn("Failed to remove user: User not found: " + username);
            return false;
        }

        userDb.getUsers().remove(userOpt.get());
        saveUserDatabase();
        LOGGER.info("Removed user: " + username);
        return true;
    }

    private Optional<User> findUserByUsername(String username) {
        return userDb.getUsers().stream()
                .filter(user -> user.getCleartext() != null &&
                        username.equals(user.getCleartext().getUsername()))
                .findFirst();
    }

    private UserDatabase loadUserDatabase() {
        Path filePath = Paths.get(getUserFilePath());
        if (!Files.exists(filePath)) {
            LOGGER.info("User database file not found: " + getUserFilePath());
            return null;
        }

        try (FileReader reader = new FileReader(getUserFilePath())) {
            UserDatabase db = gson.fromJson(reader, UserDatabase.class);
            LOGGER.info("User database loaded successfully with " +
                    (db.getUsers() != null ? db.getUsers().size() : 0) + " users");
            return db;
        } catch (IOException e) {
            LOGGER.error("Error loading user database", e);
            return null;
        }
    }

    private void saveUserDatabase() {
        try (FileWriter writer = new FileWriter(USER_FILE)) {
            gson.toJson(userDb, writer);
            LOGGER.info("User database saved successfully");
        } catch (IOException e) {
            LOGGER.error("Error saving user database", e);
        }
    }

    // Data classes for JSON serialization/deserialization
    private static class UserDatabase {
        @SerializedName("algoritmoHash")
        private String algoritmoHash;

        @SerializedName("users")
        private List<User> users;

        public String getAlgoritmoHash() { return algoritmoHash; }
        public void setAlgoritmoHash(String algoritmoHash) { this.algoritmoHash = algoritmoHash; }

        public List<User> getUsers() { return users; }
        public void setUsers(List<User> users) { this.users = users; }
    }

    private static class User {
        @SerializedName("username")
        private String username;

        @SerializedName("password")
        private String password;

        @SerializedName("cleartext")
        private Cleartext cleartext;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public Cleartext getCleartext() { return cleartext; }
        public void setCleartext(Cleartext cleartext) { this.cleartext = cleartext; }
    }

    private static class Cleartext {
        @SerializedName("username")
        private String username;

        @SerializedName("password")
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
