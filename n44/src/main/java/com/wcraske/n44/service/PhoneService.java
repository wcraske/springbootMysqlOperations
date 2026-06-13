package com.wcraske.n44.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.wcraske.n44.entity.Phone;
import com.wcraske.n44.repository.PhoneProjection;
import com.wcraske.n44.repository.PhoneRepository;

@Service
public class PhoneService {

    private final PhoneRepository phoneRepository;

    PhoneService(PhoneRepository phoneRepository) {
        this.phoneRepository = phoneRepository;
    }
    //all phones list
    public List<Phone> getAllPhones() {
        return phoneRepository.findAll();
    }
    //all phones with pagination
    public List<Phone> getPagedPhones(Pageable pageable) {
        return phoneRepository.findAll(pageable).getContent();
    }
    //all phones with pagination and projection
    public List<PhoneProjection> getNecessaryDetails() {
        return phoneRepository.findPhoneProjections(PageRequest.of(0, 20));
}
}