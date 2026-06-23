package controllers;

import annotation.Controller;
import annotation.GetMapping;
import annotation.PostMapping;
import annotation.UrlMapping;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;

public class FrontControllerServlet extends HttpServlet {

    private final Map<String, Method>  routes      = new HashMap<>();
    private final Map<String, Object>  instances   = new HashMap<>();
    private final List<String>         controllerNames = new ArrayList<>();

    // URLs déclarées avec @UrlMapping  →  "NomController#nomMethode"
    private final Map<String, String>  urlMappings = new LinkedHashMap<>();

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
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        URL resource = cl.getResource(path);
        if (resource == null)
            throw new ServletException("Package introuvable : " + packageName);
        if (!"file".equals(resource.getProtocol()))
            throw new ServletException("Scan supporté uniquement sur classes exploded, URL=" + resource);

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
            method.setAccessible(true);

            // --- @GetMapping / @PostMapping (comportement existant) ---
            String url = null;
            if (method.isAnnotationPresent(GetMapping.class))
                url = method.getAnnotation(GetMapping.class).value();
            else if (method.isAnnotationPresent(PostMapping.class))
                url = method.getAnnotation(PostMapping.class).value();

            if (url != null && !url.isBlank()) {
                if (routes.containsKey(url))
                    throw new ServletException("URL dupliquée : " + url
                            + " (déjà mappée sur " + routes.get(url) + ")");
                routes.put(url, method);
                instances.put(url, instance);
            }

            // --- @UrlMapping (nouveau) ---
            if (method.isAnnotationPresent(UrlMapping.class)) {
                String mappedUrl = method.getAnnotation(UrlMapping.class).value();
                if (mappedUrl != null && !mappedUrl.isBlank()) {
                    if (urlMappings.containsKey(mappedUrl))
                        throw new ServletException("@UrlMapping dupliqué : " + mappedUrl);
                    // stocke  "NomSimpleController#nomMethode"
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
            res.setContentType("text/html;charset=UTF-8");
            PrintWriter pw = res.getWriter();
            pw.println("<h2>Contrôleurs détectés</h2><ul>");
            for (String name : controllerNames)
                pw.println("<li>" + name + "</li>");
            pw.println("</ul>");
            return;
        }

        // --- @UrlMapping : retourne controller + méthode associée ---
        if (urlMappings.containsKey(path)) {
            String[] parts = urlMappings.get(path).split("#");
            res.setContentType("text/html;charset=UTF-8");
            PrintWriter pw = res.getWriter();
            pw.println("<h2>@UrlMapping — " + path + "</h2>");
            pw.println("<p><strong>Controller :</strong> " + parts[0] + "</p>");
            pw.println("<p><strong>Méthode&nbsp;&nbsp;&nbsp;:</strong> " + parts[1] + "</p>");
            return;
        }

        // --- @GetMapping / @PostMapping : routage classique ---
        Method method = routes.get(path);
        if (method == null) {
            // URL inconnue → liste toutes les URLs accessibles
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.setContentType("text/html;charset=UTF-8");
            PrintWriter pw = res.getWriter();
            pw.println("<h2>URL introuvable : " + path + "</h2>");
            pw.println("<h3>URLs accessibles</h3><ul>");
            for (String u : urlMappings.keySet())
                pw.println("<li><a href=\"" + u + "\">" + u + "</a> (@UrlMapping)</li>");
            for (String u : routes.keySet())
                pw.println("<li><a href=\"" + u + "\">" + u + "</a></li>");
            pw.println("</ul>");
            return;
        }

        try {
            Object result = method.invoke(instances.get(path), req, res);
            if (!(result instanceof String))
                throw new ServletException("La méthode " + method.getName()
                        + " doit retourner un String (nom de vue), reçu : "
                        + (result == null ? "null" : result.getClass().getName()));
            req.getRequestDispatcher("/WEB-INF/views/" + result + ".jsp").forward(req, res);
        } catch (InvocationTargetException e) {
            throw new ServletException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException { processRequest(req, res); }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException { processRequest(req, res); }
}