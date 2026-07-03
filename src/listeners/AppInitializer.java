package listeners;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebListener;
import utils.ClassScanner;
import utils.UrlEntry;
import annotation.Controller;
import annotation.UrlMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Application listener that initializes controller mappings at startup.
 */
@WebListener
public class AppInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String packageName = sce.getServletContext().getInitParameter("controllerPackage");
        if (packageName == null || packageName.isBlank()) {
            sce.getServletContext().log("Phoenix: aucun controllerPackage configuré.");
            return;
        }

        try {
            List<Class<?>> controllerClasses = ClassScanner.getClasses(packageName, Controller.class);
            List<String> controllerNames = new ArrayList<>();
            Map<String, UrlEntry> routes = new LinkedHashMap<>();

            for (Class<?> clazz : controllerClasses) {
                controllerNames.add(clazz.getName());
                registerController(clazz, routes);
            }

            sce.getServletContext().setAttribute("controllerNames", controllerNames);
            sce.getServletContext().setAttribute("routes", routes);

            sce.getServletContext().log("Phoenix: " + controllerNames.size() + " contrôleur(s) enregistré(s).");
            sce.getServletContext().log("Phoenix: " + routes.size() + " @UrlMapping(s) enregistré(s).");
        } catch (Exception e) {
            throw new RuntimeException("Phoenix: échec du scan du package " + packageName, e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Cleanup on shutdown if needed
        sce.getServletContext().removeAttribute("controllerNames");
        sce.getServletContext().removeAttribute("routes");
    }

    private void registerController(Class<?> clazz, Map<String, UrlEntry> routes) throws Exception {
        for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
            method.setAccessible(true);
            if (method.isAnnotationPresent(annotation.UrlMapping.class)) {
                annotation.UrlMapping annotation = method.getAnnotation(annotation.UrlMapping.class);
                String mappedUrl = annotation.value();
                String httpMethod = annotation.method();
                if (mappedUrl != null && !mappedUrl.isBlank()) {
                    UrlEntry candidate = new UrlEntry(mappedUrl, clazz.getName(), method, httpMethod);
                    String key = mappedUrl + "_" + httpMethod;
                    // Check for duplicate mapping (same URL and same HTTP method)
                    if (routes.containsKey(key) && routes.get(key).equals(candidate)) {
                        throw new ServletException("@UrlMapping dupliqué: " + httpMethod + " " + mappedUrl);
                    }
                    routes.put(key, candidate);
                }
            }
        }
    }
}