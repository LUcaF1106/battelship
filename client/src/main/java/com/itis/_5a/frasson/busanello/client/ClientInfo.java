package com.itis._5a.frasson.busanello.client;

import lombok.Getter;
import lombok.Setter;

public class ClientInfo {

    @Getter @Setter
    private boolean value;

    private static ClientInfo instance;

    private ClientInfo() {
        this.value = false;
    }

    public static ClientInfo getInstance() {
        if (instance == null) {
            instance = new ClientInfo();
        }
        return instance;
    }
}
