package annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD) // pour les methodes
public @interface PostMapping {
    String value() default "";
}
