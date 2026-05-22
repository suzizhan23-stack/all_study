package com.wordlearning.repository;

import com.wordlearning.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
    List<UserSetting> findByUserId(Long userId);
    Optional<UserSetting> findByUserIdAndSettingKey(Long userId, String key);
    Optional<UserSetting> findByUuid(String uuid);
}
