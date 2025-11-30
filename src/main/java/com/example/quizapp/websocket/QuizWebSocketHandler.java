package com.example.quizapp.websocket;

import com.example.quizapp.entity.QuizQuestion;
import com.example.quizapp.entity.UserScore;
import com.example.quizapp.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class QuizWebSocketHandler extends TextWebSocketHandler {
    @Autowired
    private QuizService quizService;

    private Map<WebSocketSession, String> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session, "");
        quizService.addClient();
        sendCurrentQuestion(session);
        sendLeaderboard(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        sessions.remove(session);
        quizService.removeClient();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if (payload.startsWith("USERNAME:")) {
            String userName = payload.substring("USERNAME:".length());
            sessions.put(session, userName);
        } else if (payload.startsWith("CATEGORY:")) {
            String category = payload.substring("CATEGORY:".length());
            quizService.setCategory(category);
            sendCurrentQuestion(session);
            quizService.startTimer();
        } else if (payload.startsWith("ANSWER:")) {
            int answer = Integer.parseInt(payload.substring("ANSWER:".length()));
            String userName = sessions.get(session);
            quizService.submitAnswer(userName, answer);
            boolean correct = answer == quizService.getCurrentQuestion().getCorrectOption();
            session.sendMessage(new TextMessage("ANSWER_RESULT|" + (correct ? "correct" : "incorrect") + "|100"));
            broadcastLeaderboard();
            if (quizService.getCurrentAnswersSize() == 0) {
                quizService.nextQuestion();
                broadcastQuestion();
                quizService.startTimer();
            }
        } else if (payload.equals("TIME_UP")) {
            String userName = sessions.get(session);
            quizService.submitAnswer(userName, 0);  // Неправильный ответ
            session.sendMessage(new TextMessage("ANSWER_RESULT|incorrect|0"));  // Показать результат
            broadcastLeaderboard();
            if (quizService.getCurrentAnswersSize() == 0) {
                quizService.nextQuestion();
                broadcastQuestion();
                quizService.startTimer();
            }
        }
    }

    private void sendCurrentQuestion(WebSocketSession session) throws IOException {
        QuizQuestion q = quizService.getCurrentQuestion();
        String msg = "QUESTION|" + q.getQuestion() + "|" + q.getOption1() + "|" + q.getOption2() + "|" + q.getOption3() + "|" + q.getOption4();
        session.sendMessage(new TextMessage(msg));
    }

    private void broadcastQuestion() throws IOException {
        QuizQuestion q = quizService.getCurrentQuestion();
        String msg = "QUESTION|" + q.getQuestion() + "|" + q.getOption1() + "|" + q.getOption2() + "|" + q.getOption3() + "|" + q.getOption4();
        broadcast(msg);
    }

    private void sendLeaderboard(WebSocketSession session) throws IOException {
        StringBuilder sb = new StringBuilder("LEADERBOARD");
        List<UserScore> leaderboard = quizService.getLeaderboard();
        for (int i = 0; i < leaderboard.size(); i++) {
            UserScore score = leaderboard.get(i);
            sb.append("|").append((i + 1)).append(";").append(score.getUserName()).append(";").append(score.getPoints());
        }
        session.sendMessage(new TextMessage(sb.toString()));
        System.out.println("SEND LEADERBOARD (single): " + sb);
    }

    private void broadcastLeaderboard() throws IOException {
        StringBuilder sb = new StringBuilder("LEADERBOARD");
        List<UserScore> leaderboard = quizService.getLeaderboard();
        for (int i = 0; i < leaderboard.size(); i++) {
            UserScore score = leaderboard.get(i);
            sb.append("|").append((i + 1)).append(";").append(score.getUserName()).append(";").append(score.getPoints());
        }
        broadcast(sb.toString());
        System.out.println("SEND LEADERBOARD: " + sb);
    }

    private void broadcast(String message) throws IOException {
        for (WebSocketSession session : sessions.keySet()) {
            session.sendMessage(new TextMessage(message));
            System.out.println("SESSIONS: " + sessions);
        }
    }
}