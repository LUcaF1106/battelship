package com.itis._5a.frasson.busanello.common.Message;

import lombok.Getter;
import lombok.Setter;


@Getter
public class Message {

    protected String type;

    Message(){

    }
    public Message(String type){
        this.type=type;
    }
}
