-- ============================================================
--  Migration: Notificaciones, Ligas, Torneos
--  Ejecutar en Neon PostgreSQL
-- ============================================================

-- Tabla de suscripciones a notificaciones push
CREATE TABLE IF NOT EXISTS notification_subscriptions (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    endpoint    VARCHAR(500) NOT NULL,
    p256dh      VARCHAR(500) NOT NULL,
    auth_key    VARCHAR(500) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, endpoint)
);

-- Tabla de ligas privadas
CREATE TABLE IF NOT EXISTS leagues (
    id          BIGSERIAL PRIMARY KEY,
    owner_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(80)  NOT NULL,
    code        VARCHAR(12)  NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Tabla de miembros de liga
CREATE TABLE IF NOT EXISTS league_members (
    id          BIGSERIAL PRIMARY KEY,
    league_id   BIGINT       NOT NULL REFERENCES leagues(id) ON DELETE CASCADE,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(league_id, user_id)
);

-- Tabla de torneos (soporte multi-torneo)
CREATE TABLE IF NOT EXISTS tournaments (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(20)  NOT NULL UNIQUE,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Insertar Wimbledon 2026 como torneo activo
INSERT INTO tournaments (name, slug, active)
VALUES ('Wimbledon 2026', 'wimbledon-2026', TRUE)
ON CONFLICT (slug) DO NOTHING;

-- Índices
CREATE INDEX IF NOT EXISTS idx_notif_sub_user ON notification_subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_league_members_user ON league_members(user_id);
CREATE INDEX IF NOT EXISTS idx_league_members_league ON league_members(league_id);