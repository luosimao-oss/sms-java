package com.luosimao.sms.model;

import com.google.gson.annotations.SerializedName;

public class StatusResponse extends BaseResponse {

    @SerializedName("deposit")
    private Integer deposit;

    public Integer getDeposit() {
        return deposit;
    }

    public void setDeposit(Integer deposit) {
        this.deposit = deposit;
    }

    @Override
    public String toString() {
        return "StatusResponse{" +
                "error=" + getError() +
                ", msg='" + getMsg() + '\'' +
                ", deposit=" + deposit +
                '}';
    }
}
