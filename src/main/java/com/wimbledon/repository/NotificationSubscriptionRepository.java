package com.wimbledon.repository;

import com.wimbledon.entity.NotificationSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationSubscriptionRepository extends JpaRepository<NotificationSubscription, Long> {

    List<NotificationSubscription> findByUserId(Long userId);

    void deleteByEndpoint(String endpoint);

    List<NotificationSubscription> findAll();
}