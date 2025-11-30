package com.example.quizapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.quizapp.service.QuizService;

@RestController
public class QuizController {
    @Autowired
    private QuizService quizService;

    @GetMapping("/")
    public String home() {
        return "Welcome to Quiz App! Open /index.html in browser.";
    }
}