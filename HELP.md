# Getting Started
## Development
Run .\gradlew -PisLocal :bootRun and env variable spring_profiles_active=local
In local development postgresql is used

## Build
Run .\gradlew :build
On prod in memory derby is used.

# Initialize data base command
```sql
CREATE USER zaval_backend WITH PASSWORD '1';

CREATE DATABASE zaval_backend with
    owner zaval_backend
    TEMPLATE template0
    ENCODING 'UTF8'
        LC_COLLATE = 'ru_RU.UTF-8'
        LC_CTYPE = 'ru_RU.UTF-8';
```

# Restore database from dump
psql -U username -d dbname -f dumpfile
