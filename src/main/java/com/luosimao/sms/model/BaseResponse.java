package com.luosimao.sms.model;

import com.google.gson.annotations.SerializedName;

public class BaseResponse {
    @SerializedName("error")
    private int error;

    @SerializedName("msg")
    private String msg;

    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public boolean isSuccess() {
        return error == 0;
    }
}
