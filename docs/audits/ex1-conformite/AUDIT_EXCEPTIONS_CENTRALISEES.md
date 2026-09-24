# Audit — Centralisation de la gestion des exceptions (controllers)

**Date** : 2026-08-07
**Branche** : `chore/verification-finale-livrable-p4`
**Type** : audit lecture seule, aucune modification de fichier

## Consigne auditée

Ex1-Back (`ETAPES_RECOMMANDEE.md`) : « Supprimez la multitude d'instructions
try{} catch{} et centralisez la gestion des exceptions [via
GlobalExceptionHandler] pour réduire la duplication de code. »

Objectif de cet audit : vérifier qu'aucun try/catch de gestion
métier/HTTP n'a été réintroduit dans les controllers depuis Ex1, et que
`GlobalExceptionHandler` couvre bien tous les types d'exception métier
utilisés par les services.

## Méthode

1. `grep -rn "try\s*{"` puis `grep -rni "try|catch"` sur
   `back/src/main/java/com/openclassrooms/starterjwt/controllers/`.
2. Lecture intégrale des 4 controllers (`AuthController`,
   `SessionController`, `TeacherController`, `UserController`).
3. Lecture intégrale de `GlobalExceptionHandler`.
4. `grep -rn "throw new (ForbiddenException|NotFoundException|
   BadRequestException|NumberFormatException)"` sur tout le module
   `starterjwt` pour identifier les types d'exception métier réellement
   levés par la couche services.

## Résultat 1 — Recherche de try/catch dans les controllers

Aucune occurrence de `try` ou `catch` (recherche insensible à la casse,
y compris commentaires et chaînes de caractères) n'a été trouvée dans
les 4 fichiers de `controllers/` :

- `AuthController.java` — 0 occurrence
- `SessionController.java` — 0 occurrence
- `TeacherController.java` — 0 occurrence
- `UserController.java` — 0 occurrence

Les 4 controllers laissent remonter les exceptions telles quelles
(`Long.valueOf(id)`, `sessionService.getById(...)`, etc.) sans aucune
capture locale. **0 try/catch résiduel confirmé.**

## Résultat 2 — Couverture de GlobalExceptionHandler

`src/main/java/com/openclassrooms/starterjwt/exception/GlobalExceptionHandler.java`
est annoté `@RestControllerAdvice` et expose les `@ExceptionHandler`
suivants :

| Exception                          | Handler                                   | Réponse HTTP |
|-------------------------------------|--------------------------------------------|--------------|
| `NumberFormatException`             | `handleNumberFormatException`               | 400 Bad Request |
| `BadRequestException`               | `handleBadRequestException`                 | 400 Bad Request (avec message si présent) |
| `NotFoundException`                 | `handleNotFoundException`                   | 404 Not Found |
| `ForbiddenException`                | `handleForbiddenException`                  | 403 Forbidden |
| `MethodArgumentNotValidException`   | `handleMethodArgumentNotValidException`     | 400 Bad Request (avec détail des erreurs de champ) |

## Résultat 3 — Vérification croisée avec les exceptions réellement levées

`grep` sur tout le module confirme que les seules exceptions métier
levées via `throw new ...` proviennent de `UserService.java` et
`SessionService.java`, et sont de type `ForbiddenException`,
`NotFoundException` ou `BadRequestException` — toutes couvertes par
`GlobalExceptionHandler` (cf. tableau ci-dessus).

`NumberFormatException` n'est pas levée explicitement par le code métier
mais peut survenir implicitement dans les controllers via
`Long.valueOf(id)` / `Long.parseLong(id)` sur un `@PathVariable` non
numérique (ex. `GET /api/session/abc`) ; elle est également couverte par
le handler global, donc correctement centralisée même si elle n'est pas
« throw new » explicitement dans les services.

`MethodArgumentNotValidException` est levée automatiquement par Spring
lors de la validation `@Valid` sur les DTO (`SessionDto`, `LoginRequest`,
`SignupRequest`) ; couverte également.

## Conclusion

- **0 try/catch trouvé** dans les 4 controllers (`Auth`, `Session`,
  `Teacher`, `User`).
- **Tous les types d'exception métier/HTTP utilisés dans le code**
  (`NumberFormatException`, `BadRequestException`, `NotFoundException`,
  `ForbiddenException`, `MethodArgumentNotValidException`) **sont
  couverts par un `@ExceptionHandler` dans `GlobalExceptionHandler`**.

La centralisation de la gestion des exceptions issue d'Ex1 est donc
**intacte** : aucune régression locale (try/catch réintroduit) n'a été
détectée dans les controllers malgré les évolutions ultérieures
(introduction de `ForbiddenException`, typage explicite en
`ResponseEntity<X>`), et l'absence de try/catch dans les controllers
n'est pas due à une exception non interceptée remontant en 500 — chaque
type est bien mappé sur un code HTTP explicite dans le handler global.

**Aucune correction n'a été appliquée** conformément au périmètre de cet
audit (lecture seule).
