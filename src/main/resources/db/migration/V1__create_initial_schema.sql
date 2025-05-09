-- Create tables for the parking management system

-- Garage table
CREATE TABLE if not exists garages
(
    id         UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL     DEFAULT CURRENT_TIMESTAMP
);

-- Sector table
CREATE TABLE if not exists sectors
(
    code                   VARCHAR(50)    NOT NULL PRIMARY KEY,
    base_price             DECIMAL(10, 2) NOT NULL,
    max_capacity           INT            NOT NULL,
    open_hour              TIME           NOT NULL,
    close_hour             TIME           NOT NULL,
    duration_limit_minutes INT            NOT NULL,
    garage_id              UUID           NOT NULL REFERENCES garages (id),
    created_at             TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP      NULL     DEFAULT CURRENT_TIMESTAMP
);

-- Spot table
CREATE TABLE if not exists spots
(
    id         INT PRIMARY KEY,
    latitude   DOUBLE PRECISION NOT NULL,
    longitude  DOUBLE PRECISION NOT NULL,
    sector_id  VARCHAR(50)      NOT NULL REFERENCES sectors (code),
    occupied   BOOLEAN          NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP        NULL     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (latitude, longitude)
);

-- Vehicle table
CREATE TABLE if not exists vehicles
(
    id            UUID PRIMARY KEY,
    license_plate VARCHAR(50) NOT NULL UNIQUE,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   NULL     DEFAULT CURRENT_TIMESTAMP
);

-- Parking Event table
CREATE TABLE if not exists parking_events
(
    id         UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    event_time TIMESTAMP   NOT NULL,
    vehicle_id UUID        NOT NULL REFERENCES vehicles (id),
    spot_id    INT REFERENCES spots (id),
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Parking Session table
CREATE TABLE if not exists parking_sessions
(
    id                   UUID PRIMARY KEY,
    entry_time           TIMESTAMP        NOT NULL,
    exit_time            TIMESTAMP,
    parked_time          TIMESTAMP,
    price                DECIMAL(10, 2)   NULL,
    applied_price_factor DOUBLE PRECISION NULL,
    active               BOOLEAN          NOT NULL DEFAULT TRUE,
    vehicle_id           UUID             NOT NULL REFERENCES vehicles (id),
    parked_spot_id       INT REFERENCES spots (id),
    created_at           TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP        NULL     DEFAULT CURRENT_TIMESTAMP
);

-- Revenue table
CREATE TABLE if not exists revenues
(
    id           UUID PRIMARY KEY,
    revenue_date DATE           NOT NULL,
    amount       DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    currency     VARCHAR(3)     NOT NULL DEFAULT 'BRL',
    sector_id    VARCHAR(50)    NOT NULL REFERENCES sectors (code),
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP      NULL     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (revenue_date, sector_id)
);

-- Create indexes for better performance
CREATE INDEX if not exists idx_spots_sector_id ON spots (sector_id);
CREATE INDEX if not exists idx_spots_occupied ON spots (occupied);
CREATE INDEX if not exists idx_parking_events_vehicle_id ON parking_events (vehicle_id);
CREATE INDEX if not exists idx_parking_events_spot_id ON parking_events (spot_id);
CREATE INDEX if not exists idx_parking_events_event_time ON parking_events (event_time);
CREATE INDEX if not exists idx_parking_sessions_vehicle_id ON parking_sessions (vehicle_id);
CREATE INDEX if not exists idx_parking_sessions_active ON parking_sessions (active);
CREATE INDEX if not exists idx_revenues_sector_id ON revenues (sector_id);
CREATE INDEX if not exists idx_revenues_revenue_date ON revenues (revenue_date);