package annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ManyToOne {
    String joinColumn() default "";   // nom de la colonne FK, ex: "adresse_id"
    boolean eager() default false;    // chargement automatique ?
}
