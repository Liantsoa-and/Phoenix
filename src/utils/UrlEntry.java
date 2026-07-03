package utils;

import java.lang.reflect.Method;
import java.util.Objects;

public class UrlEntry {
    private final String url;
    private final String controllerName;
    private final Method method;
    private final String httpMethod;

    public UrlEntry(String url, String controllerName, Method method, String httpMethod) {
        this.url = url;
        this.controllerName = controllerName;
        this.method = method;
        this.httpMethod = httpMethod;
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

    public String getHttpMethod() {
        return httpMethod;
    }

    @Override
    public String toString() {
        return httpMethod + " " + url + " -> " + controllerName + "#" + method.getName();
    }

    // equals et hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UrlEntry that = (UrlEntry) o;
        return Objects.equals(url, that.url) && Objects.equals(httpMethod, that.httpMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, httpMethod);
    }
}   