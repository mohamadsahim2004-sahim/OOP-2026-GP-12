# Run guide – Admin/Core module (Shahim)

## Requirements
- **JDK 17** (the `pom.xml` targets Java 17)
- **Maven 3.8+** (needs internet the first time to download `mysql-connector-j 8.4.0`)
- **MySQL 8.x** running locally (default port 3306)
- VS Code with the *Extension Pack for Java*

## 1. Database setup
The app uses database **`faculty_ams`**.

> **WARNING:** `database/schema.sql` starts with `DROP DATABASE IF EXISTS faculty_ams`.
> Use it only on a fresh/throw-away database. If you already have data, skip it and run only the migrations (step 1b).

**1a. Fresh install** (from the project folder, in the VS Code terminal):
```
mysql -u root -p < database/schema.sql
mysql -u root -p faculty_ams < database/migrations/001_notices_timetables.sql
mysql -u root -p faculty_ams < database/migrations/002_department_head.sql
mysql -u root -p faculty_ams < database/sample_data.sql
mysql -u root -p faculty_ams < database/migrations/001_sample_notices_timetables.sql
```
The last file (demo notices/timetables) is optional. It must run after `sample_data.sql`.

**1b. Existing database** (never drops data; safe to re-run):
```
mysql -u root -p faculty_ams < database/migrations/001_notices_timetables.sql
mysql -u root -p faculty_ams < database/migrations/002_department_head.sql
```

## 2. Database credentials
Copy `src/main/resources/db.properties.example` to `src/main/resources/db.properties` and edit
`db.url`, `db.user`, `db.password`. That file is git-ignored, so the password is never committed.
Alternative: set the environment variables `FOTAMS_DB_URL`, `FOTAMS_DB_USER`, `FOTAMS_DB_PASSWORD`.
Without either, the code falls back to `localhost:3306/faculty_ams`, user `root`, empty password.

## 3. Compile, check, run (VS Code terminal, project folder)
```
mvn clean compile
```
The project has no JUnit tests. Its self-checks are plain classes with a fake database, so they need no MySQL:
```
java -cp target/classes com.facultyams.CoreChecks
java -cp target/classes com.facultyams.AdminDashboardChecks
java -cp target/classes com.facultyams.NoticeTimetableChecks
```
Run the app (needs MySQL from step 1). Easiest: open `Main.java` and click **Run** above `main`. From the terminal (Windows):
```
mvn dependency:copy-dependencies -DoutputDirectory=target/lib
java -cp "target/classes;target/lib/*" com.facultyams.Main
```
(On macOS/Linux use `:` instead of `;` in the classpath.)

## 4. Login
`database/sample_data.sql` documents these test accounts (loaded by step 1a):

| Role | Username | Password |
|---|---|---|
| ADMIN | `admin` | `admin123` |

Other sample roles are listed in the header of `sample_data.sql`. Create a new hash with
`java -cp target/classes com.facultyams.security.PasswordUtil <password>`.

## 5. Common errors
| Message | Fix |
|---|---|
| `No suitable driver found for jdbc:mysql...` | The MySQL driver is not on the classpath. Run through VS Code/Maven, or use the `target/lib/*` command above. |
| `Cannot connect to the database` / `Access denied` | MySQL is not running, or `db.properties` has the wrong URL/user/password. |
| `Unknown database 'faculty_ams'` | Run step 1a. |
| Notices/Timetables screen says a table is missing | Run `001_notices_timetables.sql`. |
| `Unknown column 'head_user_id'` | Run `002_department_head.sql`. |
| App runs old code after an edit | `.vscode/settings.json` has `onBuildFailureProceed: true`, which lets VS Code launch stale classes when the build fails. Run `mvn clean compile` and read the errors. |
| Every file shows as modified in `git status` | Line endings only (CRLF/LF). Use `git config core.autocrlf true` on Windows. |
