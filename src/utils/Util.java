package utils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Part;
import jakarta.servlet.http.HttpServletRequest;
import models.Fichier;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Util {

    // Garde-fou contre les références circulaires (A -> B -> A ...)
    private static final int MAX_DEPTH = 5;

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

    /** Ancienne signature conservée pour ne rien casser. */
    public static Object[] getParameter(Method method, HttpServletRequest req) {
        return getParameter(method, req, null);
    }

    /**
     * Construit les arguments de la méthode du controller.
     * - WebApplicationContext  -> injecte le springContext
     * - type simple            -> req.getParameter(nomDuParamètre)
     * - tableau / List         -> req.getParameterValues(nomDuParamètre)
     * - objet                  -> binding via "nomDuParamètre.propriété"
     *                             (avec repli sur "propriété" seule, comme avant)
     */
    public static Object[] getParameter(Method method, HttpServletRequest req,
                                        WebApplicationContext springContext) {

        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {

            Class<?> type = parameters[i].getType();
            String name = parameters[i].getName();

            Object value;

            if (type == WebApplicationContext.class) {
                value = springContext;
            } else if (type == Fichier.class) {
                value = resolveFile(name, req);
            } else if (isSimpleType(type) || isMultiValued(type)) {
                value = resolveValue(type, parameters[i].getParameterizedType(), name, null, req);
            } else {
                value = bind(type, name, true, req, 0);
            }

            if (value == null && type.isPrimitive()) {
                value = defaultPrimitive(type);
            }
            args[i] = value;
        }

        return args;
    }

    /** Récupère une partie fichier d'une requête multipart, si elle existe. */
    private static Fichier resolveFile(String parameterName, HttpServletRequest req) {
        String contentType = req.getContentType();
        if (contentType == null
                || !contentType.toLowerCase().startsWith("multipart/form-data")) {
            return null;
        }

        try {
            Part part = req.getPart(parameterName);
            if (part == null) {
                return null;
            }

            String submittedFileName = part.getSubmittedFileName();
            if (submittedFileName == null || submittedFileName.trim().isEmpty() || part.getSize() == 0) {
                return null;
            }

            Fichier fichier = new Fichier();
            fichier.setNom(submittedFileName);
            fichier.setTypeContenu(part.getContentType());
            fichier.setTaille(part.getSize());
            try (InputStream input = part.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                fichier.setContenu(output.toByteArray());
            }
            return fichier;
        } catch (IOException | ServletException e) {
            throw new IllegalArgumentException(
                    "Impossible de lire le fichier du paramètre '" + parameterName + "'", e);
        }
    }

    // ------------------------------------------------------------------
    // Binding d'un objet
    // ------------------------------------------------------------------

    /**
     * @param prefix     préfixe des champs ("employe" -> "employe.nom")
     * @param legacyFlat true pour le premier niveau : si "employe.nom" n'existe pas,
     *                   on essaie "nom" (comportement du Sprint-7 bis)
     */
    private static Object bind(Class<?> type, String prefix, boolean legacyFlat,
                               HttpServletRequest req, int depth) {

        if (depth > MAX_DEPTH) {
            throw new RuntimeException("Profondeur de binding dépassée pour " + type.getName());
        }

        try {
            Object object = type.getDeclaredConstructor().newInstance();

            for (Method setter : type.getMethods()) {

                String methodName = setter.getName();
                if (!methodName.startsWith("set") || methodName.length() <= 3
                        || setter.getParameterCount() != 1) {
                    continue;
                }

                String property = Character.toLowerCase(methodName.charAt(3))
                        + methodName.substring(4);
                String key = prefix + "." + property;
                String fallback = legacyFlat ? property : null;

                Class<?> propType = setter.getParameterTypes()[0];
                Type genericType = setter.getGenericParameterTypes()[0];

                Object value;

                if (isSimpleType(propType) || isMultiValued(propType)) {
                    value = resolveValue(propType, genericType, key, fallback, req);
                } else if (isBindableObject(propType)) {
                    // Objet imbriqué : on ne le crée que si au moins un champ "key.xxx" existe
                    if (!hasPrefix(req, key + ".")) {
                        continue;
                    }
                    value = bind(propType, key, false, req, depth + 1);
                } else {
                    continue;
                }

                if (value != null) {
                    setter.invoke(object, value);
                }
            }

            return object;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Impossible de binder l'objet " + type.getName(), e);
        }
    }

    // ------------------------------------------------------------------
    // Lecture d'une valeur (simple, tableau, List)
    // ------------------------------------------------------------------

    private static Object resolveValue(Class<?> type, Type genericType,
                                       String key, String fallbackKey,
                                       HttpServletRequest req) {

        String[] values = values(req, key, fallbackKey);
        if (values == null) {
            return null;
        }

        if (isSimpleType(type)) {
            return values.length == 0 ? null : convert(values[0], type);
        }

        if (type.isArray()) {
            Class<?> component = type.getComponentType();
            Object array = Array.newInstance(component, values.length);
            for (int i = 0; i < values.length; i++) {
                Object item = convert(values[i], component);
                if (item == null && component.isPrimitive()) {
                    item = defaultPrimitive(component);
                }
                if (item != null) {
                    Array.set(array, i, item);
                }
            }
            return array;
        }

        // List / Collection : on lit le type des éléments dans List<X>
        Class<?> elementType = String.class;
        if (genericType instanceof ParameterizedType) {
            Type element = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            if (element instanceof Class<?>) {
                elementType = (Class<?>) element;
            }
        }

        List<Object> list = new ArrayList<>();
        for (String v : values) {
            list.add(convert(v, elementType));
        }
        return list;
    }

    private static String[] values(HttpServletRequest req, String key, String fallbackKey) {
        String[] values = req.getParameterValues(key);
        if (values == null && fallbackKey != null) {
            values = req.getParameterValues(fallbackKey);
        }
        return values;
    }

    private static boolean hasPrefix(HttpServletRequest req, String prefix) {
        for (Map.Entry<String, String[]> e : req.getParameterMap().entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Classification des types
    // ------------------------------------------------------------------

    private static boolean isSimpleType(Class<?> type) {
        return type == String.class
                || type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == boolean.class || type == Boolean.class
                || type == double.class || type == Double.class
                || type == float.class || type == Float.class
                || type == BigDecimal.class || type == BigInteger.class
                || type == LocalDate.class || type == LocalDateTime.class
                || type == LocalTime.class
                || type.isEnum();
    }

    private static boolean isMultiValued(Class<?> type) {
        return type.isArray() || type == List.class || type == Collection.class;
    }

    private static boolean isBindableObject(Class<?> type) {
        return !type.isPrimitive()
                && !type.isInterface()
                && !Modifier.isAbstract(type.getModifiers())
                && !Collection.class.isAssignableFrom(type)
                && !Map.class.isAssignableFrom(type);
    }

    // ------------------------------------------------------------------
    // Conversion String -> type
    // ------------------------------------------------------------------

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convert(String value, Class<?> type) {

        if (value == null) {
            return null;
        }
        if (type == String.class) {
            return value;
        }

        // Un champ de formulaire vide pour un nombre / une date = "pas de valeur"
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }

        try {
            if (type == int.class || type == Integer.class) return Integer.parseInt(value);
            if (type == long.class || type == Long.class) return Long.parseLong(value);
            if (type == double.class || type == Double.class) return Double.parseDouble(value);
            if (type == float.class || type == Float.class) return Float.parseFloat(value);
            if (type == BigDecimal.class) return new BigDecimal(value);
            if (type == BigInteger.class) return new BigInteger(value);
            if (type == LocalDate.class) return LocalDate.parse(value);
            if (type == LocalDateTime.class) return LocalDateTime.parse(value);
            if (type == LocalTime.class) return LocalTime.parse(value);
            if (type.isEnum()) return Enum.valueOf((Class<Enum>) type, value);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "Valeur invalide '" + value + "' pour le type " + type.getSimpleName(), e);
        }

        if (type == boolean.class || type == Boolean.class) {
            // Une checkbox HTML cochée envoie "on" par défaut
            return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("on")
                    || value.equals("1");
        }

        return value;
    }

    private static Object defaultPrimitive(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        return null;
    }
}
