package com.facultyams.service;

import com.facultyams.dao.EvaluationComponentDAO;
import com.facultyams.dao.MarksDAO;
import com.facultyams.exception.AuthorizationException;
import com.facultyams.exception.DatabaseException;
import com.facultyams.exception.ValidationException;
import com.facultyams.model.Course;
import com.facultyams.model.EvaluationComponent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Create, view, edit and delete the assessment components of the lecturer's own courses.
 *
 * <p>Rules that protect the results:
 * <ul>
 *   <li>weightages of one course may never add up to more than 100 %, and marks can only be
 *       entered once they add up to exactly 100 % (see {@link AcademicRules#checkScheme});</li>
 *   <li>once ANY mark has been entered for the course, components cannot be added or removed,
 *       and a component that has marks can only be renamed (its maximum, weightage and category
 *       would change every calculated result);</li>
 *   <li>nothing can change while the course has published results.</li>
 * </ul>
 */
public class EvaluationComponentService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final double MAX_MARKS_LIMIT = 9999.99;
    private static final int MAX_NAME_LENGTH = 60;

    /** The scheme of one course as shown on screen. */
    public record Scheme(Course course, List<EvaluationComponent> components,
                         AcademicRules.SchemeCheck check, int marksEntered, boolean published) {

        /** True when components can no longer be added or removed. */
        public boolean structureLocked() {
            return published || marksEntered > 0;
        }
    }

    private final LecturerAccessService access;
    private final EvaluationComponentDAO dao;
    private final MarksDAO marksDAO;

    public EvaluationComponentService() {
        this(new LecturerAccessService(), new EvaluationComponentDAO(), new MarksDAO());
    }

    public EvaluationComponentService(LecturerAccessService access, EvaluationComponentDAO dao, MarksDAO marksDAO) {
        this.access = access;
        this.dao = dao;
        this.marksDAO = marksDAO;
    }

    public Scheme getScheme(int courseId) throws DatabaseException {
        Course course = access.requireOwnedCourse(courseId);
        List<EvaluationComponent> components = dao.findByCourse(courseId);
        return new Scheme(course, components, AcademicRules.checkScheme(components),
                dao.countMarksForCourse(courseId), marksDAO.countPublished(courseId) > 0);
    }

    public EvaluationComponent addComponent(int courseId, String name, String category,
                                            double maxMarks, double weightage)
            throws DatabaseException, ValidationException {
        Scheme scheme = getScheme(courseId);
        requireNotPublished(scheme);
        if (scheme.marksEntered() > 0) {
            throw new ValidationException(
                    "Marks have already been entered for this course, so components can no longer be added.");
        }
        EvaluationComponent component = validated(scheme.course(), 0, name, category, maxMarks, weightage);
        component.setCourseId(courseId);
        requireUniqueName(courseId, component);
        requireTotalWithinLimit(scheme.components(), 0, component.getWeightage());
        component.setEvalComponentId(dao.insert(component));
        return component;
    }

    public void updateComponent(int componentId, String name, String category,
                                double maxMarks, double weightage)
            throws DatabaseException, ValidationException {
        EvaluationComponent existing = requireOwnedComponent(componentId);
        Scheme scheme = getScheme(existing.getCourseId());
        requireNotPublished(scheme);

        EvaluationComponent updated =
                validated(scheme.course(), componentId, name, category, maxMarks, weightage);
        updated.setCourseId(existing.getCourseId());
        requireUniqueName(existing.getCourseId(), updated);

        boolean hasMarks = dao.countMarksForComponent(componentId) > 0;
        boolean structuralChange = !existing.getCategory().equals(updated.getCategory())
                || Double.compare(existing.getMaxMarks(), updated.getMaxMarks()) != 0
                || Double.compare(existing.getWeightage(), updated.getWeightage()) != 0;
        if (hasMarks && structuralChange) {
            throw new ValidationException("Marks have already been entered for \""
                    + existing.getComponentName() + "\", so only its name can be changed.");
        }
        requireTotalWithinLimit(scheme.components(), componentId, updated.getWeightage());
        dao.update(updated);
    }

    public void deleteComponent(int componentId) throws DatabaseException, ValidationException {
        EvaluationComponent existing = requireOwnedComponent(componentId);
        Scheme scheme = getScheme(existing.getCourseId());
        requireNotPublished(scheme);
        if (scheme.marksEntered() > 0) {
            throw new ValidationException(
                    "Marks have already been entered for this course, so components can no longer be removed.");
        }
        dao.delete(componentId);
    }

    // ------------------------------------------------------------------ helpers

    /** The component, but only if it belongs to one of the logged-in lecturer's courses. */
    private EvaluationComponent requireOwnedComponent(int componentId) throws DatabaseException {
        access.currentLecturerId();
        EvaluationComponent existing = componentId <= 0 ? null : dao.findById(componentId);
        if (existing == null) {
            throw new AuthorizationException("This evaluation component is not in one of your courses.");
        }
        try {
            access.requireOwnedCourse(existing.getCourseId());
        } catch (AuthorizationException e) {
            throw new AuthorizationException("This evaluation component is not in one of your courses.");
        }
        return existing;
    }

    private static void requireNotPublished(Scheme scheme) throws ValidationException {
        if (scheme.published()) {
            throw new ValidationException(
                    "Results of this course are published. Unpublish them before changing the evaluation scheme.");
        }
    }

    private void requireUniqueName(int courseId, EvaluationComponent component)
            throws DatabaseException, ValidationException {
        if (dao.nameExists(courseId, component.getComponentName(), component.getEvalComponentId())) {
            throw new ValidationException("This course already has a component named \""
                    + component.getComponentName() + "\".");
        }
    }

    private static void requireTotalWithinLimit(List<EvaluationComponent> existing, int replacingId,
                                                double newWeight) throws ValidationException {
        BigDecimal total = BigDecimal.valueOf(newWeight);
        for (EvaluationComponent c : existing) {
            if (c.getEvalComponentId() != replacingId) {
                total = total.add(BigDecimal.valueOf(c.getWeightage()));
            }
        }
        if (total.compareTo(HUNDRED) > 0) {
            throw new ValidationException("Weightages would add up to "
                    + total.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%. The total cannot exceed 100%.");
        }
    }

    /** Validates the form values and returns the cleaned component (not yet saved). */
    static EvaluationComponent validated(Course course, int id, String name, String category,
                                         double maxMarks, double weightage) throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Component name is required.");
        }
        String cleanName = name.trim();
        if (cleanName.length() > MAX_NAME_LENGTH) {
            throw new ValidationException("Component name must be at most " + MAX_NAME_LENGTH + " characters.");
        }
        String cleanCategory = category == null ? "" : category.trim().toUpperCase();
        if (!EvaluationComponent.CATEGORIES.contains(cleanCategory)) {
            throw new ValidationException("Choose a category: CA, ASSIGNMENT, PRACTICAL or FINAL_EXAM.");
        }
        if (EvaluationComponent.PRACTICAL.equals(cleanCategory) && !course.getCourseType().hasPractical()) {
            throw new ValidationException(course.getCourseCode() + " is a theory-only course, "
                    + "so it cannot have a practical component.");
        }
        if (Double.isNaN(maxMarks) || Double.isInfinite(maxMarks) || maxMarks <= 0 || maxMarks > MAX_MARKS_LIMIT) {
            throw new ValidationException("Maximum marks must be greater than 0 and at most " + MAX_MARKS_LIMIT + ".");
        }
        if (Double.isNaN(weightage) || Double.isInfinite(weightage) || weightage <= 0 || weightage > 100) {
            throw new ValidationException("Weightage must be greater than 0 and at most 100.");
        }
        return new EvaluationComponent(id, 0, cleanName, cleanCategory,
                round2(maxMarks), round2(weightage));
    }

    private static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
