package com.luosimao.sms.model;

import com.google.gson.annotations.SerializedName;

public class SmsResponse extends BaseResponse {
    
    @SerializedName("batch_id")
    private String batchId;

    @SerializedName("hit")
    private String hit;

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getHit() {
        return hit;
    }

    public void setHit(String hit) {
        this.hit = hit;
    }

    @Override
    public String toString() {
        return "SmsResponse{" +
                "error=" + getError() +
                ", msg='" + getMsg() + '\'' +
                ", batchId='" + batchId + '\'' +
                ", hit='" + hit + '\'' +
                '}';
    }
}
