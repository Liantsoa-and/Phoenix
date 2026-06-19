Bon, j'ai bien cerné ton projet : c'est un framework Java web (style Spring MVC minimaliste) avec un `FrontControllerServlet`. Tu veux créer tes propres annotations comme `@Controller`, `@GetMapping`, etc.

Voici comment faire en Java, de A à Z :

---

## 1. Créer une annotation en Java

En Java, une annotation est une **interface spéciale** déclarée avec `@interface` :

```java
// src/annotation/Controller.java
package annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)  // disponible au runtime (réflexion)
@Target(ElementType.TYPE)            // applicable sur une classe
public @interface Controller {
    String value() default "";       // optionnel : nom du contrôleur
}
```

```java
// src/annotation/GetMapping.java
package annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)          // applicable sur une méthode
public @interface GetMapping {
    String value();                  // ex: "/home"
}
```

---

## 2. Les méta-annotations clés

| Méta-annotation | Rôle |
|---|---|
| `@Retention(RUNTIME)` | L'annotation est lisible via réflexion au runtime |
| `@Retention(SOURCE)` | Disparaît après compilation (ex: `@Override`) |
| `@Target(TYPE)` | S'applique aux classes/interfaces |
| `@Target(METHOD)` | S'applique aux méthodes |
| `@Target(FIELD)` | S'applique aux attributs |

---

## 3. Scanner et utiliser les annotations dans ton FrontController

C'est là que la **réflexion** (`java.lang.reflect`) entre en jeu. Ton `FrontControllerServlet` doit :
1. Scanner les classes du classpath
2. Détecter celles annotées `@Controller`
3. Mapper leurs méthodes `@GetMapping` aux URLs

```java
// Dans FrontControllerServlet.java
import annotation.Controller;
import annotation.GetMapping;
import java.lang.reflect.*;
import java.util.*;

public class FrontControllerServlet extends HttpServlet {

    // Map URL → méthode à appeler
    private Map<String, Method> routes = new HashMap<>();
    private Map<String, Object> controllers = new HashMap<>();

    @Override
    public void init() throws ServletException {
        // Enregistrer manuellement les contrôleurs (ou scanner le classpath)
        registerController(HomeController.class);
    }

    private void registerController(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Controller.class)) return;

        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(GetMapping.class)) {
                    String url = method.getAnnotation(GetMapping.class).value();
                    routes.put(url, method);
                    controllers.put(url, instance);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur init contrôleur", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String path = req.getPathInfo(); // ex: "/home"
        Method method = routes.get(path);

        if (method == null) {
            res.sendError(404);
            return;
        }

        try {
            String view = (String) method.invoke(controllers.get(path), req, res);
            req.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(req, res);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
```

---

## 4. Un contrôleur utilisateur (ce que tu écriras au quotidien)

```java
// src/controller/HomeController.java
package controller;

import annotation.Controller;
import annotation.GetMapping;
import jakarta.servlet.http.*;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String index(HttpServletRequest req, HttpServletResponse res) {
        req.setAttribute("message", "Bienvenue sur Phoenix !");
        return "home"; // → /WEB-INF/views/home.jsp
    }
}
```

---

## 5. Structure de fichiers conseillée

```
src/
├── annotation/
│   ├── Controller.java
│   ├── GetMapping.java
│   └── PostMapping.java
├── controller/
│   ├── FrontControllerServlet.java
│   └── HomeController.java
```

---

**Résumé du flux** : `@interface` définit l'annotation → `@Retention(RUNTIME)` la rend lisible → `Class.isAnnotationPresent()` / `Method.getAnnotation()` permettent de l'exploiter par réflexion dans ton FrontController.

