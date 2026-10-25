-- =============================================================================
-- DEVELOPMENT ONLY - DESTROYS ALL DATA in the faculty_ams database.
-- Never part of normal setup. Run it only on a throw-away local database, by hand:
--   mysql -u root -p < database/dev_reset_database.sql
-- then run database/schema.sql and the migrations again (see README.md).
-- =============================================================================
DROP DATABASE IF EXISTS faculty_ams;