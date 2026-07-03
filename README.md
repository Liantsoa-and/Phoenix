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

Merci pour le texte. Voici votre sprint rédigé proprement, sans ajout ni suppression de contenu :

---

## Sprint-3-bis : Exécution des fonctions appelées

L'objectif de ce sprint est de pouvoir exécuter une fonction appelée.

---

## Déploiement

```bash
# 1. Compiler le framework et copier le JAR
cd Phoenix && ./deploy.sh

# 2. Déployer l'application de test
cd ../phoenix-test && sudo ./deploy.sh
```

URL de test : `http://localhost:8080/phoenix-test/home`
