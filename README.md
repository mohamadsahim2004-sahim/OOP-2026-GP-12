# Faculty of Technology Academic Management System (FOT-AMS)
**University of Ruhuna — Department of Information and Communication Technology**  
*Course Unit: ICT2132 – Object Oriented Programming Practicum (Level II – Semester I | Mini Project)*  
*Group 12: Mohamad Shahim (TG/2024/2105), M. Aamir (TG/2024/2104), F. Zumra (TG/2024/2093), M. Mariyam (TG/2024/2076)*

---

## 🎨 Modern UI Implementation
Designed directly from the provided reference image:
- **Left Hero Card**: Multi-stop radial/linear mesh gradient (Deep Sapphire Blue `#121E87`, Royal Purple `#5830C3`, Soft Violet `#9356EB`, and Sky Cyan Bloom `#56C0F5`).
- **Brand Emblems**: Custom Java2D anti-aliased 8-point Asterisk logo on both the gradient hero card and above the form headline.
- **Login Screen ("Sign in to your account")**: Clean, minimalist white card with rounded inputs, interactive eye password toggle, glowing gradient primary button, divider, and quick role demo badges.
- **Registration Screen ("Create an account")**: Dynamic multi-role registration page with individual field sets for **Undergraduate**, **Lecturer**, **Technical Officer**, and **Administrator**.
- **Two-Way Navigation**:
  - On Login: Click *"Don't have an account? **Sign up**"* to switch to Registration.
  - On Registration: Click *"Already have an account? **Sign in**"* to switch to Login.
- **Role Dashboard Redirection**: Directly fulfills **FR-AUTH-03** by showing role-specific dashboard views with permissions mapped per **SRS Section 3**.

---

## 🧱 OOP Implementation Mapping (SRS Section 9)

| OOP Concept | Implementation in Project | File Reference |
| :--- | :--- | :--- |
| **Classes & Objects** | Concrete role entities and data models | [`Admin.java`](src/com/fot/ams/model/Admin.java), [`Lecturer.java`](src/com/fot/ams/model/Lecturer.java), [`TechnicalOfficer.java`](src/com/fot/ams/model/TechnicalOfficer.java), [`Undergraduate.java`](src/com/fot/ams/model/Undergraduate.java) |
| **Inheritance** | All specific user classes extend the abstract `User` base class | [`User.java`](src/com/fot/ams/model/User.java) |
| **Abstraction** | Abstract methods `getDashboardTitle()`, `getPermissions()`, `getRoleSpecificIdentifier()` enforced across all roles | [`User.java`](src/com/fot/ams/model/User.java) |
| **Polymorphism** | Dynamic dispatch and method overriding of `getDashboardTitle()` and `getPermissions()` | Role models in `com.fot.ams.model` |
| **Encapsulation** | Private instance fields with strict accessors (getters/setters); business logic inside services | [`User.java`](src/com/fot/ams/model/User.java), [`AuthService.java`](src/com/fot/ams/service/AuthService.java) |
| **Exception Handling** | Custom hierarchy (`AuthenticationException`, `InvalidCredentialsException`, `DatabaseException`); defensive try-catch | `com.fot.ams.exception` |
| **Database Handling** | DAO Pattern with JDBC `PreparedStatement` + offline resilient seed fallback | [`UserDAO.java`](src/com/fot/ams/dao/UserDAO.java), [`UserDAOImpl.java`](src/com/fot/ams/dao/UserDAOImpl.java) |
| **GUI** | Java Swing & Java2D anti-aliased custom painting with layout managers | `com.fot.ams.ui.*` |

---

## 🔑 Preloaded Test Credentials

You can click any of the 4 demo badges on the login screen for instant one-click credential auto-fill, or enter them manually:

| Role | Username / ID | Password | Name / Details |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `admin123` | System Administrator (Full privileges) |
| **Lecturer** | `lec_ict01` | `lec123` | Dr. K. L. Perera (Senior Lecturer Gr. I) |
| **Technical Officer** | `to_ict01` | `to123` | Mr. S. Fernando (Attendance & Medicals) |
| **Undergraduate** | `TG/2024/2105` | `student123` | Mohamad Shahim (Batch B09 - Group 12 Leader) |
| **Undergraduate** | `TG/2024/2104` | `student123` | M. Aamir (Batch B09) |
| **Undergraduate** | `TG/2024/2093` | `student123` | F. Zumra (Batch B09) |
| **Undergraduate** | `TG/2024/2076` | `student123` | M. Mariyam (Batch B09) |

*All passwords are encrypted with SHA-256 in accordance with SRS NFR-05.*

---

## 🚀 How to Run

### Option 1: PowerShell
```powershell
.\run.ps1
```

### Option 2: Windows Batch / Double-Click
Double-click `run.bat` or run:
```cmd
run.bat
```

### Option 3: Manual Compilation
```powershell
javac -encoding UTF-8 -d bin -sourcepath src src/com/fot/ams/Main.java
java -cp bin com.fot.ams.Main
```

### Option 4: Run Automated Tests
```powershell
java -cp bin com.fot.ams.TestRunner
```

---

## 🗄️ Database Setup (Optional)
To attach to a live MySQL instance, execute [`database/schema.sql`](database/schema.sql) in MySQL Workbench or MySQL CLI:
```sql
mysql -u root -p < database/schema.sql
```
*Note: If MySQL is not running, the application will automatically fall back to its internal seeded repository so it is guaranteed to run cleanly during viva presentations and grading.*
