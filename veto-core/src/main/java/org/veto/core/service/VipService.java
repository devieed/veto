package org.veto.core.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.veto.core.rdbms.repository.UserRepository;

import java.math.BigDecimal;

@Service
public class VipService {

    @Resource
    private UserRepository userRepository;

    // 重新更新用户等级
    @Transactional
    public Boolean updateUserLevel(Long userId, BigDecimal total){
//        User user = userRepository.findById(userId).orElse(null);
//        user.setIsVip(true);
//        userRepository.save(user);

        return true;
    }
}
