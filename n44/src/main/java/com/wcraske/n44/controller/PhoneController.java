package com.wcraske.n44.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.wcraske.n44.entity.Phone;
import com.wcraske.n44.repository.PhoneProjection;
import com.wcraske.n44.service.PhoneService;

@RestController
@RequestMapping("/phones")
public class PhoneController {

    private final PhoneService phoneService;

    PhoneController(PhoneService phoneService) {
        this.phoneService = phoneService;
    }

    @GetMapping("/full-list")
    public List<Phone> getAllPhones() {
        return phoneService.getAllPhones();
    }

    @GetMapping("/pagination")
    public List<Phone> getPagedPhones(@RequestParam(required = false, defaultValue = "1") int pageNo, @RequestParam(required = false, defaultValue = "5") int pageSize) {
        return phoneService.getPagedPhones(PageRequest.of(pageNo, pageSize));
    }

    @GetMapping("/projection")
    public List<PhoneProjection> getProjection() {
        return phoneService.getNecessaryDetails();
    }

    
}