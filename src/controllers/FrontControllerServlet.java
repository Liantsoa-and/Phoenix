package controllers;

import annotation.Controller;
import annotation.GetMapping;
import annotation.PostMapping;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;

public class FrontControllerServlet extends HttpServlet {

    private final Map<String, Method> routes = new HashMap<>();
    private final Map<String, Object> instances = new HashMap<>();
    private final List<String> controllerNames = new ArrayList<>();

    @Override
    public void init() throws ServletException {
        String packageName = getServletConfig().getInitParameter("controllerPackage");
        if (packageName == null || packageName.isBlank()) {
            log("Phoenix: aucun controllerPackage configuré.");
            return;
        }
        try {
            scanPackage(packageName);
            log("Phoenix: " + controllerNames.size() + " contrôleur(s) enregistré(s).");
        } catch (Exception e) {
            throw new ServletException("Phoenix: échec du scan du package " + packageName, e);
        }
    }

    private void scanPackage(String packageName) throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        URL resource = cl.getResource(path);
        if (resource == null) {
            throw new ServletException("Package introuvable : " + packageName);
        }
        if (!"file".equals(resource.getProtocol())) {
            throw new ServletException("Scan supporté uniquement sur classes exploded (WEB-INF/classes), URL=" + resource);
        }

        File dir = new File(resource.toURI());
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (!file.getName().endsWith(".class")) continue;
            String className = packageName + "." + file.getName().replace(".class", "");
            Class<?> clazz = cl.loadClass(className);
            if (clazz.isAnnotationPresent(Controller.class)) {
                controllerNames.add(className);
                registerController(clazz);
            }
        }
    }

    private void registerController(Class<?> clazz) throws Exception {
        Constructor<?> ctor = clazz.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object instance = ctor.newInstance();

        for (Method method : clazz.getDeclaredMethods()) {
            String url = null;
            if (method.isAnnotationPresent(GetMapping.class)) {
                url = method.getAnnotation(GetMapping.class).value();
            } else if (method.isAnnotationPresent(PostMapping.class)) {
                url = method.getAnnotation(PostMapping.class).value();
            }
            if (url == null || url.isBlank()) continue;

            method.setAccessible(true);
            if (routes.containsKey(url)) {
                throw new ServletException("URL dupliquée : " + url
                        + " (déjà mappée sur " + routes.get(url) + ")");
            }
            routes.put(url, method);
            instances.put(url, instance);
        }
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        // Avec url-pattern "/", servletPath contient l'URL réelle
        String path = req.getServletPath();

        // Sprint-1 : liste des contrôleurs
        if ("/controllers".equals(path)) {
            res.setContentType("text/html;charset=UTF-8");
            PrintWriter pw = res.getWriter();
            pw.println("<h2>Contrôleurs détectés</h2><ul>");
            for (String name : controllerNames) {
                pw.println("<li>" + name + "</li>");
            }
            pw.println("</ul>");
            return;
        }

        Method method = routes.get(path);
        if (method == null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "Aucune route pour " + path);
            return;
        }

        try {
            Object result = method.invoke(instances.get(path), req, res);
            if (!(result instanceof String)) {
                throw new ServletException("La méthode " + method.getName()
                        + " doit retourner un String (nom de vue), reçu : "
                        + (result == null ? "null" : result.getClass().getName()));
            }
            String view = (String) result;
            req.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(req, res);
        } catch (InvocationTargetException e) {
            throw new ServletException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        processRequest(req, res);
    }
}
