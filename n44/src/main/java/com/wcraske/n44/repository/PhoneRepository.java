package com.wcraske.n44.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.wcraske.n44.entity.Phone;

public interface PhoneRepository extends JpaRepository<Phone, Long> {

   @Query("SELECT new com.wcraske.n44.repository.PhoneProjection(p.brand, p.modelName, p.storageGb, p.os) FROM Phone p")
    List<PhoneProjection> findPhoneProjections(Pageable pageable);
}