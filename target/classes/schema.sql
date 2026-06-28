-- ============================================================
--  Wimbledon 2026 - Singles Masculino - Schema (MySQL)
-- ============================================================

CREATE DATABASE IF NOT EXISTS wimbledon
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE wimbledon;

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS matches (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    match_date  DATE         NOT NULL,
    match_time  TIME         NOT NULL,
    court       VARCHAR(60)  NOT NULL,
    player1     VARCHAR(100) NOT NULL,
    player2     VARCHAR(100) NOT NULL,
    round       VARCHAR(40),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS match_results (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    match_id     BIGINT       NOT NULL UNIQUE,
    winner       VARCHAR(100) NOT NULL,
    sets_winner  INT,
    games_winner INT,
    games_loser  INT,
    entered_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_result_match FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS picks (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    match_id      BIGINT       NOT NULL,
    winner        VARCHAR(100) NOT NULL,
    sets_winner   INT,
    games_winner  INT,
    games_loser   INT,
    is_correction TINYINT(1)   NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_match (user_id, match_id),
    CONSTRAINT fk_pick_user  FOREIGN KEY (user_id)  REFERENCES users(id)   ON DELETE CASCADE,
    CONSTRAINT fk_pick_match FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS daily_corrections (
    id        BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id   BIGINT NOT NULL,
    used_date DATE   NOT NULL,
    UNIQUE KEY uq_user_date (user_id, used_date),
    CONSTRAINT fk_correction_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tournament_picks (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL UNIQUE,
    champion   VARCHAR(100),
    semi1      VARCHAR(100),
    semi2      VARCHAR(100),
    semi3      VARCHAR(100),
    semi4      VARCHAR(100),
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tpick_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tournament_result (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    champion   VARCHAR(100),
    semi1      VARCHAR(100),
    semi2      VARCHAR(100),
    semi3      VARCHAR(100),
    semi4      VARCHAR(100),
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ── Seed: admin user (password: "admin1234" bcrypt) ───────────────────
INSERT INTO users (name, email, password, role)
VALUES ('Admin', 'admin@wimbledon.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'ADMIN')
ON DUPLICATE KEY UPDATE email = email;

-- ── Seed: partidos de ejemplo Wimbledon Day 1 ─────────────────────────
INSERT INTO matches (match_date, match_time, court, player1, player2, round) VALUES
  ('2026-06-29', '11:00:00', 'Centre Court', 'Carlos Alcaraz',  'Ugo Humbert',   'R128'),
  ('2026-06-29', '12:30:00', 'Court 1',      'Jannik Sinner',   'Ben Shelton',   'R128'),
  ('2026-06-29', '14:00:00', 'Court 2',      'Novak Djokovic',  'Taylor Fritz',  'R128'),
  ('2026-06-29', '16:00:00', 'Court 1',      'Daniil Medvedev', 'Holger Rune',   'R128');
