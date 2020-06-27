package com.xiaojiang.sbu.justwifi;

public class UpdateHelper {

    String userId;

    String decviceId;


    public UpdateHelper(String userId, String decviceId) {
        this.userId = userId;
        this.decviceId = decviceId;
    }
    public UpdateHelper() {}

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDecviceId() {
        return decviceId;
    }

    public void setDecviceId(String decviceId) {
        this.decviceId = decviceId;
    }
}
