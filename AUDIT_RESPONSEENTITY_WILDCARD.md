# Audit — usages de `ResponseEntity<?>` dans le package controller/exception

## Résumé

Recherche exhaustive (`grep -rn "ResponseEntity<?>" back/src/main/java`) : **18 occurrences**
trouvées, contre les **15 attendues** dans la commande initiale (4 controllers). Écart de **+3**,
entièrement expliqué par `GlobalExceptionHandler` (5 méthodes, non compté dans le périmètre
initial de "4 controllers") :

| Fichier | Occurrences |
|---|---|
| `controllers/AuthController.java` | 2 |
| `controllers/SessionController.java` | 7 |
| `controllers/TeacherController.java` | 2 |
| `controllers/UserController.java` | 2 |
| `exception/GlobalExceptionHandler.java` | 5 |
| **Total** | **18** |

Aucune autre occurrence de `ResponseEntity<?>` n'existe ailleurs dans `back/src/main/java`
(recherche confirmée sur l'arborescence complète, pas seulement `controllers/`). Si le chiffre de
15 provenait d'un décompte limité aux 4 controllers, il est inexact : ces 4 fichiers totalisent
**13** occurrences (2+7+2+2), pas 15 — l'écart avec "15" reste donc non expliqué au-delà de
l'hypothèse ci-dessus ; à confirmer avec la source du chiffre 15.

Aucun fichier de production ni de test n'a été modifié dans le cadre de cet audit.

## Tableau détaillé

| Fichier | Méthode | Type(s) de succès réellement renvoyé(s) dans le corps | Tous les cas d'échec passent-ils par GlobalExceptionHandler ? | Retour d'erreur construit à la main dans la méthode ? | Type cible proposé | Risque test associé |
|---|---|---|---|---|---|---|
| AuthController.java | `authenticateUser` (POST /api/auth/login) | `JwtResponse` (toujours) | **Non** : `AuthenticationException` (ex. `BadCredentialsException`) levée par `authenticationManager.authenticate(...)` n'est interceptée par aucun `@ExceptionHandler` de `GlobalExceptionHandler` ; elle remonte via la chaîne de filtres Spring Security et est traduite en 401 par l'`authenticationEntryPoint` de `WebSecurityConfig`, en dehors du périmètre du handler. | Non | `ResponseEntity<JwtResponse>` | `AuthControllerIT.login_returns200AndJwtResponse_whenCredentialsAreValid` fait des assertions `jsonPath` fines (`id`, `username`, `firstName`, `lastName`, `admin`, `type`, `token`) sur le corps JSON — compatible avec un typage strict en `JwtResponse`, pas de risque identifié. |
| AuthController.java | `registerUser` (POST /api/auth/register) | `MessageResponse` (toujours) | Oui : la seule erreur métier possible (`BadRequestException` si email déjà pris) passe par `handleBadRequestException`. Les erreurs de validation Bean Validation (`@Valid`) passent par `handleMethodArgumentNotValidException`. | Non | `ResponseEntity<MessageResponse>` | `AuthControllerIT.register_returns200AndSuccessMessage_whenDataIsValid` vérifie `jsonPath("$.message")` — compatible avec `MessageResponse`, pas de risque identifié. |
| SessionController.java | `findById` (GET /api/session/{id}) | `SessionDto` (toujours, via `sessionMapper.toDto`) | Oui : id inexistant → `NotFoundException` (`SessionService.getById`) → `handleNotFoundException`. Id non numérique → `NumberFormatException` (`Long.valueOf`) → `handleNumberFormatException`. | Non | `ResponseEntity<SessionDto>` | `SessionControllerIT.findById_returns200AndSession_whenSessionExists` vérifie plusieurs champs (`id`, `name`, `description`, `teacher_id`, `date`, `users`, `createdAt`, `updatedAt`) — compatible avec `SessionDto`, pas de risque identifié. |
| SessionController.java | `findAll` (GET /api/session) | `List<SessionDto>` (toujours) | Oui (aucune branche d'échec dans cette méthode ; seule l'auth peut échouer en amont, hors du controller). | Non | `ResponseEntity<List<SessionDto>>` | `SessionControllerIT.findAll_returns200AndAllSessions_whenAuthenticated` vérifie `hasSize`/`containsInAnyOrder` sur un tableau JSON — compatible, pas de risque identifié. |
| SessionController.java | `create` (POST /api/session) | `SessionDto` (toujours) | Oui : erreurs de validation (`@Valid`) → `handleMethodArgumentNotValidException`. Aucune autre exception métier levée par `SessionService.create`. | Non | `ResponseEntity<SessionDto>` | `SessionControllerIT.create_returns200_whenCalledByAdmin` vérifie plusieurs champs du DTO — compatible, pas de risque identifié. |
| SessionController.java | `update` (PUT /api/session/{id}) | `SessionDto` (toujours) | Oui : id inexistant → `NotFoundException` → `handleNotFoundException`. Id non numérique → `NumberFormatException` → `handleNumberFormatException`. Validation (`@Valid`) → `handleMethodArgumentNotValidException`. | Non | `ResponseEntity<SessionDto>` | `SessionControllerIT.update_returns200_whenCalledByAdmin` vérifie plusieurs champs du DTO — compatible, pas de risque identifié. |
| SessionController.java | `save` (DELETE /api/session/{id}) | Aucun corps (`ResponseEntity.ok().build()`) | Oui : id inexistant → `NotFoundException` (`SessionService.delete`) → `handleNotFoundException`. Id non numérique → `NumberFormatException` → `handleNumberFormatException`. | Non | `ResponseEntity<Void>` | Aucun test n'inspecte le corps de la réponse (seul le statut est vérifié) — pas de risque identifié. |
| SessionController.java | `participate` (POST /api/session/{id}/participate/{userId}) | Aucun corps (`ResponseEntity.ok().build()`) | Oui : session ou user inexistant → `NotFoundException` → `handleNotFoundException`. Id/userId non numérique → `NumberFormatException` → `handleNumberFormatException`. Déjà participant → `BadRequestException` → `handleBadRequestException`. Propriété non respectée → `ForbiddenException` (`SessionService.assertRequestingUserIsSelf`) → `handleForbiddenException`. | Non | `ResponseEntity<Void>` | Aucun test n'inspecte le corps — pas de risque identifié. |
| SessionController.java | `noLongerParticipate` (DELETE /api/session/{id}/participate/{userId}) | Aucun corps (`ResponseEntity.ok().build()`) | Oui : mêmes chemins d'exception que `participate` (`NotFoundException`, `NumberFormatException`, `BadRequestException` si non-participant, `ForbiddenException`). | Non | `ResponseEntity<Void>` | Aucun test n'inspecte le corps — pas de risque identifié. |
| TeacherController.java | `findById` (GET /api/teacher/{id}) | `TeacherDto` (toujours, via `teacherMapper.toDto`) | Oui : id inexistant → `NotFoundException` (`TeacherService.findById`) → `handleNotFoundException`. Id non numérique → `NumberFormatException` → `handleNumberFormatException`. | Non | `ResponseEntity<TeacherDto>` | `TeacherControllerIT.findById_returns200AndTeacher_whenTeacherExists` vérifie plusieurs champs du DTO — compatible, pas de risque identifié. |
| TeacherController.java | `findAll` (GET /api/teacher) | `List<TeacherDto>` (toujours) | Oui (aucune branche d'échec dans cette méthode). | Non | `ResponseEntity<List<TeacherDto>>` | `TeacherControllerIT.findAll_returns200AndAllTeachers_whenAuthenticated` vérifie un tableau JSON — compatible, pas de risque identifié. |
| UserController.java | `findById` (GET /api/user/{id}) | `UserDto` (toujours, via `userMapper.toDto`) | Oui : id inexistant → `NotFoundException` (`UserService.findById` via `findOwnProfile`) → `handleNotFoundException`. Id non numérique → `NumberFormatException` → `handleNumberFormatException`. Compte différent du sien → `ForbiddenException` (`UserService.assertRequestingUserIsSelf`) → `handleForbiddenException`. | Non | `ResponseEntity<UserDto>` | `UserControllerIT.findById_returns200AndUserDto_whenUserReadsOwnAccount` vérifie de nombreux champs (`id`, `email`, `firstName`, `lastName`, `admin`, `createdAt`, `updatedAt`) et l'absence de `password` — compatible avec `UserDto`, pas de risque identifié. |
| UserController.java | `save` (DELETE /api/user/{id}) | Aucun corps (`ResponseEntity.ok().build()`) | Oui : id inexistant → `NotFoundException` (`UserService.deleteById` via `findById`) → `handleNotFoundException`. Id non numérique → `NumberFormatException` (`Long.parseLong`) → `handleNumberFormatException`. Compte différent du sien → `ForbiddenException` → `handleForbiddenException`. | Non | `ResponseEntity<Void>` | Aucun test n'inspecte le corps — pas de risque identifié. |
| GlobalExceptionHandler.java | `handleNumberFormatException` | Aucun corps (`ResponseEntity.badRequest().build()`) | N/A (c'est le handler lui-même) | Oui, ligne 17 : `ResponseEntity.badRequest().build()` | `ResponseEntity<Void>` | Plusieurs tests (`*_returns400_whenIdIsNotNumeric` dans les 4 IT controllers) vérifient uniquement le statut, pas le corps — pas de risque identifié. |
| GlobalExceptionHandler.java | `handleBadRequestException` | Deux cas distincts : (1) `MessageResponse` si `e.getMessage()` non vide ; (2) aucun corps (`ResponseEntity.badRequest().build()`) sinon | N/A (c'est le handler lui-même) | Oui, lignes 23 et 25 : `ResponseEntity.badRequest().body(...)` et `ResponseEntity.badRequest().build()` | À arbitrer (deux types de succès/échec distincts selon la présence d'un message) | `AuthControllerIT.register_returns400AndMessage_whenEmailIsAlreadyTaken` vérifie `jsonPath("$.message")` (cas avec message) ; `SessionControllerIT.participate_returns400_whenAlreadyParticipating` et `noLongerParticipate_returns400_whenNotParticipating` ne vérifient que le statut (cas sans message, `BadRequestException` levée sans argument dans `SessionService`). Un typage unique casserait soit le cas avec corps, soit imposerait un corps toujours présent (actuellement absent dans le cas sans message) — à arbitrer. |
| GlobalExceptionHandler.java | `handleNotFoundException` | Aucun corps (`ResponseEntity.notFound().build()`) | N/A (c'est le handler lui-même) | Oui, ligne 30 : `ResponseEntity.notFound().build()` | `ResponseEntity<Void>` | Tous les tests `*_returns404_*` ne vérifient que le statut — pas de risque identifié. |
| GlobalExceptionHandler.java | `handleForbiddenException` | Aucun corps (`ResponseEntity.status(HttpStatus.FORBIDDEN).build()`) | N/A (c'est le handler lui-même) | Oui, ligne 35 : `ResponseEntity.status(HttpStatus.FORBIDDEN).build()` | `ResponseEntity<Void>` | `UserControllerIT.findById_returns403_whenUserReadsAnotherUsersAccount` vérifie explicitement `jsonPath("$.email").doesNotExist()` (absence de fuite de données), cohérent avec un corps vide — pas de risque identifié. |
| GlobalExceptionHandler.java | `handleMethodArgumentNotValidException` | `MessageResponse` (toujours) | N/A (c'est le handler lui-même) | Oui, ligne 43 : `ResponseEntity.badRequest().body(new MessageResponse(message))` | `ResponseEntity<MessageResponse>` | Plusieurs `AuthControllerIT.register_returns400_when...` vérifient `jsonPath("$.message")` avec des contenus précis (agrégation multi-champs) — compatible avec `MessageResponse`, pas de risque identifié. |

## Points d'attention transverses (factuels, sans recommandation)

- **`handleBadRequestException` est le seul cas où le type de succès/échec varie au sein d'une
  même méthode** (corps présent ou absent selon si `BadRequestException` a été construite avec ou
  sans message). C'est la seule ligne du tableau marquée "à arbitrer".
- Tous les endpoints acceptant un `@PathVariable("id") String id` (et le convertissant via
  `Long.valueOf`/`Long.parseLong`) partagent le chemin d'échec `NumberFormatException` →
  `handleNumberFormatException`, qui renvoie toujours un corps vide.
- `AuthController.authenticateUser` est la seule méthode dont un chemin d'échec (mauvais
  identifiants / email inconnu) ne transite pas par `GlobalExceptionHandler` : le 401 est produit
  en amont par la configuration Spring Security (`authenticationEntryPoint`), documenté
  explicitement dans le Javadoc de `AuthControllerIT`.
