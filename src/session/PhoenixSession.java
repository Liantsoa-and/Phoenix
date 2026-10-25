package session;

import jakarta.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A small wrapper around the servlet container's HTTP session. */
public class PhoenixSession {

    private final HttpSession session;

    public PhoenixSession(HttpSession session) {
        this.session = Objects.requireNonNull(session, "session ne doit pas être null");
    }

    public void setAttribute(String key, Object value) {
        session.setAttribute(Objects.requireNonNull(key, "key ne doit pas être null"), value);
    }

    public Object getAttribute(String key) {
        return session.getAttribute(key);
    }

    public <T> T getAttribute(String key, Class<T> type) {
        Objects.requireNonNull(type, "type ne doit pas être null");
        Object value = session.getAttribute(key);
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new IllegalStateException("L'attribut '" + key + "' est de type "
                    + value.getClass().getName() + ", type attendu : " + type.getName());
        }
        return type.cast(value);
    }

    public void removeAttribute(String key) {
        session.removeAttribute(key);
    }

    /** Returns a snapshot; changing it does not change the HTTP session. */
    public Map<String, Object> getAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            attributes.put(name, session.getAttribute(name));
        }
        return attributes;
    }

    public void clear() {
        List<String> namesToRemove = new ArrayList<>();
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            namesToRemove.add(names.nextElement());
        }
        for (String name : namesToRemove) {
            session.removeAttribute(name);
        }
    }

    public String getId() {
        return session.getId();
    }
}
