package com.itis._5a.frasson.busanello.common;

import com.google.gson.Gson;
import com.itis._5a.frasson.busanello.common.Message.Message;

import java.nio.charset.StandardCharsets;

public class Json {

    private static final Gson GSON=new Gson();

    public static byte[] toByteArray(Object message) {
        String json = GSON.toJson(message);
        return json.getBytes(StandardCharsets.UTF_8);
    }
    public static <T> T deserializedSpecificMessage(byte[] arr, Class<T> typeM)
    {
        String s= new String(arr, StandardCharsets.UTF_8);
        return GSON.fromJson(s, typeM);
    }
    public static Message deserialiazedMessage(byte[] arr){
        String s= new String(arr, StandardCharsets.UTF_8);
        return GSON.fromJson(s, Message.class);
    }
    public static byte[] serializedMessage(Object mes){
        String s=GSON.toJson(mes);
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
