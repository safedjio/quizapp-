package com.example.quizapp.service;

import com.example.quizapp.entity.QuizQuestion;
import com.example.quizapp.entity.UserScore;
import com.example.quizapp.repository.QuizQuestionRepository;
import com.example.quizapp.repository.UserScoreRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


@Service
public class QuizService {
    @Autowired
    private QuizQuestionRepository questionRepo;
    @Autowired
    private UserScoreRepository scoreRepo;

    @Value("${quiz.category:JavaNovice}")  // Категория из application.properties, по умолчанию JavaNovice
    private String currentCategory;

    private List<QuizQuestion> questions;
    private int currentIndex = 0;
    private Map<String, Integer> currentAnswers = new ConcurrentHashMap<>();
    private int totalClients = 0;
    private ScheduledExecutorService timerExecutor = Executors.newScheduledThreadPool(1);  // Для таймера
    private ScheduledFuture<?> currentTimer;  // Текущий таймер

    @PostConstruct
    public void init() {
        loadQuestions();
    }

    public void loadQuestions() {
        questions = questionRepo.findByCategory(currentCategory);  // Загружаем только по категории
        if (questions.isEmpty()) {
            throw new RuntimeException("Нет вопросов в категории: " + currentCategory);
        }
    }

    public QuizQuestion getCurrentQuestion() {
        return questions.get(currentIndex);
    }

    public void addClient() {
        totalClients++;
    }

    public void removeClient() {
        totalClients--;
    }

    public void submitAnswer(String userName, int answer) {
        if (!currentAnswers.containsKey(userName)) {
            currentAnswers.put(userName, answer);
            if (currentAnswers.size() == totalClients) {
                processAnswers();
                nextQuestion();
            }
        }
    }

    public int getCurrentAnswersSize() {
        return currentAnswers.size();
    }

    private void processAnswers() {
        for (Map.Entry<String, Integer> entry : currentAnswers.entrySet()) {
            String user = entry.getKey();
            int answer = entry.getValue();
            boolean correct = answer == getCurrentQuestion().getCorrectOption();
            UserScore score = scoreRepo.findById(user).orElse(new UserScore());
            score.setUserName(user);
            if (correct) {
                score.setPoints(score.getPoints() + 100);
            }
            scoreRepo.save(score);
        }
        currentAnswers.clear();
    }

    public void nextQuestion() {
        currentIndex = (currentIndex + 1) % questions.size();
    }

    public void setCategory(String category) {  // Этот метод добавлен для категорий
        this.currentCategory = category;
        loadQuestions();
    }

    public List<UserScore> getLeaderboard() {
        return scoreRepo.findAll().stream()
                .sorted((a, b) -> Integer.compare(b.getPoints(), a.getPoints()))
                .toList();
    }

    public void startTimer() {
        if (currentTimer != null) {
            currentTimer.cancel(false);
        }
        int timeLimit = getCurrentQuestion().getTimeLimit();
        if (timeLimit <= 0) timeLimit = 30;  // По умолчанию 30 секунд
        currentTimer = timerExecutor.schedule(() -> {
            processTimeout();
            nextQuestion();
            // broadcastQuestion() нужно вызвать из QuizWebSocketHandler
        }, timeLimit, TimeUnit.SECONDS);
    }

    private void processTimeout() {
        // Для всех подключённых пользователей, кто не ответил, добавить неправильный ответ
        // sessions — из QuizWebSocketHandler, передайте или сделайте публичным
        // Пример: quizWebSocketHandler.getSessions().keySet()
        // Здесь заглушка — в реальности интегрируйте с handler
        for (String user : currentAnswers.keySet()) {  // Заглушка, замените на реальный список пользователей
            if (!currentAnswers.containsKey(user)) {
                submitAnswer(user, 0);
            }
        }
    }
}