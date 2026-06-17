# Archived MySQL Deployment Path

The current database migration chain is `sql/flyway`.

`sql/kingbase_schema.sql` is retained as the Kingbase baseline schema used to generate the first Flyway migration.

The previous MySQL init script has been archived at `deploy/archive/mysql/001_schema.sql` for historical reference only. New environments should initialize through the Flyway migration chain used by `deploy/docker-compose.yml` and `scripts/init-db.sh`.
