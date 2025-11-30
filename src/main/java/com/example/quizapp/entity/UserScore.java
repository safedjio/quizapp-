package com.example.quizapp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class UserScore {
    @Id
    private String userName;
    private int points;

    // Геттеры и сеттеры
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
}

