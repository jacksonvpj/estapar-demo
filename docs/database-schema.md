# Database Schema (DER Model)

```mermaid
erDiagram
    GARAGE {
        uuid id PK
        timestamp created_at
        timestamp updated_at
    }

    SECTOR {
        string code PK
        float base_price
        int max_capacity
        time open_hour
        time close_hour
        int duration_limit_minutes
        uuid garage_id FK
        timestamp created_at
        timestamp updated_at
    }

    SPOT {
        int id PK
        float latitude
        float longitude
        uuid sector_id FK
        timestamp created_at
        timestamp updated_at
    }

    VEHICLE {
        uuid id PK
        string license_plate
        timestamp created_at
        timestamp updated_at
    }

    PARKING_EVENT {
        uuid id PK
        string event_type
        timestamp event_time
        uuid vehicle_id FK
        uuid spot_id FK "nullable"
        timestamp created_at
    }

    PARKING_SESSION {
        uuid id PK
        timestamp entry_time
        timestamp parked_time
        timestamp exit_time "nullable"
        decimal price "nullable"
        decimal applied_price_factor "nullable"
        boolean active
        uuid vehicle_id FK
        uuid parked_spot_id FK "nullable"
        timestamp created_at
        timestamp updated_at
    }

    REVENUE {
        uuid id PK
        date revenue_date
        float amount
        string currency
        uuid sector_id FK
        timestamp created_at
        timestamp updated_at
    }

    GARAGE ||--o{ SECTOR: contains
    SECTOR ||--o{ SPOT: contains
    SECTOR ||--o{ REVENUE: generates
    VEHICLE ||--o{ PARKING_EVENT: triggers
    VEHICLE ||--o{ PARKING_SESSION: has
    SPOT ||--o{ PARKING_EVENT: hosts
    SPOT ||--o{ PARKING_SESSION: hosts_entry
    SPOT ||--o{ PARKING_SESSION: hosts_parked
    SPOT ||--o{ PARKING_SESSION: hosts_exit
    SECTOR ||--o{ PARKING_SESSION: belongs_to
```

## Entity Descriptions

### GARAGE

Represents the entire parking garage facility.

### SECTOR

Represents a section of the garage with specific pricing and capacity rules.

### SPOT

Represents an individual parking spot with its geolocation.

### VEHICLE

Represents a vehicle identified by its license plate.

### PARKING_EVENT

Represents events related to vehicles (entry, parked, exit).

### PARKING_SESSION

Represents a complete parking session from entry to exit, including pricing information.

### REVENUE

Represents the daily revenue for each sector.

## Relationships

- A Garage contains multiple Sectors
- A Sector contains multiple Spots
- A Sector generates multiple Revenue records (daily)
- A Vehicle triggers multiple Parking Events
- A Vehicle has multiple Parking Sessions
- A Spot hosts multiple Parking Events
- A Spot hosts multiple Parking Sessions (entry, parked, exit)
- A Sector belongs to multiple Parking Sessions