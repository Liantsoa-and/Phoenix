package models;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {

    private String viewName;
    private Map<String, Object> attributes;

    public ModelAndView() {
        this.attributes = new HashMap<>();
    }

    public ModelAndView(String viewName) {
        this.viewName = viewName;
        this.attributes = new HashMap<>();
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public void addAttribute(String name, Object value) {
        this.attributes.put(name, value);
    }

    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
