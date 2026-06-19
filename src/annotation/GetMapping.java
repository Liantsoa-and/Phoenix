package annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD) // pour les methodes
public @interface GetMapping {
    String value() default "";
}
