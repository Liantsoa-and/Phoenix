package listeners;

import annotation.Controller;
import annotation.UrlMapping;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import utils.ClassScanner;
import utils.UrlEntry;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppInitializer implements ServletContextListener {

    // Sprint 5-bis : clé d'attribut sous laquelle Spring stocke son WebApplicationContext racine
    private static final String SPRING_ROOT = "org.springframework.web.context.WebApplicationContext.ROOT";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();

        Map<String, UrlEntry> routes = new HashMap<>();
        List<Class<?>> controllers = ClassScanner.getClasses("controllers", Controller.class);
        for (Class<?> controllerClass : controllers) {
            registerController(controllerClass, routes);
        }
        servletContext.setAttribute("routes", routes);

        // Sprint 5-bis : on récupère le contexte Spring déjà chargé par ContextLoaderListener
        // (celui-ci doit être déclaré AVANT AppInitializer dans web.xml)
        // et on le republie sous un attribut propre à notre framework.
        servletContext.setAttribute("springContext", servletContext.getAttribute(SPRING_ROOT));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // nettoyage éventuel
    }

    private void registerController(Class<?> controllerClass, Map<String, UrlEntry> routes) {
        Controller controllerAnnotation = controllerClass.getAnnotation(Controller.class);
        String controllerName = controllerAnnotation.value();

        for (Method method : controllerClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(UrlMapping.class)) {
                UrlMapping mapping = method.getAnnotation(UrlMapping.class);
                String key = mapping.method() + ":" + mapping.value();
                routes.put(key, new UrlEntry(mapping.value(), controllerName, method, mapping.method()));
            }
        }
    }
}
