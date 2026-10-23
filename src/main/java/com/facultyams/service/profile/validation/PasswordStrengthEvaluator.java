package com.facultyams.service.profile.validation;

import java.util.List;

/** Evaluates password strength with independent, composable validation strategies. */
public final class PasswordStrengthEvaluator {
    private final List<PasswordRule> rules = List.of(
            new MinimumLengthRule(), new UppercaseRule(), new LowercaseRule(),
            new NumberRule(), new SymbolRule(), new DifferentFromCurrentRule());

    /** Returns labels in the same order as the evaluated requirements. */
    public List<String> ruleLabels() {
        return rules.stream().map(PasswordRule::label).toList();
    }

    /** Returns the current satisfaction state of each password requirement. */
    public boolean[] evaluate(char[] candidate, char[] current) {
        boolean[] result = new boolean[rules.size()];
        for (int i = 0; i < rules.size(); i++) {
            result[i] = rules.get(i).isSatisfied(candidate, current);
        }
        return result;
    }

    private static final class MinimumLengthRule implements PasswordRule {
        @Override public String label() { return "8+ characters"; }
        @Override public boolean isSatisfied(char[] candidate, char[] current) { return candidate.length >= 8; }
    }

    private static final class UppercaseRule implements PasswordRule {
        @Override public String label() { return "Uppercase letter"; }
        @Override public boolean isSatisfied(char[] candidate, char[] current) {
            return contains(candidate, Character::isUpperCase);
        }
    }

    private static final class LowercaseRule implements PasswordRule {
        @Override public String label() { return "Lowercase letter"; }
        @Override public boolean isSatisfied(char[] candidate, char[] current) {
            return contains(candidate, Character::isLowerCase);
        }
    }

    private static final class NumberRule implements PasswordRule {
        @Override public String label() { return "Number"; }
        @Override public boolean isSatisfied(char[] candidate, char[] current) {
            return contains(candidate, Character::isDigit);
        }
    }

    private static final class SymbolRule implements PasswordRule {
        @Override public String label() { return "Symbol"; }
        @Override public boolean isSatisfied(char[] candidate, char[] current) {
            for (char c : candidate) if (!Character.isLetterOrDigit(c)) return true;
            return false;
        }
    }

    private static final class DifferentFromCurrentRule implements PasswordRule {
        @Override public String label() { return "Different from current"; }
        @Override public boolean isSatisfied(char[] candidate, char[] current) {
            if (candidate.length != current.length) return true;
            int difference = 0;
            for (int i = 0; i < candidate.length; i++) difference |= candidate[i] ^ current[i];
            return difference != 0;
        }
    }

    private static boolean contains(char[] value, java.util.function.IntPredicate predicate) {
        for (char c : value) if (predicate.test(c)) return true;
        return false;
    }
}
