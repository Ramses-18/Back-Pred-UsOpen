-- ============================================================
--  Wimbledon 2026 - Singles Masculino - Schema
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',  -- USER | ADMIN
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS matches (
    id          BIGSERIAL PRIMARY KEY,
    match_date  DATE         NOT NULL,
    match_time  TIME         NOT NULL,
    court       VARCHAR(60)  NOT NULL,
    player1     VARCHAR(100) NOT NULL,
    player2     VARCHAR(100) NOT NULL,
    round       VARCHAR(40),                           -- R128, R64, R32, QF, SF, F
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS match_results (
    id          BIGSERIAL PRIMARY KEY,
    match_id    BIGINT       NOT NULL UNIQUE REFERENCES matches(id) ON DELETE CASCADE,
    winner      VARCHAR(100) NOT NULL,
    sets_winner INT,                                   -- sets won by winner (2 or 3)
    games_winner INT,                                  -- total games won by winner
    games_loser  INT,                                  -- total games won by loser
    entered_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS picks (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    match_id        BIGINT       NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    winner          VARCHAR(100) NOT NULL,
    sets_winner     INT,
    games_winner    INT,
    games_loser     INT,
    is_correction   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, match_id)
);

CREATE TABLE IF NOT EXISTS daily_corrections (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    used_date   DATE    NOT NULL,
    UNIQUE(user_id, used_date)
);

CREATE TABLE IF NOT EXISTS tournament_picks (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    champion    VARCHAR(100),
    semi1       VARCHAR(100),
    semi2       VARCHAR(100),
    semi3       VARCHAR(100),
    semi4       VARCHAR(100),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS tournament_result (
    id          BIGSERIAL PRIMARY KEY,
    champion    VARCHAR(100),
    semi1       VARCHAR(100),
    semi2       VARCHAR(100),
    semi3       VARCHAR(100),
    semi4       VARCHAR(100),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Seed admin user  (password: "admin1234" bcrypt)
INSERT INTO users (name, email, password, role)
VALUES ('Admin', 'admin@wimbledon.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh7y', 'ADMIN')
ON CONFLICT (email) DO NOTHING;

-- Seed sample matches for 2026-06-29 (Wimbledon Day 1)
INSERT INTO matches (match_date, match_time, court, player1, player2, round) VALUES
  ('2026-06-29', '11:00', 'Centre Court', 'Carlos Alcaraz',   'Ugo Humbert',      'R128'),
  ('2026-06-29', '12:30', 'Court 1',       'Jannik Sinner',    'Ben Shelton',       'R128'),
  ('2026-06-29', '14:00', 'Court 2',       'Novak Djokovic',   'Taylor Fritz',      'R128'),
  ('2026-06-29', '16:00', 'Court 1',       'Daniil Medvedev',  'Holger Rune',       'R128')
ON CONFLICT DO NOTHING;
