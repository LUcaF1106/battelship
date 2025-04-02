package com.itis._5a.frasson.busanello;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Auth {
    private Map<String, String> users = new HashMap<>();

    public Auth() {
        users.put("user1", "password1");
        users.put("user2", "password2");
        users.put("admin", "admin123");
    }

    public boolean authenticate(String username, String password) {
        String storedPassword = users.get(username);
        return storedPassword != null && storedPassword.equals(password);
    }

    public void addUser(String username, String password) {
        users.put(username, password);
    }

    public void removeUser(String username) {
        users.remove(username);
    }
}



