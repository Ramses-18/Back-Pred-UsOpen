-- ============================================================
--  Migration: Notificaciones Push
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

-- Índice
CREATE INDEX IF NOT EXISTS idx_notif_sub_user ON notification_subscriptions(user_id);