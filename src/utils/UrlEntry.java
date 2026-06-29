package utils;

import java.lang.reflect.Method;
import java.util.Objects;

public class UrlEntry {
    private final String url;
    private final String controllerName;
    private final Method method;

    public UrlEntry(String url, String controllerName, Method method) {
        this.url = url;
        this.controllerName = controllerName;
        this.method = method;
    }

    // Getters
    public String getUrl() {
        return url;
    }

    public String getControllerName() {
        return controllerName;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public String toString() {
        return url + " → " + controllerName + "#" + method.getName();
    }

    // equals et hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlEntry urlEntry = (UrlEntry) o;
        return Objects.equals(url, urlEntry.url) && Objects.equals(controllerName, urlEntry.controllerName) && Objects.equals(method, urlEntry.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, controllerName, method);
    }
}   