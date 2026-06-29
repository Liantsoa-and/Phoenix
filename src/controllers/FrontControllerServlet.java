package controllers;

import annotation.Controller;
import annotation.UrlMapping;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import utils.ClassScanner;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class FrontControllerServlet extends HttpServlet {

    private final Map<String, Method> routes = new HashMap<>();
    private final Map<String, Object> instances = new HashMap<>();
    private final List<String> controllerNames = new ArrayList<>();

    // URLs déclarées avec @UrlMapping → "NomController#nomMethode"
    private final Map<String, String> urlMappings = new LinkedHashMap<>();

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
            log("Phoenix: " + urlMappings.size() + " @UrlMapping(s) enregistré(s).");
        } catch (Exception e) {
            throw new ServletException("Phoenix: échec du scan du package " + packageName, e);
        }
    }

    private void scanPackage(String packageName) throws Exception {
        List<Class<?>> controllerClasses = ClassScanner.getClasses(packageName, Controller.class);
        for (Class<?> clazz : controllerClasses) {
            controllerNames.add(clazz.getName());
            registerController(clazz);
        }
    }

    private void registerController(Class<?> clazz) throws Exception {
        for (Method method : clazz.getDeclaredMethods()) {
            method.setAccessible(true);
            if (method.isAnnotationPresent(UrlMapping.class)) {
                String mappedUrl = method.getAnnotation(UrlMapping.class).value();
                if (mappedUrl != null && !mappedUrl.isBlank()) {
                    if (urlMappings.containsKey(mappedUrl))
                        throw new ServletException("@UrlMapping dupliqué : " + mappedUrl);
                    urlMappings.put(mappedUrl, clazz.getSimpleName() + "#" + method.getName());
                }
            }
        }
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

        // --- @UrlMapping : retourne controller + méthode associée ---
        // if (urlMappings.containsKey(path)) {
        //     String[] parts = urlMappings.get(path).split("#");
        //     res.setContentType("text/html;charset=UTF-8");
        //     PrintWriter pw = res.getWriter();
        //     pw.println("<h2>@UrlMapping — " + path + "</h2>");
        //     pw.println("<p><strong>Controller :</strong> " + parts[0] + "</p>");
        //     pw.println("<p><strong>Méthode&nbsp;&nbsp;&nbsp;:</strong> " + parts[1] + "</p>");
        //     return;
        // }

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