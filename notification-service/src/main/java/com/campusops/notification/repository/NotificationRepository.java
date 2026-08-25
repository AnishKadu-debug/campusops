package com.campusops.notification.repository;

import com.campusops.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByOrderByCreatedAtDesc();

    List<Notification> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);
}
