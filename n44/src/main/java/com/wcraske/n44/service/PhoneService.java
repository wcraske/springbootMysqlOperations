package com.wcraske.n44.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.wcraske.n44.entity.Phone;
import com.wcraske.n44.repository.PhoneProjection;
import com.wcraske.n44.repository.PhoneRepository;

@Service
public class PhoneService {

    private final PhoneRepository phoneRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    PhoneService(PhoneRepository phoneRepository, RedisTemplate<String, Object> redisTemplate) {
        this.phoneRepository = phoneRepository;
        this.redisTemplate = redisTemplate;
    }

    public List<Phone> getAllPhones() {
        return phoneRepository.findAll();
    }

    @SuppressWarnings("unchecked")
    public List<Phone> getPagedPhones(Pageable pageable) {
        String key = "pagedPhones:" + pageable.getPageNumber() + ":" + pageable.getPageSize();

        List<Phone> cached = (List<Phone>) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        List<Phone> result = phoneRepository.findAll(pageable).getContent();
        redisTemplate.opsForValue().set(key, result, 10, TimeUnit.MINUTES);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<PhoneProjection> getNecessaryDetails() {
        String key = "phoneProjections:page0";

        List<PhoneProjection> cached = (List<PhoneProjection>) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        List<PhoneProjection> result = phoneRepository.findPhoneProjections(PageRequest.of(0, 20));
        redisTemplate.opsForValue().set(key, result, 10, TimeUnit.MINUTES);
        return result;
    }

    public void clearCache() {
        redisTemplate.delete("phoneProjections:page0");

        var keys = redisTemplate.keys("pagedPhones:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}