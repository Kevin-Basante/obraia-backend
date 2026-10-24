package com.obraia.backend.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Password rule used in registration and password reset: 8 to 72
 * characters (BCrypt ignores anything after 72) with at least one letter
 * and one number.
 */
@NotBlank
@Size(min = 8, max = 72)
@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$")
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "must have 8 to 72 characters with at least one letter and one number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
