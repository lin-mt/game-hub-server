package com.app.gamehub.repository;

import com.app.gamehub.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByOpenid(String openid);

  boolean existsByOpenid(String openid);

  List<User> findByArenaNotificationEnabledTrue();
}
