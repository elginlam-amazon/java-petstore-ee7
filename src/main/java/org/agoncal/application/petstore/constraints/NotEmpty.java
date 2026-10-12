package org.agoncal.application.petstore.constraints;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Antonio Goncalves
 *         http://www.antoniogoncalves.org
 *         --
 */

@Constraint(validatedBy = {})
@NotNull
@Size(min = 1)
@ReportAsSingleViolation
@Retention(RetentionPolicy.RUNTIME)
@Target( {ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER,
      ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR})
@Documented
public @interface NotEmpty
{

   // ======================================
   // =             Attributes             =
   // ======================================

   String message() default "{org.agoncal.application.petstore.constraints.NotEmpty.message}";

   Class<?>[] groups() default {};

   Class<? extends Payload>[] payload() default {};

   // ======================================
   // =          Inner Annotation          =
   // ======================================

   @Retention(RetentionPolicy.RUNTIME)
   @Target( {ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER,
         ElementType.TYPE, ElementType.ANNOTATION_TYPE,
         ElementType.CONSTRUCTOR})
   public @interface List
   {
      NotEmpty[] value();
   }
}
