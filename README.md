# FoT-AMS - Faculty of Technology Academic Management System (group OOP-2026-GP-12)

University of Ruhuna - ICT2132 Object Oriented Programming practicum. Java 17, Swing, JDBC, MySQL 8, database `faculty_ams`.
Roles: Admin, Lecturer, Technical Officer, Undergraduate.

## Requirements
- **JDK 17** (the `pom.xml` targets Java 17)
- **Maven 3.8+** (needs internet the first time to download `mysql-connector-j 8.4.0`)
- **MySQL 8.0.16 or newer** (CHECK constraints are only enforced from 8.0.16) running locally (port 3306)
- Windows PowerShell. `mysql` and `mysqldump` must be on the PATH, normally
  `C:\Program Files\MySQL\MySQL Server 8.0\bin`. Check with `mysql --version`.
- VS Code with the *Extension Pack for Java* (optional)

## Read this first - safety rules for the database
- **Never run `database/dev_reset_database.sql`** on a database you want to keep. It drops the whole database. It is not
  part of any setup step.
- Nothing below drops, empties or renames your tables. The scripts only add what is missing and **stop with a message**
  (changing nothing) when something looks incompatible.
- **Take a backup before every change** (section 1).
- **Never use `mysql --force`** with these scripts: they stop on purpose when a precondition fails.
- In PowerShell the `<` redirection (`mysql ... < file.sql`) does **not** work. Use `mysql ... -e "source <file>"` exactly as shown.
- Demo data (section 5) is for a development / throw-away database. Do not load it into a database with real records.

All commands are run from the project folder (the folder that contains `pom.xml`). Replace `<admin_user>` by a MySQL
account that may create/alter tables (for example `root`); `-p` makes MySQL ask for the password.

## 1. Backup (always first)
```powershell
New-Item -ItemType Directory -Force backups | Out-Null
$ts = Get-Date -Format "yyyyMMdd_HHmmss"
mysqldump -u <admin_user> -p --single-transaction --routines --triggers --default-character-set=utf8mb4 --result-file="backups\faculty_ams_$ts.sql" faculty_ams
(Get-Item "backups\faculty_ams_$ts.sql").Length      # must be larger than 0
```
`--result-file` is used on purpose: `mysqldump > file.sql` in Windows PowerShell 5.1 writes a UTF-16 file that MySQL cannot read back.
Keep the backup outside the project folder as well (it contains password hashes). `backups/` is not meant to be committed.

## 2. Install the database

### 2a. Fresh install (the database does not exist yet)
```powershell
mysql -u <admin_user> -p -e "source database/schema.sql"
foreach ($f in "000_schema_migrations","001_notices_timetables","002_department_head","003_attendance_medical","005_lecturer_module","007_student_module","009_technical_officer_medical","010_reconcile_schema") {
  Write-Host "== $f"
  mysql -u <admin_user> -p faculty_ams --table -e "source database/migrations/$f.sql"
  if ($LASTEXITCODE -ne 0) { Write-Host "STOPPED at $f - read the message above (section 7)"; break }
}
```
`schema.sql` already contains the final shape of every table, so on a fresh database the migrations find everything in place
and only record themselves in the table `schema_migrations`. (MySQL asks for the password for every command; use a
`~/.my.cnf` option file if that is annoying.)

### 2b. Upgrade an existing database (data already in it)
1. **Backup** (section 1).
2. **Audit before** - read-only, changes nothing, a missing table does not stop it:
   ```powershell
   mysql -u <admin_user> -p faculty_ams --table -e "source database/audit_existing_database.sql"
   ```
   Read every `FAIL` and `WARN` row. In particular:
   `01 tables left behind by the OLD migration 005` or `columns of marks/course_materials ... FAIL` means a table of another layout exists - go to
   section 7 (Recovery) **before** continuing.
3. **Try it on a copy first** (recommended; the scripts contain `USE faculty_ams;`, so a helper rewrites that line):
   ```powershell
   mysql -u <admin_user> -p -e "CREATE DATABASE faculty_ams_trial CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
   mysql -u <admin_user> -p faculty_ams_trial -e "source backups/faculty_ams_<timestamp>.sql"
   function Invoke-FotSql($File, $Db = "faculty_ams") {
     $tmp = Join-Path $env:TEMP ("fotams_" + [IO.Path]::GetFileName($File))
     (Get-Content $File -Raw) -replace 'USE faculty_ams;', "USE $Db;" | Set-Content -Path $tmp -Encoding ascii
     mysql -u <admin_user> -p $Db --table -e ("source " + ($tmp -replace '\\','/'))
   }
   Invoke-FotSql database\audit_existing_database.sql faculty_ams_trial
   ```
   Then run the migrations of the next step with `Invoke-FotSql <file> faculty_ams_trial`. Never run `schema.sql` on the trial copy.
4. **Migrate** the real database, in this order (each script is repeatable):
   ```powershell
   foreach ($f in "000_schema_migrations","001_notices_timetables","002_department_head","003_attendance_medical","005_lecturer_module","007_student_module","009_technical_officer_medical","010_reconcile_schema") {
     Write-Host "== $f"
     mysql -u <admin_user> -p faculty_ams --table -e "source database/migrations/$f.sql"
     if ($LASTEXITCODE -ne 0) { Write-Host "STOPPED at $f - read the message above (section 7)"; break }
   }
   ```
   Do **not** run `004`, `006`, `008` (demo data) or `schema.sql` on a database with real data.
5. **Audit after** and verify (same commands as step 2 and section 6). Rows with `WARNING:` printed by `010`
   mean a unique key / foreign key / CHECK was **not** added because existing rows violate it; the audit lists those rows;
   correct them and run `010` again.

### What each script does
| Order | Script | Purpose |
|---|---|---|
| 0 | `schema.sql` | Authoritative schema of all tables in their final shape (fresh install; creates the database). |
| 1 | `migrations/000_schema_migrations.sql` | Creates the history table `schema_migrations` (every numbered script records itself: `run_count`, `last_run_at`). |
| 2 | `001_notices_timetables.sql`, `002_department_head.sql` | Notices / timetables tables; `departments.head_user_id`. |
| 3 | `003_attendance_medical.sql` | `course_component`, `session_attendance`, `medical`. Refuses an existing table that lacks its key columns. |
| 4 | `005_lecturer_module.sql` | `enrollments`, `evaluation_component`, `component_marks`, `marks`, `course_materials` (one layout shared by the Lecturer and Undergraduate modules). **Does not rename anything**; stops if `marks` / `course_materials` have another layout. |
| 5 | `007_student_module.sql` | `enrollments.status/dropped_at`, `notices.category/priority`, `notice_reads`, the extra `medical` columns, `medical.reviewed_by` may be NULL. |
| 6 | `009_technical_officer_medical.sql` | `medical.submitted_by`, evidence, rejection and decision columns, status CHECK. The one-time conversion of old rows runs only the first time. |
| 7 | `010_reconcile_schema.sql` | Adds missing unique keys, foreign keys, CHECK ranges (marks 0-100 ...) and indexes - **only where the existing rows allow it**, otherwise prints a WARNING and skips. |
| demo | `sample_data.sql`, `001_sample_notices_timetables.sql`, `004`, `006`, `008` | Development demo data (section 5). |

## 3. Database credentials (and the application user)
1. Copy `src/main/resources/db.properties.example` to `src/main/resources/db.properties` and set `db.url`, `db.user`, `db.password`.
   That file is git-ignored. Alternative: environment variables `FOTAMS_DB_URL`, `FOTAMS_DB_USER`, `FOTAMS_DB_PASSWORD` (they win over the file).
   There is **no default user or password**: without either source the application says "The database is not configured".
2. **Create the application database user** (do not run the application as `root`). In `mysql -u root -p`:
   ```sql
   CREATE USER 'faculty_user'@'localhost' IDENTIFIED BY 'choose-a-strong-password';
   GRANT SELECT, INSERT, UPDATE, DELETE ON faculty_ams.* TO 'faculty_user'@'localhost';
   ```
   and put that user in `db.properties`. Use an administrator account (not `faculty_user`) only for the scripts in section 2.
3. **The project ZIP contained a local `db.properties` with the MySQL `root` user and a password.** Treat that password as exposed:
   change it (`ALTER USER 'root'@'localhost' IDENTIFIED BY '<new password>';`) and do not share `db.properties` again.

## 4. Compile, check, run
```powershell
mvn clean compile
```
The project has no JUnit tests. Its self-checks are plain classes with a fake JDBC driver, so they need no MySQL
(add `-Djava.awt.headless=true` on a machine without a display):
```powershell
java -cp target/classes com.facultyams.CoreChecks
java -cp target/classes com.facultyams.AdminDashboardChecks
java -cp target/classes com.facultyams.NoticeTimetableChecks
java -cp target/classes com.facultyams.config.DatabaseConfigChecks
java -cp target/classes com.facultyams.ui.admin.FormFieldChecks
```
Notes from the last run (JDK 21 with `javac --release 17`; Maven was not available there):
`CoreChecks` reports one failure, *"found the 30 seeded users (found 0)"*, when no demo database is connected - this is expected;
`NoticeTimetableChecks` has one timing-dependent check (*"shell pages are not loaded until opened"*) that fails in some runs
and did so before the corrections too.

Checks that **do** need MySQL (after section 2 and the demo data of section 5): `com.facultyams.LecturerDbCheck`, `com.facultyams.LecturerChecks`,
`com.facultyams.StudentModuleChecks` (needs `database/test/student_checks_fixture.sql`, section 5).

Run the application (needs MySQL from section 2): open `Main.java` and click **Run**, or
```powershell
mvn dependency:copy-dependencies -DoutputDirectory=target/lib
java -cp "target/classes;target/lib/*" com.facultyams.Main
```
(`run-admin.bat` does the same.) Create a password hash: `java -cp target/classes com.facultyams.security.PasswordUtil <password>`.

## 5. Demo (sample) data - development database only
```powershell
$demo = "sample_data.sql","migrations/001_sample_notices_timetables.sql","migrations/004_attendance_test_data.sql","migrations/006_lecturer_test_data.sql","migrations/008_student_test_data.sql"
foreach ($f in $demo) { Write-Host "== $f"; mysql -u <admin_user> -p faculty_ams --table -e "source database/$f"; if ($LASTEXITCODE -ne 0) { break } }
```
Run it **after** section 2. Every script is repeatable: it inserts only rows that do not exist yet, matches people by
username + e-mail (`@fot.example.lk`), never updates a real account and never deletes attendance or marks.
If a real account already uses a demo username, `sample_data.sql` lists it ("skipped: ...") and leaves it alone.

| Role | Username(s) | Password |
|---|---|---|
| ADMIN | `admin` | `admin123` |
| LECTURER (5) | `lec.perera`, `lec.silva`, `lec.fernando`, `lec.jayasuriya`, `lec.bandara` | `lecturer123` |
| TECHNICAL_OFFICER (4) | `to.kumara`, `to.dissanayake`, `to.rathnayake`, `to.wijesinghe` | `officer123` |
| STUDENT (20) | `tg1001`-`tg1016` (REGULAR, batch B09), `tg0950`, `tg0951` (REPEAT), `tg0960`, `tg0961` (BATCH_MISSED) | `student123` |

Change or remove these accounts before real use. The course list, timetable slots, session dates, assessment schemes and
weights in the demo data are **placeholders**, not the official B09 timetable or the module coordinators' criteria. Replace
them with the official ones (Admin > Courses, Lecturer > Evaluation Components) before the presentation.

For `StudentModuleChecks` only (throw-away database that contains **only** demo accounts; the script refuses to run otherwise):
```powershell
mysql -u <admin_user> -p faculty_ams -e "source database/test/student_checks_fixture.sql"
```

## 6. Verify
Read-only; one result set `check_name | status | detail` (`PASS`, `FAIL`, `WARN`, `INFO`, `SKIP`) followed by reports:
```powershell
mysql -u <admin_user> -p faculty_ams --table -e "source database/audit_existing_database.sql"
mysql -u <admin_user> -p faculty_ams --table -e "source database/verify_assignment_data.sql"
```
`audit_existing_database.sql`: required tables and columns, migration history, duplicates, orphaned foreign-key values,
role/subtype rows, enrollment and course consistency, attendance and medical consistency, marks/grade/GPA consistency.
`verify_assignment_data.sql`: the assignment rules - 1 admin, >= 5 lecturers, >= 4 technical officers, >= 20 undergraduates incl.
REPEAT and BATCH_MISSED; 15 theory + 15 practical sessions; the five attendance scenarios (> 80 %, exactly 80 %, < 80 % without medical,
> 80 % with medical, < 80 % with medical); medical tracking; marks 0-100; CA >= 40; attendance + CA eligibility; grades, final marks;
SGPA/CGPA; then Reports A-F (attendance theory/practical/combined, eligibility, SGPA, CGPA, batch summaries).

**Expected result after section 2 + the demo data of section 5** (computed with an independent Python model of the rules and by running
the scripts in an **SQLite emulation**; *not yet observed on MySQL* - if your MySQL output differs, the MySQL output is the truth, please report it):

| Item | Expected |
|---|---|
| Rows | users 30, lecturers 5, students 20, courses 8, enrollments 100, course_component 3, session_attendance 900, medical 14 (11 approved, 2 pending, 1 rejected), evaluation_component 8, component_marks 160, marks 40 (all published) |
| `verify_assignment_data.sql` | every check `PASS`; scenario rows: scenario 1: 17, 2: 7, 3: 10, 4: 2, 5: 4 student/course pairs |
| `audit_existing_database.sql` | all `PASS` except `WARN 07 courses without a lecturer in charge` (1 course, BST2112, has none in the sample data) and `WARN 03 migration recorded: ...` for scripts you did not run |
| Eligibility | ICT2113: 10 of 20 eligible; ICT2122: 15 of 20 eligible |
| Example (tg1001, ICT2113) | CA 86.25, final 84.00, overall 84.90, grade A, points 4.0, attendance 100 % |
| CGPA (published) | tg1001 4.00, tg1002 3.54, tg1003 3.00, tg1004 2.18, tg1005 3.54, tg1006 2.54, tg1007 1.70, tg1008 3.30, tg1009 0.00, tg1010 1.18, tg1011 2.30, tg1012 1.88, tg1013 2.88, tg1014 3.18, tg1015 4.00, tg1016 3.00, tg0950 2.00, tg0951 2.70, tg0960 1.30, tg0961 1.00 |

Rules (taken from the code, `AcademicRules.java` / `AttendanceService.java`): attendance % = 100 x (present + late + approved-medical absences) /
(15 x number of components) - a course with Theory and Practical counts both; eligible = CA % >= 40 **and** attendance >= 80 %;
overall = sum(mark / max x weightage) / sum(weightage) x 100; GPA = sum(grade points x credits) / sum(credits) over published results.
**The grade bands (A+ >= 85 ... E < 30) are the application's own and have NOT been checked against UGC Commission Circular No. 12-2024.**
Compare them with the circular; if they differ, change `AcademicRules.BANDS`, `GradingScheme.BANDS`, `008_student_test_data.sql` and the CASE expressions in `verify_assignment_data.sql`.

## 7. Recovery - when a script stops
A stop prints a `problem` row ("ABORT: ...") followed by an error such as *Table 'faculty_ams.ABORT__...' doesn't exist*. That error is the intended
stop signal; nothing was changed by the statement that stopped.

| Message (table name in the error) | Meaning and fix |
|---|---|
| `ABORT__<table>_table_missing__run_earlier_migrations` | A table the script needs is missing. Run the earlier scripts in the order of section 2. |
| `ABORT__<table>_missing_columns__run_003_005_007_009` | `010` found a table without a final column. Run 003, 005, 007, 009 again, then 010. |
| `ABORT__run_migration_009_before_004`, `..._007_before_006`, `..._007_before_008` | A demo script was run before its prerequisite. Run the prerequisite first. |
| `ABORT__marks_incompatible__see_README_Recovery`, `ABORT__course_materials_incompatible__...`, `ABORT__<other>_incompatible__...`, `ABORT__marks_key_types__...` | An existing table has another module's layout. **Do not force it.** Follow *Incompatible marks / course_materials* below. |
| `ABORT__fixture_refuses_real_users`, `ABORT__fixture_needs_demo_ids_11_to_16` | The test fixture was pointed at a database with real users. Do not run it there. |
| `WARNING: unique key / foreign key / CHECK ... NOT added` (printed by `010`) | Existing rows violate the rule. Run `audit_existing_database.sql`, correct or remove the listed rows yourself, run `010` again. Nothing was deleted automatically. |

### Incompatible marks / course_materials
1. Do not run anything else. Take a backup (section 1) if you have not.
2. `mysql -u <admin_user> -p faculty_ams --table -e "source database/manual/migrate_incompatible_marks_and_materials.sql"` - sections A and B are read-only: they show the existing layout, row counts, what is missing.
3. If the **old** version of `005` was run earlier, your other module's records are in `marks_other_module` / `course_materials_other_module` (the audit lists them). They were renamed, not deleted.
4. Edit the column mapping in section D of that file, then un-comment and run sections C-F one at a time. Every step has a count check; if one fails, stop and restore.
5. Re-run `005`, `007`, `010`, then the audit.

### Restore from a backup
Do **not** drop the live database to restore. Restore into a new database, compare, then point `db.url` at it (or copy the rows you need):
```powershell
mysql -u <admin_user> -p -e "CREATE DATABASE faculty_ams_restore CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
mysql -u <admin_user> -p faculty_ams_restore -e "source backups/faculty_ams_<timestamp>.sql"
```

## 8. Common errors
| Message | Fix |
|---|---|
| `The database is not configured` | Create `db.properties` or set the `FOTAMS_DB_*` variables (section 3). |
| `No suitable driver found for jdbc:mysql...` | The MySQL driver is not on the classpath. Run through VS Code/Maven or use the `target/lib/*` command. |
| `Cannot connect to the database` / `Access denied` | MySQL is not running, or `db.properties` has the wrong URL/user/password. |
| `Unknown database 'faculty_ams'` | Run section 2a. |
| `Unknown column ...` / a screen says a table is missing | Run the migrations in order (section 2); `java -cp target/classes com.facultyams.LecturerDbCheck` tells which one. |
| VS Code says the build failed and will not start the app | Intentional (`onBuildFailureProceed` is `false`). Run `mvn clean compile` and fix the errors. |
| Every file shows as modified in `git status` | Line endings only (CRLF/LF). Use `git config core.autocrlf true` on Windows. |

## 9. Project layout
`database/` - `schema.sql`, `migrations/`, `sample_data.sql`, `audit_existing_database.sql`, `verify_assignment_data.sql`, `manual/`, `test/`, `PREFLIGHT_REPORT.md` (findings and corrections);
`src/main/java/com/facultyams` - `config`, `dao`, `model`, `service`, `ui` (Swing), `security`.