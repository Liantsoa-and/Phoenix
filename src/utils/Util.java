package utils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import jakarta.servlet.http.HttpServletRequest;

public class Util {

    private Util() {
        // classe utilitaire, non instanciable
    }

    /**
     * Vérifie si la méthode attend, parmi ses paramètres, un paramètre
     * du type (ou sous-type de) la classe fournie.
     *
     * @param methode la méthode du controller à inspecter
     * @param param   la classe recherchée parmi les paramètres (ex: WebApplicationContext.class)
     * @return true si un des paramètres de la méthode est assignable depuis "param"
     */
    public static boolean haveParameter(Method methode, Class<?> param) {
        if (methode == null || param == null) {
            return false;
        }
        for (Class<?> paramType : methode.getParameterTypes()) {
            if (paramType.isAssignableFrom(param)) {
                return true;
            }
        }
        return false;
    }

    public static Object[] getParameter(Method method, HttpServletRequest req) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            String nom = parameters[i].getName();
            args[i] = req.getParameter(nom);
        }
        return args;
    }
}
