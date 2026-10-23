# Student Result Management System (SRMS) - Undergraduate Portal

Modern Java Swing Academic Management System with a SaaS dashboard aesthetic based on the SRMS UI design.

---

## 🌟 Included Undergraduate Operations

1. **Undergraduate Dashboard**
   - Welcome banner with vector illustration & greeting.
   - 5 KPI Metric Cards: **CGPA (8.45)**, **Courses Enrolled (6)**, **Exams Completed (4)**, **Overall Rank (12 of 120)**, **Attendance (87%)**.
   - Recent Results Table with color-coded Grade badges (A+, A, B+).
   - Notice Board with official circulars and instant full modal view.
   - Mini Interactive Academic Calendar (May 2024).
   - 2x3 Quick Access navigation panel.
   - Circular Performance Donut Chart (84% Overall Performance).

2. **Course Enrollment**
   - Credit limit tracker & progress bar (`18 / 20 Credits Enrolled`).
   - List of currently enrolled courses with real-time `Drop` option.
   - List of available elective & core modules with real-time `Enroll` option.

3. **Course Materials**
   - Filter by course module (Data Structures, DBMS, OS, Networks, Software Engineering).
   - Search materials by keyword.
   - Download lecture slides, assignments, lab manuals, and past exam papers.

4. **Notices**
   - Categorized announcements (Examinations, Academic, General, Events).
   - Search notices.
   - Read full circulars in a clean reader dialog.

5. **Timetable**
   - Compact day pills, live session state/countdown, search, and class-type filtering.
   - Persisted personal events and reminder preferences, clash warnings, and scheduled notifications.
   - Export of the selected day or full week to PDF without requiring an external PDF library.

6. **Student Results**
   - Cumulative GPA breakdown & honors standing ("Dean's List").
   - Semester filter (Semester 5, 4, 3).
   - Comprehensive results table with course code, subject name, exam type, grade badges, score, credits, and status.
   - Print Academic Transcript action.

7. **Student Profile**
   - Responsive, scrollable student summary and aligned contact/academic information cards, with the supplied circular profile photo.
   - Edit mode with inline validation, Sri Lankan phone formatting, dirty-state confirmation, photo crop/replace/remove, and asynchronous save.
   - Password change dialog with live strength rules and salted SHA-256 password hashes.
   - GPA is validated and presented consistently on the 4.00 scale.

8. **Student Attendance View**
   - Overall attendance gauge (87%) with exam eligibility indicator (>= 80% mandatory policy).
   - Course-by-course breakdown of total, attended, absent sessions, and eligibility status.

9. **Student Medical View**
   - Medical submissions table with approval status badges (`Approved`, `Pending`, `Rejected`).
   - **Submit New Medical Certificate** dialog to record medical leaves.

---

## Timetable implementation

The timetable is implemented in `com.facultyams.ui.views.TimetableView` and leaves the other dashboard views unchanged. Its model and service code is in `com.facultyams.model.timetable` and `com.facultyams.service.timetable`; `FileTimetableService` supplies the CS301/CS305/CS306 sample week and persists personal events/reminder preferences to `${user.home}/.srms/timetable.properties`. The `TimetableService` interface is the replacement point for a future JDBC implementation.

The reminder scheduler uses a daemon executor and posts Swing updates on the EDT. PDF export is implemented by `PdfScheduleExporter`; no additional dependencies are required.

### Timetable classes

```text
TimetableView -> TimetableController -> TimetableService
                                |         `- FileTimetableService
                                |             `- ClassSession
                                |                 |- Lecture
                                |                 |- Practical
                                |                 |- Tutorial
                                |                 `- PersonalEvent
                                |- ReminderService -> ScheduledReminderService -> ReminderListener
                                `- Exportable -> PdfScheduleExporter
```

Build and launch from the project root with Java 17:

```powershell
$files = Get-ChildItem -Path .\src\main\java -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d .\bin $files
if ($LASTEXITCODE -eq 0) { java -cp .\bin com.facultyams.App }
```

The existing MySQL connector dependency is unchanged; the timetable implementation adds no external dependency.

## Student profile implementation

`StudentProfileView` uses the `StudentProfileService` boundary (`InMemoryStudentProfileService` currently adapts the existing application data service). `PasswordService` uses per-password salted SHA-256 hashing and constant-time hash comparison. Shared GPA bounds are in `com.facultyams.util.Constants`, and `GpaClassifier` provides the profile classification label. Reusable profile UI components and image utilities live in `com.facultyams.ui.components`. The supplied portrait is `src/main/resources/images/profile.png`; when launched directly from the repository root, image loading also checks this resources path if the resource has not been copied to the classpath.

```text
StudentProfileView
  -> StudentProfileService
       `- InMemoryStudentProfileService -> DataService -> Student
             `- PasswordService -> SaltedSha256PasswordService
  -> FormField / AvatarLabel / ProfilePhotoCropDialog
       `- ImageUtils
  -> ProfileChangeListener -> TopBarPanel avatar
  -> Constants.MAX_GPA / GpaClassifier
```

The profile implementation uses only Java 17 and Swing/AWT; no new Maven or Gradle dependencies are required.

## 🚀 How to Run

### Option 1: Run Direct Dashboard
```powershell
# From the project root directory
java -cp bin com.facultyams.App
```
Or to compile and run:
```powershell
$files = Get-ChildItem -Path .\src\main\java -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d .\bin $files
if ($LASTEXITCODE -eq 0) { java -cp .\bin com.facultyams.App }
```

### Option 2: Run with Login Screen
```powershell
java -cp bin com.facultyams.ui.LoginFrame
```

---

## 🗄️ Database

- Database Schema: [`database/schema.sql`](file:///d:/Java%20Project/Login/OOP-2026-GP-12/database/schema.sql)
- Sample Seed Data: [`database/sample_data.sql`](file:///d:/Java%20Project/Login/OOP-2026-GP-12/database/sample_data.sql)
