package com.wyh.ems.entity.request;

public class HotRecommendationRequest {

    private int sum;

    public HotRecommendationRequest(int sum) {
        this.sum = sum;
    }

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }
}