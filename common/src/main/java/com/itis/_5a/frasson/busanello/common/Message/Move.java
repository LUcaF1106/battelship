package com.itis._5a.frasson.busanello.common.Message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Move extends Message {

    private int x;
    private int y;


    public Move() {
        super("MOVE");
    }

    public Move(int x, int y) {
        super("MOVE");
        this.x = x;
        this.y = y;
    }

}
