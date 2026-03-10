-- =============================================================================
-- Anthem AI Traffic Dashboard — Supabase Seed File
-- Run this in Supabase → SQL Editor to create the schema and populate test data.
-- Safe to re-run: CREATE TABLE IF NOT EXISTS + ON CONFLICT DO NOTHING.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. SCHEMA
-- ---------------------------------------------------------------------------

CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- enables gen_random_uuid()

CREATE TABLE IF NOT EXISTS traffic_records (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    cars             INTEGER     NOT NULL DEFAULT 0,
    trucks           INTEGER     NOT NULL DEFAULT 0,
    motorcycles      INTEGER     NOT NULL DEFAULT 0,
    total            INTEGER     NOT NULL DEFAULT 0,
    cars_pct         FLOAT       NOT NULL DEFAULT 0.0,
    trucks_pct       FLOAT       NOT NULL DEFAULT 0.0,
    motorcycles_pct  FLOAT       NOT NULL DEFAULT 0.0,
    congestion_level VARCHAR(10) NOT NULL DEFAULT 'smooth',
    source           VARCHAR(20) NOT NULL DEFAULT 'frontend'
);

CREATE INDEX IF NOT EXISTS idx_traffic_records_timestamp ON traffic_records (timestamp DESC);

CREATE TABLE IF NOT EXISTS system_status (
    id                 INTEGER     PRIMARY KEY DEFAULT 1,
    backend_connected  BOOLEAN     NOT NULL DEFAULT TRUE,
    database_connected BOOLEAN     NOT NULL DEFAULT TRUE,
    ai_model_loaded    BOOLEAN     NOT NULL DEFAULT FALSE,
    stream_active      BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS reports (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status     VARCHAR(20) NOT NULL DEFAULT 'pending',
    file_url   VARCHAR(500),
    from_ts    TIMESTAMPTZ,
    to_ts      TIMESTAMPTZ,
    row_count  INTEGER
);

-- ---------------------------------------------------------------------------
-- 2. SYSTEM STATUS — single row, safe upsert
-- ---------------------------------------------------------------------------

INSERT INTO system_status (id, backend_connected, database_connected, ai_model_loaded, stream_active)
VALUES (1, TRUE, TRUE, FALSE, FALSE)
ON CONFLICT (id) DO UPDATE SET
    backend_connected  = TRUE,
    database_connected = TRUE,
    updated_at         = NOW();

-- ---------------------------------------------------------------------------
-- 3. TRAFFIC RECORDS — ~1 008 realistic rows (7 days × 24 h × 6 ticks/h)
--
-- Pattern: 
--   AM rush  07:00–09:00  → high volume (total 70-95, heavy/moderate)
--   PM rush  17:00–19:00  → high volume (total 65-90, heavy/moderate)
--   Midday   10:00–16:00  → medium volume (total 45-70, moderate/smooth)
--   Night    20:00–06:00  → low volume  (total 15-40, smooth)
--
-- Formula uses generate_series so no manual row listing is needed.
-- ---------------------------------------------------------------------------

WITH series AS (
    -- One row every 10 minutes for the last 7 days
    SELECT
        generate_series(
            NOW() - INTERVAL '7 days',
            NOW(),
            INTERVAL '10 minutes'
        ) AS ts
),
raw AS (
    SELECT
        ts,
        EXTRACT(HOUR FROM ts AT TIME ZONE 'UTC') AS hr,
        -- Hour-based multiplier drives realistic peaks
        CASE
            WHEN EXTRACT(HOUR FROM ts AT TIME ZONE 'UTC') BETWEEN 7  AND 8  THEN 1.0
            WHEN EXTRACT(HOUR FROM ts AT TIME ZONE 'UTC') BETWEEN 17 AND 18 THEN 0.95
            WHEN EXTRACT(HOUR FROM ts AT TIME ZONE 'UTC') BETWEEN 9  AND 16 THEN 0.65
            ELSE 0.30
        END AS volume_factor
    FROM series
),
computed AS (
    SELECT
        ts,
        -- Cars: base 30 + volume-scaled variance using a hash-based pseudorandom
        GREATEST(0, ROUND(30 * volume_factor + (ABS(HASHTEXT(ts::TEXT)) % 25) * volume_factor)::INT) AS cars,
        GREATEST(0, ROUND(8  * volume_factor + (ABS(HASHTEXT(ts::TEXT || 'T')) % 15) * volume_factor)::INT) AS trucks,
        GREATEST(0, ROUND(12 * volume_factor + (ABS(HASHTEXT(ts::TEXT || 'M')) % 20) * volume_factor)::INT) AS motorcycles
    FROM raw
),
totalled AS (
    SELECT
        ts,
        cars,
        trucks,
        motorcycles,
        cars + trucks + motorcycles AS total
    FROM computed
)
INSERT INTO traffic_records (
    timestamp, cars, trucks, motorcycles, total,
    cars_pct, trucks_pct, motorcycles_pct, congestion_level, source
)
SELECT
    ts,
    cars,
    trucks,
    motorcycles,
    total,
    CASE WHEN total > 0 THEN ROUND((cars::FLOAT        / total * 100)::NUMERIC, 2) ELSE 0 END,
    CASE WHEN total > 0 THEN ROUND((trucks::FLOAT      / total * 100)::NUMERIC, 2) ELSE 0 END,
    CASE WHEN total > 0 THEN ROUND((motorcycles::FLOAT / total * 100)::NUMERIC, 2) ELSE 0 END,
    CASE
        WHEN total > 75 THEN 'heavy'
        WHEN total > 50 THEN 'moderate'
        ELSE                  'smooth'
    END,
    'seed'
FROM totalled
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4. QUICK SANITY CHECKS (run these manually in the SQL Editor to verify)
-- ---------------------------------------------------------------------------

-- SELECT COUNT(*) FROM traffic_records;                         -- expect ~1008
-- SELECT * FROM traffic_records ORDER BY timestamp DESC LIMIT 5;
-- SELECT congestion_level, COUNT(*) FROM traffic_records GROUP BY 1;
-- SELECT * FROM system_status;
