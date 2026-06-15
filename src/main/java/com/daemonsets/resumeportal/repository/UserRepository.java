package com.daemonsets.resumeportal.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.daemonsets.resumeportal.model.User;

import java.util.Optional;

//使用JPA持久化框架，基于ORM对象关系映射，将数据持久化到数据库中
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUserName(String userName);
}
