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
   - Interactive day-by-day weekly schedule tabs (Monday through Friday).
   - Session cards showing time slots, venue (lecture halls & labs), lecturer, and session type.
   - Schedule PDF export action.

6. **Student Results**
   - Cumulative GPA breakdown & honors standing ("Dean's List").
   - Semester filter (Semester 5, 4, 3).
   - Comprehensive results table with course code, subject name, exam type, grade badges, score, credits, and status.
   - Print Academic Transcript action.

7. **Student Profile**
   - Student identity card with degree, advisor, CGPA, and registration number.
   - Editable contact information form (Personal email, phone number, address, emergency contact) with Save feature.

8. **Student Attendance View**
   - Overall attendance gauge (87%) with exam eligibility indicator (>= 80% mandatory policy).
   - Course-by-course breakdown of total, attended, absent sessions, and eligibility status.

9. **Student Medical View**
   - Medical submissions table with approval status badges (`Approved`, `Pending`, `Rejected`).
   - **Submit New Medical Certificate** dialog to record medical leaves.

---

## 🚀 How to Run

### Option 1: Run Direct Dashboard
```powershell
# From the project root directory
java -cp bin com.facultyams.App
```
Or to compile and run:
```powershell
$files = (Get-ChildItem -Recurse -Filter "*.java" src\main\java).FullName | ForEach-Object { '"{0}"' -f $_.Replace('\', '/') }; [System.IO.File]::WriteAllLines("sources.txt", $files); cmd /c "javac -d bin @sources.txt"; Remove-Item sources.txt; java -cp bin com.facultyams.App
```

### Option 2: Run with Login Screen
```powershell
java -cp bin com.facultyams.ui.LoginFrame
```

---

## 🗄️ Database

- Database Schema: [`database/schema.sql`](file:///d:/Java%20Project/Login/OOP-2026-GP-12/database/schema.sql)
- Sample Seed Data: [`database/sample_data.sql`](file:///d:/Java%20Project/Login/OOP-2026-GP-12/database/sample_data.sql)
