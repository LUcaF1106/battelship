package com.itis._5a.frasson.busanello.common.Message;

import lombok.Getter;
import lombok.Setter;



public class Message {

    @Getter
    protected String type;

    Message(){

    }
    public Message(String type){
        this.type=type;
    }
}
