package com.fot.ams;

import com.fot.ams.exception.AuthenticationException;
import com.fot.ams.exception.InvalidCredentialsException;
import com.fot.ams.model.User;
import com.fot.ams.model.UserRole;
import com.fot.ams.security.PasswordHasher;
import com.fot.ams.service.AuthService;
import com.fot.ams.service.SessionManager;

/**
 * TestRunner verifies core business requirements, security, and authentication flows.
 */
public class TestRunner {
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("Running Automated System & Authentication Checks");
        System.out.println("Faculty of Technology Academic Management System");
        System.out.println("=================================================");

        int passed = 0;
        int total = 0;

        AuthService authService = new AuthService();

        // Test 1: Hashing
        total++;
        String raw = "student123";
        String hash = PasswordHasher.hashPassword(raw);
        if (PasswordHasher.verify(raw, hash) && !PasswordHasher.verify("wrong", hash)) {
            System.out.println("[PASS] Test 1: SHA-256 password hashing & verification");
            passed++;
        } else {
            System.err.println("[FAIL] Test 1: SHA-256 password hashing failed");
        }

        // Test 2: Admin Login
        total++;
        try {
            User admin = authService.login("admin", "admin123");
            if (admin.getRole() == UserRole.ADMIN && "System Administrator".equals(admin.getFullName())) {
                System.out.println("[PASS] Test 2: Admin login and role assignment (" + admin.getRoleSpecificIdentifier() + ")");
                passed++;
            } else {
                System.err.println("[FAIL] Test 2: Admin user properties mismatch");
            }
        } catch (Exception e) {
            System.err.println("[FAIL] Test 2: Admin login failed: " + e.getMessage());
        }

        // Test 3: Lecturer Login
        total++;
        try {
            User lecturer = authService.login("lec_ict01", "lec123");
            if (lecturer.getRole() == UserRole.LECTURER) {
                System.out.println("[PASS] Test 3: Lecturer login (" + lecturer.getFullName() + " - " + lecturer.getRoleSpecificIdentifier() + ")");
                passed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] Test 3: Lecturer login failed: " + e.getMessage());
        }

        // Test 4: Technical Officer Login
        total++;
        try {
            User to = authService.login("to_ict01", "to123");
            if (to.getRole() == UserRole.TECHNICAL_OFFICER) {
                System.out.println("[PASS] Test 4: Technical Officer login (" + to.getFullName() + ")");
                passed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] Test 4: Technical Officer login failed: " + e.getMessage());
        }

        // Test 5: Undergraduate (Student Group 12 Leader - Shahim) Login
        total++;
        try {
            User ug = authService.login("TG/2024/2105", "student123");
            if (ug.getRole() == UserRole.UNDERGRADUATE && "Mohamad Shahim".equals(ug.getFullName())) {
                System.out.println("[PASS] Test 5: Undergraduate login (" + ug.getFullName() + " - " + ug.getRoleSpecificIdentifier() + ")");
                passed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] Test 5: Undergraduate login failed: " + e.getMessage());
        }

        // Test 6: Invalid Credentials Exception (FR-AUTH-04)
        total++;
        try {
            authService.login("admin", "wrongpassword");
            System.err.println("[FAIL] Test 6: Invalid credentials did not throw exception");
        } catch (InvalidCredentialsException e) {
            if (e.getMessage().equals(InvalidCredentialsException.DEFAULT_MESSAGE)) {
                System.out.println("[PASS] Test 6: Invalid credentials properly rejected per FR-AUTH-04: \"" + e.getMessage() + "\"");
                passed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] Test 6: Unexpected exception: " + e.getMessage());
        }

        // Test 7: Empty credentials validation (NFR-02)
        total++;
        try {
            authService.login("", "");
            System.err.println("[FAIL] Test 7: Empty credentials did not fail validation");
        } catch (AuthenticationException e) {
            System.out.println("[PASS] Test 7: Empty field validation caught: " + e.getMessage());
            passed++;
        } catch (Exception e) {
            System.err.println("[FAIL] Test 7: Unexpected exception: " + e.getMessage());
        }

        // Test 8: Session Logout (FR-AUTH-05)
        total++;
        authService.logout();
        if (!SessionManager.getInstance().isLoggedIn()) {
            System.out.println("[PASS] Test 8: Session termination / logout (FR-AUTH-05)");
            passed++;
        } else {
            System.err.println("[FAIL] Test 8: Session still active after logout");
        }

        // Test 9: Register New Undergraduate & Immediate Login
        total++;
        try {
            com.fot.ams.model.Undergraduate newUg = new com.fot.ams.model.Undergraduate(
                0, "TG/2024/2999", "", "Naveen Silva", "naveen@fot.ruh.ac.lk", "+94 77 999 8888",
                "DICT", "TG/2024/2999", "B09", "Active", 2024
            );
            authService.register(newUg, "pass1234", "pass1234");
            User loggedIn = authService.login("TG/2024/2999", "pass1234");
            if (loggedIn != null && loggedIn.getRole() == UserRole.UNDERGRADUATE) {
                System.out.println("[PASS] Test 9: Individual Undergraduate registration & immediate login verified");
                passed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] Test 9: Undergraduate registration failed: " + e.getMessage());
        }

        // Test 10: Register New Lecturer
        total++;
        try {
            com.fot.ams.model.Lecturer newLec = new com.fot.ams.model.Lecturer(
                0, "lec_alwis", "", "Prof. H. Alwis", "alwis@fot.ruh.ac.lk", "+94 71 555 4433",
                "DICT", "Professor in ICT", "Distributed Cloud Computing"
            );
            authService.register(newLec, "prof123", "prof123");
            User loggedIn = authService.login("lec_alwis", "prof123");
            if (loggedIn instanceof com.fot.ams.model.Lecturer) {
                System.out.println("[PASS] Test 10: Individual Lecturer registration & designation assignment verified");
                passed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] Test 10: Lecturer registration failed: " + e.getMessage());
        }

        // Test 11: Registration Password Mismatch
        total++;
        try {
            com.fot.ams.model.Admin newAdmin = new com.fot.ams.model.Admin(
                0, "admin_test", "", "Test Admin", "test@fot.ruh.ac.lk", "+94 71 111 0000", "DICT"
            );
            authService.register(newAdmin, "passwordA", "passwordB");
            System.err.println("[FAIL] Test 11: Password mismatch did not fail");
        } catch (AuthenticationException e) {
            if (e.getMessage().contains("Passwords do not match")) {
                System.out.println("[PASS] Test 11: Password mismatch correctly rejected: " + e.getMessage());
                passed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] Test 11: Unexpected exception: " + e.getMessage());
        }

        // Test 12: Duplicate Username Rejection
        total++;
        try {
            com.fot.ams.model.Undergraduate dupUg = new com.fot.ams.model.Undergraduate(
                0, "TG/2024/2105", "", "Duplicate User", "dup@fot.ruh.ac.lk", "+94 77 000 0000",
                "DICT", "TG/2024/2105", "B09", "Active", 2024
            );
            authService.register(dupUg, "secret123", "secret123");
            System.err.println("[FAIL] Test 12: Duplicate username allowed");
        } catch (AuthenticationException e) {
            System.out.println("[PASS] Test 12: Duplicate registration properly blocked: " + e.getMessage());
            passed++;
        } catch (Exception e) {
            System.err.println("[FAIL] Test 12: Unexpected exception: " + e.getMessage());
        }

        // Test 13: Password Length Validation (< 6 chars)
        total++;
        try {
            com.fot.ams.model.TechnicalOfficer toTest = new com.fot.ams.model.TechnicalOfficer(
                0, "to_short", "", "Short Pass", "to@fot.ruh.ac.lk", "+94 77 000 0000", "DICT"
            );
            authService.register(toTest, "123", "123");
            System.err.println("[FAIL] Test 13: Short password allowed");
        } catch (AuthenticationException e) {
            System.out.println("[PASS] Test 13: Short password rejected (< 6 chars): " + e.getMessage());
            passed++;
        } catch (Exception e) {
            System.err.println("[FAIL] Test 13: Unexpected exception: " + e.getMessage());
        }

        System.out.println("=================================================");
        System.out.println("Result: " + passed + " / " + total + " tests passed successfully!");
        System.out.println("=================================================");

        if (passed == total) {
            System.exit(0);
        } else {
            System.exit(1);
        }
    }
}
