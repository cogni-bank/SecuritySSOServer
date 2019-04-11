package com.cognibank.securityMicroservice.Repository;

import com.cognibank.securityMicroservice.Model.NotificationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, Long> {
}
