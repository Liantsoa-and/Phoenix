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
    // sprint-3 : nouvelle clé
    // clé = url + "_" + méthode HTTP (ex: /hello_GET, /hello_POST)
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
        UrlEntry match = findMatch(path, req.getMethod());

        if (match != null) {
            PrintWriter pw = res.getWriter();
            pw.println("URL trouvée : " + match.getUrl() + "\n");
            pw.println("Méthode HTTP : " + match.getHttpMethod() + "\n");
            pw.println("Controller   : " + match.getControllerName() + "\n");
            pw.println("Methode      : " + match.getMethod().getName() + "\n");
            return;
        }

        PrintWriter pw = res.getWriter();
        pw.println("URL introuvable (ou méthode http non supportée): " + path + "\n");
        pw.println("URLs valides :\n");
        for (UrlEntry entry : /* urlEntries */ routes.values()) {
            pw.println(entry.getHttpMethod() + " " + entry.getUrl() + " -> " + entry.getControllerName() + "/" + entry.getMethod().getName() + "\n");
        }

    }

    private void scanPackage(String packageName) throws Exception {
        List<Class<?>> controllerClasses = ClassScanner.getClasses(packageName, Controller.class);
        for (Class<?> clazz : controllerClasses) {
            controllerNames.add(clazz.getName());
            registerController(clazz);
        }
    }

    private UrlEntry findMatch(String path, String httpMethod) {
        // for (UrlEntry entry : urlEntries) {
        // if (entry.getUrl().equals(path)) {
        // return entry;
        // }
        // }
        // return null;
        return routes.get(buildKey(path, httpMethod));
    }

    private String buildKey(String url, String httpMethod){
        return url + "_" + httpMethod; 
    }

    private void registerController(Class<?> clazz) throws Exception {
        for (Method method : clazz.getDeclaredMethods()) {
            method.setAccessible(true);
            if (method.isAnnotationPresent(UrlMapping.class)) {
                UrlMapping annotation = method.getAnnotation(UrlMapping.class);
                String mappedUrl = annotation.value();
                String httpMethod = annotation.method(); 
                if (mappedUrl != null && !mappedUrl.isBlank()) {
                    // urlEntries.add(new UrlEntry(mappedUrl, clazz.getSimpleName(), method));
                    UrlEntry candidate = new UrlEntry(mappedUrl, clazz.getSimpleName(), method, httpMethod);
                    String key = buildKey(mappedUrl, httpMethod);
                    // doublon reel : meme url et meme httpMethode
                    if (routes.containsKey(key) && routes.get(key).equals(candidate)) {
                        throw new ServletException("@UrlMapping dupliqué: " + httpMethod + " " + mappedUrl);
                    }
                    routes.put(key, candidate);
                }
            }
        }
    }

}