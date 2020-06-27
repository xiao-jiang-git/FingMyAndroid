package com.xiaojiang.sbu.justwifi;

public class MessageHelper {

    String message;
    String username;
    String data;

    public MessageHelper(String message, String username, String data) {
        this.message = message;
        this.username = username;
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
