package annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OneToMany {
    String mappedBy() default "";     // nom du champ dans l’entité cible
    String joinTable() default "";    // nom de la table de jointure
    String joinColumn() default "";   // colonne FK côté propriétaire
    String inverseJoinColumn() default ""; // colonne FK côté cible
}
