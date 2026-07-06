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

    private List<String> controllerNames;
    private Map<String, UrlEntry> routes;

    @Override
    public void init() throws ServletException {
        String packageName = getServletConfig().getInitParameter("controllerPackage");
        if (packageName == null || packageName.isBlank()) {
            log("Phoenix: aucun controllerPackage configuré.");
            return;
        }

        ServletContext sc = getServletContext();
        this.controllerNames = (List<String>) sc.getAttribute("controllerNames");
        this.routes = (Map<String, UrlEntry>) sc.getAttribute("routes");

        if (this.controllerNames == null || this.routes == null) {
            throw new ServletException("Phoenix: les attributs controllerNames ou routes ne sont pas définis dans le contexte. Vérifiez que le listener AppInitializer est bien configuré.");
        }

        log("Phoenix: " + this.controllerNames.size() + " contrôleur(s) récupéré(s) depuis le contexte.");
        log("Phoenix: " + this.routes.size() + " @UrlMapping(s) récupéré(s) depuis le contexte.");
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
            try {
                // Load the controller class
                Class<?> controllerClass = Class.forName(match.getControllerName());

                // Create instance (assuming no-arg constructor)
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();

                // Get the method to invoke
                Method methodToInvoke = match.getMethod();

                // Invoke the method with request and response parameters
                methodToInvoke.invoke(controllerInstance, req, res);

                // Method execution completed - the method should have handled the response
                return;
            } catch (ClassNotFoundException e) {
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Controller class not found: " + match.getControllerName());
                return;
            } catch (NoSuchMethodException e) {
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Controller class must have a public no-arg constructor: " + match.getControllerName());
                return;
            } catch (InstantiationException | IllegalAccessException e) {
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Could not instantiate controller: " + match.getControllerName());
                return;
            } catch (InvocationTargetException e) {
                // The method threw an exception
                Throwable cause = e.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else if (cause instanceof ServletException) {
                    throw (ServletException) cause;
                } else {
                    res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Error invoking controller method: " + cause.getMessage());
                    return;
                }
            }
        }

        PrintWriter pw = res.getWriter();
        pw.println("URL introuvable (ou méthode http non supportée): " + path + "\n");
        pw.println("URLs valides :\n");
        for (UrlEntry entry : routes.values()) {
            pw.println(entry.getHttpMethod() + " " + entry.getUrl() + " -> " + entry.getControllerName() + "/"
                    + entry.getMethod().getName() + "\n");
        }
    }

    private UrlEntry findMatch(String path, String httpMethod) {
        return routes.get(buildKey(path, httpMethod));
    }

    private String buildKey(String url, String httpMethod) {
        return url + "_" + httpMethod;
    }
}