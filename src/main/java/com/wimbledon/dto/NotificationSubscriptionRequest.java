package com.wimbledon.dto;
import lombok.Data;

@Data
public class NotificationSubscriptionRequest {
    private String endpoint;
    private String p256dh;
    private String authKey;
}