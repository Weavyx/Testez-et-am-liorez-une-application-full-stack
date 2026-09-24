# AUDIT — Traitement des erreurs de Bean Validation (`MethodArgumentNotValidException`)

Diagnostic **read-only**, back uniquement. Aucun fichier de code modifié.

Origine : en corrigeant les 3 tests `register_returns400_when*` de
`AuthControllerIT` (audit qualité des tests, point clarté #5), une vérification
empirique (`.andDo(print())` sur `register_returns400_whenEmailIsBlank`, voir
`AuthControllerIT.java` autour de la ligne 117) a montré que le corps de la
réponse 400 est **strictement vide**, contrairement aux autres 400 du projet
(ex. `Error: Email is already taken!`) qui portent un champ `"message"`.

---

## 1. GlobalExceptionHandler actuel

Fichier : `back/src/main/java/com/openclassrooms/starterjwt/exception/GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<?> handleNumberFormatException(NumberFormatException e) {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequestException(BadRequestException e) {
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> handleNotFoundException(NotFoundException e) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<?> handleForbiddenException(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}
```

| Exception gérée | Code HTTP | Corps produit |
|---|---|---|
| `NumberFormatException` | 400 | vide (`.build()`) |
| `BadRequestException` | 400 | `{"message": "..."}` **si** `getMessage()` non vide, sinon vide |
| `NotFoundException` | 404 | vide |
| `ForbiddenException` | 403 | vide |

**Aucun `@ExceptionHandler(MethodArgumentNotValidException.class)` n'existe.**
C'est la seule des 400 du projet à ne pas être interceptée par cette classe.

---

## 2. Endpoints concernés par le trou

Tous les endpoints qui portent `@Valid @RequestBody` sur un DTO d'entrée sont
concernés — l'échec de n'importe quelle contrainte Bean Validation sur ce DTO
lève `MethodArgumentNotValidException`, non gérée.

| Controller | Méthode | DTO validé (`@Valid`) | Règles de validation présentes |
|---|---|---|---|
| `AuthController` (`AuthController.java:38`) | `POST /api/auth/login` → `authenticateUser` | `LoginRequest` | `email`: `@NotBlank` · `password`: `@NotBlank` |
| `AuthController` (`AuthController.java:55`) | `POST /api/auth/register` → `registerUser` | `SignupRequest` | `email`: `@NotBlank` `@Size(max=50)` `@Email` · `firstName`: `@NotBlank` `@Size(min=3,max=20)` · `lastName`: `@NotBlank` `@Size(min=3,max=20)` · `password`: `@NotBlank` `@Size(min=6,max=40)` |
| `SessionController` (`SessionController.java:49`) | `POST /api/session` → `create` | `SessionDto` | `name`: `@NotBlank` `@Size(max=50)` · `date`: `@NotNull` · `teacher_id`: `@NotNull` · `description`: `@NotNull` `@Size(max=2500)` |
| `SessionController` (`SessionController.java:59`) | `PUT /api/session/{id}` → `update` | `SessionDto` | idem `create` |

`TeacherController` et `UserController` n'exposent aucun endpoint d'écriture
avec `@Valid` (lecture seule / suppression par id), donc hors périmètre.

---

## 3. Comportement réel du corps de réponse

Confirmé empiriquement via `mvn test -Dtest=AuthControllerIT#register_returns400_whenEmailIsBlank`
avec `.andDo(MockMvcResultHandlers.print())` ajouté temporairement sur ce test
(retiré ensuite, aucune trace dans le code actuel) :

```
2026-07-31T15:59:53.386+02:00  WARN [...] DefaultHandlerExceptionResolver :
Resolved [org.springframework.web.bind.MethodArgumentNotValidException:
Validation failed for argument [0] [...]: [Field error in object
'signupRequest' on field 'email': rejected value []; codes
[NotBlank.signupRequest.email,...]; default message [must not be blank]] ]

MockHttpServletResponse:
           Status = 400
    Error message = Invalid request content.
     Content type = null
             Body =
```

**Le corps est bien strictement vide** (`Content type = null`, `Body =`),
pas seulement dans un format différent. Explication : sans
`@ControllerAdvice` gérant `MethodArgumentNotValidException` et sans
`spring.mvc.problemdetails.enabled=true` (absent de `application.yml` et de
`application-test.yml`), Spring MVC résout l'exception via
`DefaultHandlerExceptionResolver`, qui appelle `response.sendError(400, "Invalid request content.")`
sans écrire de corps JSON lui-même. Le message `"Invalid request content."`
visible ci-dessus est le message d'erreur *servlet* (`HttpServletResponse#getErrorMessage()`),
pas un corps de réponse — il n'apparaît dans aucun JSON retourné au client et
n'est lisible que côté serveur/test (`MvcResult`), pas par un consommateur de
l'API. Aucun `data.sql` ni configuration `server.error.*` ne modifie ce
comportement dans ce projet.

Impact pratique : un appel direct à ces 4 endpoints avec un payload invalide
reçoit un `400` sans aucune information exploitable sur la cause, alors que
`register_returns400AndMessage_whenEmailIsAlreadyTaken` (même contrôleur,
même méthode) reçoit `{"message": "Error: Email is already taken!"}` pour un
400 différent. Incohérence de contrat au sein du même endpoint.

---

## 4. Impact côté front

| Composant | Lit-il un message depuis la réponse serveur ? |
|---|---|
| `front/src/app/pages/register/register.component.ts:57-65` | Non. `error: _ => this.onError = true` — le corps de l'erreur HTTP n'est jamais inspecté, `onError` est un simple booléen qui affiche un message générique fixe dans le template. |
| `front/src/app/pages/login/login.component.ts:45-56` | Non. Même pattern : `error: error => this.onError = true`, le paramètre `error` n'est pas utilisé au-delà de son existence. |
| `front/src/app/pages/sessions/components/form/form.component.ts:51-65` | Aucun gestionnaire d'erreur du tout sur `.subscribe(...)` des appels `create`/`update` — seul le cas `next` est traité. Une erreur HTTP (validation ou autre) ne produit donc aucun retour utilisateur visible ; elle resterait une erreur RxJS non gérée. |

Dans les trois cas, le formulaire Angular applique déjà sa propre validation
côté client (`Validators.required`, `Validators.email`, etc., visibles dans
chaque composant) qui bloque normalement la soumission avant tout appel HTTP.
Le trou backend est donc **invisible en usage normal via l'UI** (le front ne
lit `message` nulle part pour ces trois formulaires), mais reste pleinement
exploitable via un appel API direct (Postman, script, ou un futur client
frontend différent) qui contournerait la validation côté client — auquel cas
l'appelant ne reçoit qu'un statut 400 sans le moindre détail.

---

## 5. Conclusion et recommandation

**Recommandation : ajouter `@ExceptionHandler(MethodArgumentNotValidException.class)`
à `GlobalExceptionHandler`.**

Justifications :
- C'est la seule cause de 400 du projet sans corps exploitable, alors que le
  pattern `{"message": "..."}` est déjà établi et utilisé par
  `BadRequestException` — l'ajouter aligne ce cas sur le contrat existant
  plutôt que d'introduire un nouveau format.
- Concerne 4 endpoints d'écriture (`login`, `register`, `create session`,
  `update session`), donc un point de dette transverse, pas un cas isolé.
- Le correctif est localisé (un seul handler ajouté) et n'affecte aucun
  comportement testé ailleurs (les 400 actuels de ces endpoints resteraient
  400, seul le corps change).

Structure de réponse la plus cohérente avec l'existant : réutiliser
`MessageResponse` (déjà utilisé par `BadRequestException` et
`AuthController#registerUser` en cas de succès) avec le message de la
**première** erreur de champ, plutôt qu'une liste — le reste de l'API ne
renvoie jamais de liste d'erreurs et `MessageResponse` ne porte qu'un champ
`message` unique :

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    FieldError fieldError = e.getBindingResult().getFieldError();
    String message = fieldError != null
            ? "Error: " + fieldError.getField() + " " + fieldError.getDefaultMessage()
            : "Error: validation failed";
    return ResponseEntity.badRequest().body(new MessageResponse(message));
}
```

Ce changement est **hors périmètre du présent diagnostic** (read-only) et
n'a pas été appliqué ; il est décrit ici à titre de recommandation pour une
intervention ultérieure explicitement demandée.
