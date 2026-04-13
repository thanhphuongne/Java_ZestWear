package com.zestwear.backend.repositories;

import com.zestwear.backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findByRefreshToken(String refreshToken);
}
