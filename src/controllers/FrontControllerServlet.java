package controllers;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import models.ModelAndView;
import models.ViewResolver;
import annotation.ApiWeb;
import org.springframework.web.context.WebApplicationContext;
import utils.UrlEntry;
import utils.Util;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class FrontControllerServlet extends HttpServlet {

    private List<String> controllerNames;
    private Map<String, UrlEntry> routes;
    private ViewResolver viewResolver;

    public FrontControllerServlet() {
        this.viewResolver = new ViewResolver();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    @SuppressWarnings("unchecked")
    private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ServletContext servletContext = getServletContext();
        this.routes = (Map<String, UrlEntry>) servletContext.getAttribute("routes");

        // Avec <url-pattern>/</url-pattern>, le servlet est traité comme le
        // servlet par défaut : getPathInfo() renvoie toujours null. C'est
        // getServletPath() qui porte alors le chemin complet demandé.
        String path = req.getPathInfo();
        if (path == null) {
            path = req.getServletPath();
        }
        String httpMethod = req.getMethod();

        UrlEntry entry = findMatch(routes, path, httpMethod);
        if (entry == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.setContentType("text/html;charset=UTF-8");
            resp.getWriter().write(buildRoutesNotFoundPage(httpMethod, path, routes));
            return;
        }

        try {
            // Sprint 5-bis : récupération du contexte Spring publié par AppInitializer
            WebApplicationContext springContext = (WebApplicationContext) servletContext.getAttribute("springContext");

            Object controllerInstance = Class.forName(entry.getControllerName())
                    .getDeclaredConstructor().newInstance();
            Method method = entry.getMethod();

            boolean isApi = method.isAnnotationPresent(ApiWeb.class);

            Object result;
            if (Util.haveParameter(method, WebApplicationContext.class)) {
                if (springContext == null) {
                    throw new ServletException("Le controller attend un WebApplicationContext mais aucun springContext n'est disponible");
                }
                result = method.invoke(controllerInstance, springContext);
            } else {
                result = method.invoke(controllerInstance);
            }

            handleResult(result, req, resp, isApi);

        } catch (ReflectiveOperationException e) {
            throw new ServletException("Erreur lors de l'invocation du controller", e);
        }
    }

    private void handleResult(Object result, HttpServletRequest req, HttpServletResponse resp, boolean isApi) throws ServletException, IOException {
        if (isApi) {
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            // Sprint-6 : si la méthode renvoie déjà une String, on considère
            // qu'elle a déjà produit du JSON elle-même -> pas de ré-encodage.
            // Sinon (objet quelconque), on le convertit en JSON via Jackson.
            String jsonResponse = (result instanceof String s) ? s : ObjectToJson(result);
            resp.getWriter().write(jsonResponse);
            return;
        } 
        if (result instanceof ModelAndView modelAndView) {
            for (Map.Entry<String, Object> attr : modelAndView.getAttributes().entrySet()) {
                req.setAttribute(attr.getKey(), attr.getValue());
            }
            String path = viewResolver.resolveViewName(modelAndView.getViewName());
            RequestDispatcher dispatcher = req.getRequestDispatcher(path);
            dispatcher.forward(req, resp);
        } else if (result instanceof String viewName) {
            String path = viewResolver.resolveViewName(viewName);
            req.getRequestDispatcher(path).forward(req, resp);
        } else {
            resp.getWriter().write(String.valueOf(result));
        }
    }

    private String buildRoutesNotFoundPage(String httpMethod, String path, Map<String, UrlEntry> routes) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>404 - Route inconnue</title></head><body>");
        sb.append("<h1>Aucune route pour ").append(httpMethod).append(" ").append(path).append("</h1>");
        sb.append("<h2>URLs disponibles :</h2><table border=\"1\" cellpadding=\"5\">");
        sb.append("<tr><th>Méthode</th><th>URL</th><th>Contrôleur</th><th>Méthode Java</th></tr>");
        if (routes != null) {
            for (UrlEntry e : routes.values()) {
                sb.append("<tr><td>").append(e.getHttpMethod()).append("</td><td>")
                  .append(e.getUrl()).append("</td><td>")
                  .append(e.getControllerName()).append("</td><td>")
                  .append(e.getMethod().getName()).append("</td></tr>");
            }
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }

    private UrlEntry findMatch(Map<String, UrlEntry> routes, String path, String httpMethod) {
        String key = buildKey(httpMethod, path);
        return routes.get(key);
    }

    private String buildKey(String httpMethod, String path) {
        return httpMethod + ":" + path;
    }

    private String ObjectToJson(Object o) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(o);
    }
}