package com.itis._5a.frasson.busanello.common.Message;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResultMove extends Message{
    private int x;
    private int y;
    private String Rmove;
    private boolean GameOver;

    public ResultMove(int x, int y, String Rmove, boolean go){
        super("RMOVE");
        this.x=x;
        this.y=y;
        this.Rmove=Rmove;
        this.GameOver=go;
    }

}
