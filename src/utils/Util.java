package utils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import jakarta.servlet.http.HttpServletRequest;

public class Util {

    private Util() {
    }

    public static boolean haveParameter(Method method, Class<?> param) {
        if (method == null || param == null) {
            return false;
        }

        for (Class<?> type : method.getParameterTypes()) {
            if (type.isAssignableFrom(param)) {
                return true;
            }
        }

        return false;
    }

    public static Object[] getParameter(Method method, HttpServletRequest req) {

        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {

            Class<?> type = parameters[i].getType();
            String name = parameters[i].getName();

            if (isSimpleType(type)) {
                args[i] = convert(req.getParameter(name), type);
            } else {
                args[i] = createObject(type, req);
            }
        }

        return args;
    }

    private static Object createObject(Class<?> type, HttpServletRequest req) {

        try {
            Object object = type.getDeclaredConstructor().newInstance();

            for (Method method : type.getMethods()) {

                if (!method.getName().startsWith("set")
                        || method.getParameterCount() != 1) {
                    continue;
                }

                String property = method.getName().substring(3);
                property = Character.toLowerCase(property.charAt(0))
                        + property.substring(1);

                String value = req.getParameter(property);

                if (value != null) {
                    Class<?> propertyType = method.getParameterTypes()[0];
                    method.invoke(object, convert(value, propertyType));
                }
            }

            return object;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Impossible de binder l'objet " + type.getName(), e);
        }
    }

    private static boolean isSimpleType(Class<?> type) {
        return type == String.class
                || type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == boolean.class || type == Boolean.class
                || type == double.class || type == Double.class
                || type == float.class || type == Float.class;
    }

    private static Object convert(String value, Class<?> type) {

        if (value == null) {
            return null;
        }

        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        if (type == float.class || type == Float.class) return Float.parseFloat(value);

        return value;
    }
}