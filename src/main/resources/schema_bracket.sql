-- Tabla para el bracket del torneo
CREATE TABLE IF NOT EXISTS bracket_matches (
    id                  BIGSERIAL PRIMARY KEY,
    round               VARCHAR(8)   NOT NULL,        -- R128, R64, R32, R16, QF, SF, F
    position_in_round   INT          NOT NULL,        -- 1-indexed
    player1             VARCHAR(100),
    player2             VARCHAR(100),
    winner              VARCHAR(100),
    score_str           VARCHAR(100),
    sets_winner         INT,
    sets_loser          INT,
    source_match_1      BIGINT,
    source_match_2      BIGINT,
    status              VARCHAR(12)  NOT NULL DEFAULT 'SCHEDULED',
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    UNIQUE(round, position_in_round)
);

CREATE INDEX IF NOT EXISTS idx_bracket_round ON bracket_matches(round);
CREATE INDEX IF NOT EXISTS idx_bracket_source1 ON bracket_matches(source_match_1);
CREATE INDEX IF NOT EXISTS idx_bracket_source2 ON bracket_matches(source_match_2);
