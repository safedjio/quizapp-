CREATE TABLE quiz_question (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               question VARCHAR(500) NOT NULL,
                               option1 VARCHAR(255) NOT NULL,
                               option2 VARCHAR(255) NOT NULL,
                               option3 VARCHAR(255) NOT NULL,
                               option4 VARCHAR(255) NOT NULL,
                               correct_option INT NOT NULL,
                               time_limit INT NOT NULL DEFAULT 30
);

CREATE TABLE user_score (
                            user_name VARCHAR(255) PRIMARY KEY,
                            points INT NOT NULL DEFAULT 0
);