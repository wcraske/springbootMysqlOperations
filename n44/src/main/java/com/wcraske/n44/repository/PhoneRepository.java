package com.wcraske.n44.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wcraske.n44.entity.Phone;

public interface PhoneRepository extends JpaRepository<Phone, Long> {

   @Query("SELECT new com.wcraske.n44.repository.PhoneProjection(p.brand, p.modelName, p.storageGb, p.os) FROM Phone p")
    List<PhoneProjection> findPhoneProjections(Pageable pageable);

    @Query("SELECT p FROM Phone p WHERE " +
       "(:brand IS NULL OR p.brand = :brand) AND " +
       "(:os IS NULL OR p.os = :os) AND " +
       "(:maxPrice IS NULL OR p.priceInr <= :maxPrice)")
    List<Phone> searchPhones(
        @Param("brand") String brand,
        @Param("os") String os,
        @Param("maxPrice") Double maxPrice,
        Pageable pageable
    );
}