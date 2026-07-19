package models;

public class ViewResolver {

    private String prefix = "/WEB-INF/views/";
    private String suffix = ".jsp";

    public String resolveViewName(String viewName) {
        return prefix + viewName + suffix;
    }
}
