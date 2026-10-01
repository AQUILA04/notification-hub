package com.optimizesolux.notificationhub.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SmsTestRecipientRepository extends JpaRepository<SmsTestRecipientEntity, UUID> {

    List<SmsTestRecipientEntity> findAllByOrderByEmailAsc();

    @Query("select e from SmsTestRecipientEntity e where lower(e.email) = lower(:email)")
    Optional<SmsTestRecipientEntity> findByEmailIgnoreCase(@Param("email") String email);

    @Query("select e.email from SmsTestRecipientEntity e order by e.email asc")
    List<String> findAllEmailsOrderByEmailAsc();
}
