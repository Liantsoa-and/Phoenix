package models;

public class ViewResolver {
    private String prefix = "/views/";
    private String suffix = ".jsp";  // ou .html
    
    public String resolveViewName(String viewName) {
        // Transforme "home" → "/views/home.jsp"
        return prefix + viewName + suffix;
    }
}