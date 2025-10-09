package com.bookWise.user.service.repository;

import com.bookWise.user.service.model.entity.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserAuthRepository extends JpaRepository<UserToken, UUID> {
}
