# PROJET PHOENIX

Framework Java web minimaliste inspiré de Spring MVC, construit autour d'un FrontControllerServlet et d'un système d'annotations personnalisées.

---

## Sprint-0 : Mise en place du FrontController

L'objectif de ce sprint était de garantir qu'aucune requête HTTP ne puisse atteindre l'application sans passer par un point d'entrée unique contrôlé par le framework. On a donc déclaré le FrontControllerServlet pour intercepter toutes les requêtes entrant dans l'application, qu'elles soient en GET ou en POST, et on les a fait converger vers un même traitement central. C'est cette centralisation qui permet ensuite à toute la logique de routage et de scan d'avoir un seul point d'accroche dans le cycle de vie du servlet.

## Sprint-1 : Découverte automatique des contrôleurs

L'objectif était de ne plus avoir à déclarer manuellement chaque contrôleur de l'application, mais de les détecter automatiquement au démarrage. Le framework explore donc le package applicatif indiqué en configuration et repère les classes marquées par l'annotation prévue à cet effet pour les contrôleurs.

Au cours de ce sprint, on a identifié que la logique de scan ne devait pas rester mélangée avec la logique propre au FrontController : on a donc extrait toute la partie « parcours du classpath » dans un utilitaire séparé, pensé pour être générique et réutilisable au-delà du seul cas des contrôleurs. Le FrontController, lui, ne fait plus que demander la liste des classes correspondant à un package et une annotation donnés, puis se charge de les enregistrer.

## Sprint-2 : Association d'une URL à un contrôleur et une méthode

L'objectif était de pouvoir déclarer, au niveau d'une méthode de contrôleur, l'URL qu'elle doit gérer, puis de savoir répondre correctement que l'URL tapée par l'utilisateur soit connue ou non.

On a d'abord mis en place une version permettant à plusieurs méthodes de partager la même URL sans aucune restriction, le temps de valider que la détection et l'affichage fonctionnaient correctement. Une fois cette base validée, on est passé à une version où chaque URL ne peut plus être associée qu'à un seul couple contrôleur/méthode : toute tentative d'enregistrer une URL déjà existante est désormais rejetée dès le démarrage de l'application, ce qui évite de découvrir un conflit de routage seulement au moment où un utilisateur tape l'URL en question. Quand l'URL demandée ne correspond à rien d'enregistré, le framework affiche la liste de toutes les URLs valides accompagnées de leur contrôleur et de leur méthode, pour faciliter le débogage côté développeur.

## Sprint-3 : Distinction des requêtes GET et POST

L'objectif de ce sprint est de permettre à une même URL d'être déclarée plusieurs fois dans l'application, à condition que chaque déclaration corresponde à une méthode HTTP différente (par exemple une version GET et une version POST pour une même URL). Le framework doit donc être capable de différencier deux déclarations qui portent le même nom d'URL mais qui ne désignent pas la même requête, et de lever une erreur uniquement quand deux déclarations sont réellement identiques au sens de l'URL et de la méthode HTTP combinées.

Aucune solution technique n'a encore été choisie pour ce sprint, l'objectif ci-dessus est pour l'instant la seule chose arrêtée.

## Sprint-3-bis : Exécution des fonctions appelées

L'objectif de ce sprint est de pouvoir exécuter une fonction appelée.

## Sprint-4 : Appel au démarrage de l'appli web de toutes les listes de controllers

L'objectif de ce sprint est de permettre à l'appli d'avoir tout de suite la liste des controllers comme précédemment, mais au démarrage de l'appli.

Ce qui est recommandé est de faire un listener pour écouter le démarrage de l'appli et de pouvoir remplir le map d'url.

## Sprint-5 : ModelAndView
Créer une page de liste en récupérant les données en dure. Le contrôleur appelle directement les données et retourne un ModelAndView contenant la vue à afficher ainsi que les données à transmettre.

### Sprint-5-bis connection à Spring
Objectif : avoir un seul conteneur spring, laisser spring gerer repository et service, notre framework gere controller, on peut avoir de classes utile pour avoir ca
- dans frontcontrollerlistner :
    - ajout d'un variable static final SPRING_ROOT
    - mettre valeur de SPRING_ROOT à "org.springframework.web.context.WebApplicationContext.ROOT"
    - envoyer un attribut nommer springcontext qui contient la valeur de SPRING_ROOT dans le context :
        servletContext.setAttribute("springContext", servletContext.getAttribute(SPRING_ROOT))
- creation d'un classe Util :
    - ajout de la fonction ststic boolean haveParameter(Method methode, Class<?> param) qui verifie si une methode à la classe param comme parametre
- dans frontcontroller :
    - recuperer le springcontext depuis le context et caster en WebApplicationContext
    - changer l'invocation de la methode :
        - verifier si la methode attend une parmetre WebApplication avec la fonction haveParam()
            - si oui : 
                - si springcontext == null : throw excepltion pas de springcontext
                - invoker la methode en mettant en argument le springcontext :
                    - result = (ModelAndView) method.invoke(obj,  springContext);
            - sinon : invocation simple comme avant

### Sprint-6 : WebAPI
Objectif : construire un API, une methode sera appele et doit retourner automatiquement un json
- creation d'une nouvelle annotation appeler apirest, apiweb ou apicontroller
- dans frontcontroller :
    - avant execution du dispatch
        - si la methode de la classe contient la nouvelle annotation : 
            - retourne une application json
            - 2 cas, type de retour de la methode : 
                - String (deja du json) : on retourne tout de suite la string dans l'app json
                - autre que String (pas json) : on appelle une fonction toJSON pour convertir l'objet en JSON
            - retourner une application json
        - sinon : 
            - on garde l'ancienne execution

### Sprint-7 : Binding
Objectif : faire enregistrer les données envoyées depuis un formulaire par paramètres, sans instanciation d’objet pour l’instant.- Créer un formulaire
- [x] Créer un controller qui envoie vers le formulaire
  * [x] Pas encore de changement au niveau du framework
- [x] Modifier le FrontServlet
  * [x] Vérifier si la requête contient des paramètres
  * [x] Si aucun paramètre → faire un `invoke` simple
  * [wip] Si des paramètres existent :
    * récupérer les paramètres de la requête
    * récupérer leur nom et leur valeur
    * faire le matching avec les paramètres de la méthode du controller
    * ajouter les paramètres correspondants à l'appel de la méthode
    * faire le `invoke`
- Créer la méthode `save(...)` dans le controller
  * Recevoir les paramètres du formulaire
  * Pour l'instant, recevoir les paramètres directement, pas un objet
- Relier le formulaire au `save()`
  * Formulaire → bouton Submit
  * Submit → URL
  * URL → FrontServlet
  * FrontServlet → mapper le controller
  * Controller → `save(...)`
- Tester le binding
  * Vérifier les paramètres reçus avec `request.getParameter(...)`
  * Vérifier que le nom du paramètre du formulaire correspond au nom du paramètre de la méthode
  * Vérifier le nombre de paramètres
  * Vérifier le matching
  * Vérifier que les valeurs sont correctement passées au `invoke`
- Cas particulier
  * Pour l'instant, ce n'est pas un objet
  * Si l'objet est `null`, prendre d'abord les paramètres de la requête
  * L'instanciation et le binding vers un objet viendront plus tard

             
---

## Déploiement

```bash
# 1. Compiler le framework et copier le JAR
cd Phoenix && ./deploy.sh

# 2. Déployer l'application de test
cd ../phoenix-test && sudo ./deploy.sh
```

URL de test : `http://localhost:8080/phoenix-test/home`
