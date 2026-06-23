package annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)  // pour class et interface 
public @interface Controller {
    String value() default ""; 
}
