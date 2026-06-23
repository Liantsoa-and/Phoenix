package annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)  // pour class et interface 
public @interface UrlMapping {
    String value() default "";     
}
