package annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// annotation/Enumerated.java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Enumerated {
    EnumType value() default EnumType.STRING;
    enum EnumType { ORDINAL, STRING }
}