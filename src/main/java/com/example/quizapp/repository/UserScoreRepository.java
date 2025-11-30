package com.example.quizapp.repository;

import com.example.quizapp.entity.UserScore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserScoreRepository extends JpaRepository<UserScore, String> {
}