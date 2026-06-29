package controllers;

import annotation.Controller;
import annotation.UrlMapping;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import utils.ClassScanner;
import utils.UrlEntry;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class FrontControllerServlet extends HttpServlet {

    private final List<String> controllerNames = new ArrayList<>();

    // private final List<UrlEntry> urlEntries = new ArrayList<>();
    private final Map<String, UrlEntry> routes = new LinkedHashMap<>();

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
            log("Phoenix: " + /* urlEntries. */ routes.size() + " @UrlMapping(s) enregistré(s).");
        } catch (Exception e) {
            throw new ServletException("Phoenix: échec du scan du package " + packageName, e);
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

    private void processRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String path = req.getServletPath();

        // --- /controllers : liste des contrôleurs (sprint-1) ---
        if ("/controllers".equals(path)) {
            PrintWriter pw = res.getWriter();
            pw.println("Les controleurs sont : \n");
            for (String name : controllerNames)
                pw.println(name + "\n");
            return;
        }

        // sprint-2 : mapper l'URL vers le contrôleur / méthode
        UrlEntry match = findMatch(path);

        if (match != null) {
            PrintWriter pw = res.getWriter();
            pw.println("URL trouvée : " + match.getUrl() + "\n");
            pw.println("Controller   : " + match.getControllerName() + "\n");
            pw.println("Methode      : " + match.getMethod().getName() + "\n");
            return;
        }

        PrintWriter pw = res.getWriter();
        pw.println("URL introuvable : " + path + "\n");
        pw.println("URLs valides :\n");
        for (UrlEntry entry : /* urlEntries */ routes.values()) {
            pw.println(entry.getUrl() + " -> " + entry.getControllerName() + "/" + entry.getMethod().getName() + "\n");
        }

    }

    private void scanPackage(String packageName) throws Exception {
        List<Class<?>> controllerClasses = ClassScanner.getClasses(packageName, Controller.class);
        for (Class<?> clazz : controllerClasses) {
            controllerNames.add(clazz.getName());
            registerController(clazz);
        }
    }

    private UrlEntry findMatch(String path) {
        // for (UrlEntry entry : urlEntries) {
        // if (entry.getUrl().equals(path)) {
        // return entry;
        // }
        // }
        // return null;
        return routes.get(path);
    }

    private void registerController(Class<?> clazz) throws Exception {
        for (Method method : clazz.getDeclaredMethods()) {
            method.setAccessible(true);
            if (method.isAnnotationPresent(UrlMapping.class)) {
                String mappedUrl = method.getAnnotation(UrlMapping.class).value();
                if (mappedUrl != null && !mappedUrl.isBlank()) {
                    // urlEntries.add(new UrlEntry(mappedUrl, clazz.getSimpleName(), method));
                    if (routes.containsKey(mappedUrl)) {
                        throw new ServletException("@UrlMapping dupliqué: " + mappedUrl);
                    }
                    routes.put(mappedUrl, new UrlEntry(mappedUrl, clazz.getSimpleName(), method));
                }
            }
        }
    }

}