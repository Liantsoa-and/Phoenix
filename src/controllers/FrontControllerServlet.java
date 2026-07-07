package controllers;

import annotation.Controller;
import annotation.UrlMapping;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import models.ModelAndView;
import models.ViewResolver;
import utils.ClassScanner;
import utils.UrlEntry;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class FrontControllerServlet extends HttpServlet {

    private List<String> controllerNames;
    private Map<String, UrlEntry> routes;
    private ViewResolver viewResolver = new ViewResolver();

    /*
     * @Override
     * public void init() throws ServletException {
     * String packageName =
     * getServletConfig().getInitParameter("controllerPackage");
     * if (packageName == null || packageName.isBlank()) {
     * log("Phoenix: aucun controllerPackage configuré.");
     * return;
     * }
     * 
     * if (this.controllerNames == null || this.routes == null) {
     * throw new
     * ServletException("Phoenix: les attributs controllerNames ou routes ne sont pas définis dans le contexte. Vérifiez que le listener AppInitializer est bien configuré."
     * );
     * }
     * 
     * log("Phoenix: " + this.controllerNames.size() +
     * " contrôleur(s) récupéré(s) depuis le contexte.");
     * log("Phoenix: " + this.routes.size() +
     * " @UrlMapping(s) récupéré(s) depuis le contexte.");
     * }
     */

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

    @SuppressWarnings("unchecked")
    private void processRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        ServletContext sc = getServletContext();
        List<String> controllerNames = (List<String>) sc.getAttribute("controllerNames");
        Map<String, UrlEntry> routes = (Map<String, UrlEntry>) sc.getAttribute("routes");

        if (controllerNames == null || routes == null) {
            throw new ServletException(
                    "Phoenix: les attributs controllerNames ou routes ne sont pas définis dans le contexte. Vérifiez que le listener AppInitializer est bien configuré.");
        }

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
        UrlEntry match = findMatch(routes, path, req.getMethod());

        if (match != null) {
            try {
                // Load the controller class
                Class<?> controllerClass = Class.forName(match.getControllerName());

                // Create instance (assuming no-arg constructor)
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();

                // Get the method to invoke
                Method methodToInvoke = match.getMethod();

                // Invoke the method with request and response parameters
                Object result = methodToInvoke.invoke(controllerInstance, req, res);

                // sprint-3 : traiter le résultat selon son type (compatibilité conservée)
                handleResult(result, req, res);
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
    
    private void handleResult(Object result, HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        if (result == null) {
            // void, ou contrôleur qui a déjà écrit la réponse lui-même (cf. TestController)
            return;
        }

        String viewName;

        if (result instanceof ModelAndView) {
            ModelAndView mv = (ModelAndView) result;
            for (Map.Entry<String, Object> entry : mv.getAttributes().entrySet()) {
                req.setAttribute(entry.getKey(), entry.getValue());
            }
            viewName = mv.getViewName();
        } else if (result instanceof String) {
            // ancien comportement : juste un nom de vue, sans attributs
            viewName = (String) result;
        } else {
            // type de retour non géré : on ne fait rien de plus
            return;
        }

        if (viewName == null || viewName.isBlank()) {
            return;
        }

        String viewPath = viewResolver.resolveViewName(viewName);
        req.getRequestDispatcher(viewPath).forward(req, res);
    }

    private UrlEntry findMatch(Map<String, UrlEntry> routes, String path, String httpMethod) {
        return routes.get(buildKey(path, httpMethod));
    }

    private String buildKey(String url, String httpMethod) {
        return url + "_" + httpMethod;
    }
}