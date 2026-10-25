# Run guide – Admin/Core module (Shahim)

## Requirements
- **JDK 17** (the `pom.xml` targets Java 17)
- **Maven 3.8+** (needs internet the first time to download `mysql-connector-j 8.4.0`)
- **MySQL 8.x** running locally (default port 3306)
- VS Code with the *Extension Pack for Java*

## 1. Database setup
The app uses database **`faculty_ams`**.

> `database/schema.sql` and the migrations only create what is missing; they never drop or empty anything,
> so they are safe to run on an existing database. The only destructive script is
> `database/dev_reset_database.sql` (drops the whole database). It is not part of setup; run it by hand,
> and only on a throw-away development database.

**1a. Fresh install** (from the project folder, in the VS Code terminal):
```
mysql -u root -p < database/schema.sql
mysql -u root -p faculty_ams < database/migrations/001_notices_timetables.sql
mysql -u root -p faculty_ams < database/migrations/002_department_head.sql
mysql -u root -p faculty_ams < database/sample_data.sql
mysql -u root -p faculty_ams < database/migrations/001_sample_notices_timetables.sql
```
The last file (demo notices/timetables) is optional. It must run after `sample_data.sql`.

**1b. Existing database** (never drops data; safe to re-run). `schema.sql` can also be re-run safely:
```
mysql -u root -p faculty_ams < database/migrations/001_notices_timetables.sql
mysql -u root -p faculty_ams < database/migrations/002_department_head.sql
```

## 2. Database credentials
Copy `src/main/resources/db.properties.example` to `src/main/resources/db.properties` and edit
`db.url`, `db.user`, `db.password` (replace `YOUR_MYSQL_PASSWORD`). That file is git-ignored, so the password is never committed.
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
| VS Code says the build failed and will not start the app | This is intentional (`onBuildFailureProceed` is `false`, so stale classes are never launched). Run `mvn clean compile` and fix the errors it prints. |
| Every file shows as modified in `git status` | Line endings only (CRLF/LF). Use `git config core.autocrlf true` on Windows. |