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
Objectif : faire enregistrer les données envoyées depuis un formulaire par paramètres, sans instanciation d’objet pour l’instant.
- [x] Créer un formulaire
- [x] Créer un controller qui envoie vers le formulaire
  * [x] Pas encore de changement au niveau du framework
- [x] Modifier le FrontServlet
  * [x] Vérifier si la requête contient des paramètres
  * [x] Si aucun paramètre → faire un `invoke` simple
  * [x] Si des paramètres existent :
    * [x] récupérer les paramètres de la requête
    * [x] récupérer leur nom et leur valeur
    * [x] faire le matching avec les paramètres de la méthode du controller
    * [x] ajouter les paramètres correspondants à l'appel de la méthode
    * [x] faire le `invoke`
- [x] Créer la méthode `save(...)` dans le controller
  * [x] Recevoir les paramètres du formulaire
  * [x] Pour l'instant, recevoir les paramètres directement, pas un objet
- [x] Relier le formulaire au `save()`
  * [x] Formulaire → bouton Submit
  * [x] Submit → URL
  * [x] URL → FrontServlet
  * [x] FrontServlet → mapper le controller
  * [x] Controller → `save(...)`
- [x] Tester le binding
  * [x] Vérifier les paramètres reçus avec `request.getParameter(...)`
  * [x] Vérifier que le nom du paramètre du formulaire correspond au nom du paramètre de la méthode
  * [x] Vérifier le nombre de paramètres
  * [x] Vérifier le matching
  * [x] Vérifier que les valeurs sont correctement passées au `invoke`
- [x] Cas particulier
  * [x] Pour l'instant, ce n'est pas un objet
  * [x] Si l'objet est `null`, prendre d'abord les paramètres de la requête
  * [x] L'instanciation et le binding vers un objet viendront plus tard

### Sprint-7 bis : Binding avec objet
Objectif : pouvoir recevoir un objet (ex: `Employe`) dans la méthode du controller. Le framework instancie l’objet et remplit ses propriétés à partir des paramètres du formulaire.
- [x] Modifier `Util.getParameter(...)`
  - [x] Détecter si le paramètre de la méthode est un type simple (`String`, `int`…) ou un objet
  - [x] Si type simple → garder le comportement actuel
  - [x] Si objet :
    - [x] Créer une instance avec le constructeur sans argument
    - [x] Pour chaque propriété de l’objet, récupérer la valeur dans la requête et appeler le setter
    - [x] Mettre l’objet rempli dans les arguments
- [x] Modifier la méthode `save` du controller
  - [x] Signature : `public Employe save(Employe employe)`
  - [x] L’objet arrive déjà rempli
  - [x] Retourner l’objet
- [x] Tester
  - [x] Envoyer le formulaire
  - [x] Vérifier que l’objet est bien créé et rempli (`nom`, `poste`…)
  - [x] Vérifier que le binding simple continue de marcher

### Sprint-8 : Upload fichier
Objectif : dans le formulaire on pourra envoyer un fichier, le fichier pourra etre traiter dans le framework, face au solution propose voici ce que j'ai pris :
Voici une version du Sprint-8 dans le même format que le 7 :

### Sprint-8 : Upload fichier
Objectif : pouvoir envoyer un fichier depuis un formulaire et le récupérer dans la méthode du controller sous forme d'un objet `Fichier`, en réutilisant le mécanisme de binding existant.
- [x] Créer la classe `Fichier`
  - [x] Constructeur sans argument
  - [x] Propriétés : `nom`, `typeContenu`, `taille`, `contenu` (`byte[]`)
  - [x] Getters / setters
- [x] Préparer le formulaire
  - [x] Ajouter `enctype="multipart/form-data"` et `method="post"`
  - [x] Ajouter un `<input type="file" name="...">`
  - [x] Garder un champ texte normal pour vérifier que le mélange texte + fichier marche
- [x] Activer le multipart dans le framework
  - [x] Ajouter `@MultipartConfig` sur le `FrontControllerServlet`
  - [x] Vérifier que les requêtes non multipart gardent le même chemin de traitement
- [x] Modifier `Util.getParameter(...)`
  - [x] Détecter si le paramètre de la méthode est de type `Fichier`
  - [x] Si oui : récupérer la `Part` correspondante avec `request.getPart(nom)`
  - [x] Remplir l'objet `Fichier` (nom, type, taille, contenu) à partir de la `Part`
  - [x] Gérer le cas où aucun fichier n'est envoyé (`null`)
  - [x] Si autre type : garder le comportement actuel (type simple / objet)
- [x] Créer la méthode du controller
  - [x] Signature du test mixte : `public ModelAndView upload(Employe employe, Poste poste, Fichier fichier)`
  - [x] Relier formulaire → URL → FrontServlet → controller
- [x] Enregistrer le fichier dans le projet de test
  - [x] Lire le dossier cible dans `phoenix-test-fixed/webapp/WEB-INF/upload.properties`
  - [x] Créer le dossier si nécessaire et éviter l'écrasement avec un nom unique
- [X] Tester
  - [x] Envoyer un petit fichier et vérifier `nom`, `typeContenu` et `taille`
  - [x] Vérifier que le contenu est identique à l'original
  - [x] Tester sans fichier
  - [X] Tester avec un fichier + des champs texte
  - [X] Vérifier que le binding simple et le binding objet marchent toujours

### Sprint-9 : Session
Objectif : rendre disponible la session HTTP de manière transparente dans les controllers de l'application développeur. Plutôt que d'exposer directement le `HttpSession` du servlet container, le framework encapsule la session dans une classe `PhoenixSession` qu'il injecte automatiquement dans les méthodes de controllers qui la demandent, exactement comme il le fait pour `ApplicationContext`. Le développeur peut ainsi lire et écrire des attributs en session sans jamais toucher aux APIs Jakarta.
- [ ] **Finaliser le contrat de `PhoenixSession`** (package `session`)
  - [x] Garder une référence privée et finale au `HttpSession`; ne pas maintenir une copie locale des attributs, afin que les lectures reflètent toujours la session réelle
  - [x] Conserver le constructeur `PhoenixSession(HttpSession session)`
  - [x] Implémenter `setAttribute(String key, Object value)` et `removeAttribute(String key)` en déléguant au `HttpSession`
  - [x] Implémenter `getAttribute(String key)` et `getAttribute(String key, Class<T> type)`; la variante typée vérifie le type et signale clairement une incompatibilité
  - [x] Implémenter `getAttributes()` comme une copie des attributs présents dans le `HttpSession`, pour que modifier la map retournée ne modifie pas implicitement la session
  - [x] Implémenter `clear()` en supprimant tous les attributs présents dans le `HttpSession`
  - [x] Implémenter `getId()` en retournant l'ID de la session sous-jacente
- [x] **Injecter `PhoenixSession` dans `Util.getParameter()`**
  - [x] Ajouter un cas explicite pour le type `PhoenixSession` avant le binding générique des objets
  - [x] Construire le wrapper avec `new PhoenixSession(req.getSession())`; la session est ainsi créée lorsqu'une méthode de controller demande ce paramètre
  - [x] Garder la résolution existante de `WebApplicationContext`, `Fichier`, des types simples et des objets inchangée
  - [x] Vérifier que `haveParameter(method, PhoenixSession.class)` reconnaît bien le paramètre; la méthode générique actuelle devrait déjà le faire
- [x] **Valider l'injection par le FrontController**
  - [x] Confirmer que `FrontControllerServlet.processRequest()` transmet toujours `req` à `Util.getParameter()` — c'est déjà le cas dans le code actuel
  - [x] Vérifier qu'une méthode de controller avec `PhoenixSession` reçoit le wrapper et que l'invocation continue de fonctionner avec d'autres paramètres
- [x] **Ajouter un scénario dans l'application témoin**
  - [x] Ajouter `PhoenixSession` à une méthode de controller existante ou à un controller de démonstration
  - [x] Incrémenter un attribut `requestCount` à chaque appel afin de vérifier la persistance entre requêtes
  - [x] Ajouter `GET /api/session` qui retourne les attributs en JSON pour observer la session courante
  - [x] Ajouter `GET /api/session/reset` qui appelle `clear()` et retourne `"OK"`
- [x] **Vérifier le Sprint 9 et les fonctions déjà présentes**
  - [x] Appeler plusieurs fois la route de compteur avec le même cookie de session et vérifier l'incrémentation
  - [x] Vérifier les lectures typées et non typées, l'ajout, la suppression, l'ID et la liste des attributs
  - [x] Réinitialiser la session et vérifier que les attributs ont été supprimés
  - [x] Vérifier l'injection de `PhoenixSession` en même temps que les paramètres simples, les objets, `Fichier` et `WebApplicationContext`
  - [x] Vérifier que le comportement des sprints précédents (binding, upload et contexte Spring) reste opérationnel
  Validation : compilation du framework et de l'application témoin, tests des opérations du wrapper et du binding combiné avec un contexte Spring simulé, puis appels HTTP sur un Tomcat temporaire au port 18080. Le compteur a renvoyé 1 puis 2 avec le même `JSESSIONID`; après reset, il est reparti à 1. Le binding objet et l'upload multipart ont également répondu correctement.

---


## Sprint-10 : Authentification et autorisation

Objectif : protéger les routes du framework avec une annotation déclarable sur une classe ou une méthode. Une session authentifiée suffit si aucun rôle n'est demandé; si un rôle est précisé, l'utilisateur doit le posséder. Un refus doit produire une réponse HTTP 401 (non authentifié) ou 403 (rôle insuffisant), en JSON pour les routes API et en HTML pour les pages.

- [x] **Définir l'annotation `Auth`**
  - [x] Créer `@Auth` avec les cibles `TYPE` et `METHOD` et une rétention à l'exécution.
  - [x] Ajouter `role()` avec `""` comme valeur par défaut.
  - [x] Documenter le contrat : rôle vide = authentification seule; rôle renseigné = authentification et rôle exigé.
- [ ] **Rendre la configuration d'authentification accessible au contrôleur**
  - [x] Charger depuis `web.xml` le nom de l'attribut de session contenant le ou les rôles de l'utilisateur.
  - [x] Publier cette valeur dans le `ServletContext` depuis `FrontServletContextListener` et la lire dans `FrontServletController`.
  - [ ] Mettre à jour la configuration de `test-source` et préciser le nom de l'attribut testé.
- [x] **Centraliser les réponses d'accès refusé dans `ResponseHelper`**
  - [x] Ajouter `handleUnauthorized` pour les requêtes sans utilisateur authentifié (HTTP 401).
  - [x] Ajouter `handleForbidden` pour les utilisateurs authentifiés sans le rôle demandé (HTTP 403).
  - [x] Produire du JSON pour les routes API et du HTML pour les pages, avec un choix de format cohérent avec le type de route.
- [x] **Vérifier les autorisations avant l'invocation**
  - [x] Ajouter `checkAuthorization` dans `FrontController` et l'appeler avant de résoudre/invoquer la méthode.
  - [x] Chercher `@Auth` sur la méthode d'abord, puis sur la classe seulement si la méthode n'en porte pas.
  - [x] Définir l'authentification par la présence de l'attribut de session configuré; appeler `handleUnauthorized` s'il manque.
  - [x] Autoriser l'accès authentifié si `role()` est vide.
  - [x] Pour un rôle requis, accepter un attribut de session `String` ou une `Collection`; appeler `handleForbidden` si le rôle n'y figure pas.
  - [x] Interrompre le traitement après une réponse 401 ou 403 afin que la méthode du contrôleur ne soit jamais exécutée.
- [x] **Compléter les scénarios dans `test-source`**
  - [x] Protéger des routes avec `@Auth` au niveau classe et au niveau méthode.
  - [x] Vérifier qu'une annotation de méthode remplace celle de la classe, y compris un rôle vide.
  - [x] Vérifier l'accès authentifié sans rôle requis, l'accès avec le bon rôle et le refus avec un rôle absent.
  - [x] Couvrir les attributs de rôle sous forme de `String` et de `Collection`.
  - [x] Vérifier les réponses 401/403 en JSON pour une API et en HTML pour une page.
  - [x] Vérifier qu'aucun contrôleur protégé n'est invoqué après un refus et que login/logout font évoluer l'accès comme attendu.

## Sprint-10-bis : Autorisation par rôles

Objectif : permettre de déclarer sur une méthode les rôles autorisés à l’appeler. Par exemple, `@RoleRequired({"admin", "RH"})` autorise les utilisateurs possédant le rôle `admin` **ou** le rôle `RH`.

- [x] **Créer l’annotation `RoleRequired`**
  - [x] La rendre applicable aux méthodes et disponible à l’exécution.
  - [x] Lui ajouter un attribut contenant une liste de rôles, par exemple `String[] value()`.
  - [x] Utiliser l’annotation ainsi : `@RoleRequired({"admin", "RH"})`.

- [x] **Vérifier les rôles avant l’appel de la méthode**
  - [x] Lire les rôles autorisés dans `@RoleRequired`.
  - [x] Récupérer le rôle ou les rôles de l’utilisateur depuis l’attribut de session configuré.
  - [x] Autoriser l’appel si l’utilisateur possède au moins un des rôles déclarés.
  - [x] Refuser l’accès si aucun rôle ne correspond et ne pas appeler la méthode du contrôleur.

- [x] **Gérer les accès refusés**
  - [x] Retourner HTTP 401 si l’utilisateur n’est pas authentifié.
  - [x] Retourner HTTP 403 si l’utilisateur est authentifié, mais ne possède aucun rôle autorisé.
  - [x] Renvoyer une erreur JSON pour une route API et une erreur HTML pour une page.

- [x] **Ajouter des scénarios de test**
  - [x] Vérifier l’accès avec chacun des rôles autorisés.
  - [x] Vérifier le refus avec un rôle non autorisé.
  - [x] Vérifier le refus pour un utilisateur non authentifié.
  - [x] Vérifier que la méthode protégée n’est pas appelée après un refus.
  - [x] Vérifier les réponses 401 et 403 en JSON et en HTML.

---

## Déploiement

```bash
# 1. Compiler le framework et copier le JAR
cd Phoenix && ./deploy.sh

# 2. Déployer l'application de test
cd ../phoenix-test && sudo ./deploy.sh
```

---

## Pour les logs de debug : catalina.out
```bash
tail -f /var/log/tomcat10/catalina.out
```

## activer tomlog : 

```bash
alias tomlog='tail -f /var/log/tomcat10/catalina.out'
```
