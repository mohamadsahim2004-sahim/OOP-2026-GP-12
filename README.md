# OOP-2026-GP-12

## Technical Officer module

**Database (run once, in this order, after `schema.sql` / `sample_data.sql`):**
`database/migrations/003_attendance_medical.sql`, optionally `004_attendance_test_data.sql`, then
`007_technical_officer_medical.sql` (adds medical evidence, reviewer/decision date, rejection reason; keeps existing rows).

**Run:** `mvn clean compile` then start `com.facultyams.Main` from your IDE.
**Login:** `to.kumara` / `officer123` (other officers `to.dissanayake`, `to.rathnayake`, `to.wijesinghe`, same password).

**Attendance rule (one place: `AttendanceService`):** attendance % = (present + late + approved medical) / 15 sessions per
subject; with a date range chosen, the divisor is the sessions recorded in that range. 80% or more is eligible.
An approved medical only counts while its session is still marked Absent.

**Medical documents** are stored outside the project in `$FOTAMS_UPLOAD_DIR` (default `~/.fotams/medical-evidence`)
under random names; only PDF/JPG/PNG up to 5 MB are accepted and only Technical Officers (or the owning student)
can open them.

**Checks without a database:** `java -cp target/classes com.facultyams.TechnicalOfficerChecks`