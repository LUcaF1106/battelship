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
import java.io.File;

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
    private final String userFilePath;

    private String getUserFilePath() {
        return userFilePath;
    }

    public Auth() {
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.INFO);
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        String appDir = System.getProperty("user.dir");
        File dataDir = new File(appDir, "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        this.userFilePath = new File(dataDir, USER_FILE).getAbsolutePath();
        LOGGER.info("Using user database path: " + userFilePath);

        UserDatabase loadedDb = loadUserDatabase();

        if (loadedDb == null) {

            LOGGER.info("Creating new user database");
            this.userDb = new UserDatabase();
            this.userDb.setAlgoritmoHash("Argon2");
            this.userDb.setUsers(new ArrayList<>());
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

            LOGGER.info("Authentication for user " + username + ": " + (result ? "successful" : "failed"));
            return result;
        } finally {
            securePassword.clear();
        }
    }

    public boolean signIn(String username, String password) {
        LOGGER.debug("Tentativo di registrazione per l'utente: " + username);

        if (username == null || password == null ||
                username.isEmpty() || password.isEmpty()) {
            LOGGER.warn("Registrazione fallita: campi username o password vuoti");
            return false;
        }

        if (findUserByUsername(username).isPresent()) {
            LOGGER.warn("Registrazione fallita: username già in uso: " + username);
            return false;
        }

        boolean result = addUser(username, password);

        if (result) {
            if (saveUserDatabase()) {
                LOGGER.info("Utente registrato con successo: " + username);
            } else {
                LOGGER.error("Registrazione riuscita ma impossibile salvare il database utenti");
            }
        } else {
            LOGGER.error("Registrazione fallita: impossibile aggiungere l'utente al database: " + username);
        }

        return result;
    }

    public boolean addUser(String username, String password) {
        if (findUserByUsername(username).isPresent()) {
            LOGGER.warn("Failed to add user: Username already exists: " + username);
            return false;
        }

        SecureString securePassword = new SecureString(password.toCharArray());
        try  {
            Argon2Function argon2 = Argon2Function.getInstance(16384, 3, 1, 256, Argon2.ID);

            String hashedUsername = Password.hash(username).with(argon2).getResult();
            String hashedPassword = Password.hash(securePassword).with(argon2).getResult();

            User newUser = new User();
            newUser.setUsername(hashedUsername);
            newUser.setPassword(hashedPassword);

            Cleartext cleartext = new Cleartext();
            cleartext.setUsername(username);
            cleartext.setPassword(password);
            newUser.setCleartext(cleartext);

            userDb.getUsers().add(newUser);

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
        File file = new File(getUserFilePath());
        if (!file.exists()) {
            LOGGER.info("User database file not found: " + file.getAbsolutePath());
            return null;
        }

        try (FileReader reader = new FileReader(file)) {
            UserDatabase db = gson.fromJson(reader, UserDatabase.class);
            LOGGER.info("User database loaded successfully with " +
                    (db.getUsers() != null ? db.getUsers().size() : 0) + " users");
            return db;
        } catch (IOException e) {
            LOGGER.error("Error loading user database", e);
            return null;
        }
    }

    private boolean saveUserDatabase() {
        String filePath = getUserFilePath();
        try {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(userDb, writer);
                LOGGER.info("User database saved successfully to: " + filePath);
                return true;
            }
        } catch (IOException e) {
            LOGGER.error("Error saving user database to: " + filePath, e);
            return false;
        }
    }

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