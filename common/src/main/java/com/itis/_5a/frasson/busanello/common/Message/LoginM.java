package com.itis._5a.frasson.busanello.common.Message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginM extends Message {

    private String user;
    private String password;

    public LoginM(){
        this.type="LOGIN";
    }
}
