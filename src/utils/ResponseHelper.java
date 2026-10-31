package utils;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Writes consistent HTML or JSON responses for authentication failures. */
public final class ResponseHelper {

    private ResponseHelper() {
    }

    public static void handleUnauthorized(HttpServletResponse response,
                                          boolean apiRequest) throws IOException {
        writeAccessError(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Authentification requise.", apiRequest);
    }

    public static void handleForbidden(HttpServletResponse response,
                                       boolean apiRequest) throws IOException {
        writeAccessError(response, HttpServletResponse.SC_FORBIDDEN,
                "Rôle insuffisant pour accéder à cette ressource.", apiRequest);
    }

    private static void writeAccessError(HttpServletResponse response,
                                         int status, String message,
                                         boolean apiRequest) throws IOException {
        response.resetBuffer();
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        if (apiRequest) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":" + status
                    + ",\"error\":\"" + escapeJson(message) + "\"}");
        } else {
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("<!doctype html><html><head><meta charset=\"UTF-8\">");
            response.getWriter().write("<title>Erreur " + status + "</title></head><body>");
            response.getWriter().write("<h1>Erreur " + status + "</h1><p>"
                    + escapeHtml(message) + "</p></body></html>");
        }
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
