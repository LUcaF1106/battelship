package com.itis._5a.frasson.busanello.common.Message;

import lombok.Getter;
import lombok.Setter;


public class LoginM extends Message {

    @Getter
    @Setter
    private String user;

    @Getter
    @Setter
    private String password;

    public LoginM(){
        this.type="LOGIN";
    }
}
