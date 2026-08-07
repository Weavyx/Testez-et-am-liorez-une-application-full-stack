# Audit — Logique métier dans les controllers (Ex1-Back)

**Type** : audit read-only, aucune modification de code.
**Périmètre** : `AuthController`, `SessionController`, `TeacherController`, `UserController`.
**Objectif** : vérifier que les corrections apportées lors d'Ex1 (déport de la logique métier vers les services) n'ont pas été érodées par les évolutions post-Ex1 (`ForbiddenException`, `assertRequestingUserIsSelf`, typage `ResponseEntity<X>`, etc.).

## Méthode

Relecture intégrale du corps de chaque méthode des 4 controllers (pas de grep par mots-clés). Pour chaque méthode, application de la grille suivante :

- **Orchestration pure** : parsing d'un `@PathVariable`, appel à **une seule** méthode de service, construction de la `ResponseEntity`.
- **Cas limite assumé** : le parsing d'id (`Long.valueOf`/`Long.parseLong`) lève une exception non catchée dans le controller, gérée par `GlobalExceptionHandler` — c'est l'architecture assumée du projet, signalé mais non compté comme violation.
- **Logique métier détectée** : `if` sur une condition métier, comparaison de valeurs métier, boucle sur une collection métier, ou enchaînement de deux appels de service orchestré depuis le controller.

## Résultats détaillés

| Fichier | Méthode | Verdict | Justification |
|---|---|---|---|
| AuthController.java | `authenticateUser` | orchestration pure | Authentifie via `AuthenticationManager`, place le contexte de sécurité, extrait les infos du principal et construit le DTO de réponse. Aucune décision métier : la lecture des champs de `userDetails` et l'appel à `userService.isAdmin(...)` sont de la construction de réponse, pas un calcul métier. |
| AuthController.java | `registerUser` | orchestration pure | Un seul appel à `userService.register(...)`, la vérification "email déjà pris" vit dans `UserService.register`. |
| SessionController.java | `findById` | cas limite assumé | `Long.valueOf(id)` peut lever `NumberFormatException` non catchée dans le controller ; le reste est un appel service unique + mapping. |
| SessionController.java | `findAll` | orchestration pure | Un appel à `sessionService.findAll()`, mapping vers DTO, réponse. |
| SessionController.java | `create` | orchestration pure | Les `log.info` sont de la journalisation, pas une décision métier. Un seul appel à `sessionService.create(...)`. |
| SessionController.java | `update` | cas limite assumé | `Long.parseLong(id)` est le seul point sensible ; sinon un seul appel service. |
| SessionController.java | `save` (DELETE `/{id}`) | cas limite assumé | Idem parsing d'id ; `sessionService.delete(...)` porte seule la vérification d'existence (404). |
| SessionController.java | `participate` | cas limite assumé | Double parsing d'id (`id`, `userId`) ; un seul appel à `sessionService.participate(id, userId)`, qui porte en interne la vérification d'existence, le contrôle de propriété (`assertRequestingUserIsSelf`) et le contrôle de doublon. |
| SessionController.java | `noLongerParticipate` | cas limite assumé | Même structure que `participate`, un seul appel à `sessionService.noLongerParticipate(...)`. |
| TeacherController.java | `findById` | cas limite assumé | `Long.valueOf(id)` seul point sensible ; un seul appel `teacherService.findById(...)`. |
| TeacherController.java | `findAll` | orchestration pure | Un seul appel `teacherService.findAll()`, mapping, réponse. |
| UserController.java | `findById` | cas limite assumé | `Long.valueOf(id)` seul point sensible ; un seul appel à `userService.findOwnProfile(id)`, qui porte en interne la vérification d'existence (404) **et** le contrôle de propriété (`assertRequestingUserIsSelf` → 403). Le controller ne fait ni la vérification d'existence ni le contrôle d'accès lui-même : il n'orchestre pas deux appels, il délègue à une méthode de service unique dédiée à ce cas d'usage. |
| UserController.java | `save` (DELETE `/{id}`) | cas limite assumé | `Long.parseLong(id)` seul point sensible ; un seul appel à `userService.deleteById(id)`, qui porte en interne l'existence, le contrôle de propriété et la désinscription des sessions. |

## Résumé

- **Méthodes auditées** : 14 (2 Auth + 7 Session + 2 Teacher + 2 User — hors constructeurs).
- **Violations réelles (logique métier dans un controller)** : **0**.
- **Cas limites assumés (parsing d'id)** : 9, présents dans les 4 controllers, non comptés comme violation par consigne — cas préexistant à Ex1 et jamais requalifié depuis (architecture assumée du projet, à trancher en conversationnel si on souhaite un jour le changer).

Aucune régression n'a été détectée : chaque méthode de controller délègue à un point d'entrée service unique portant l'intégralité de la décision métier (existence, propriété via `assertRequestingUserIsSelf`, doublons de participation, désinscription en cascade). Les évolutions post-Ex1 examinées dans la consigne (introduction de `ForbiddenException`, de `assertRequestingUserIsSelf`, du typage `ResponseEntity<X>`) ont toutes été implémentées **côté service** (`SessionService`, `UserService`) ou comme changement de signature pur côté controller (typage), sans réintroduire de branchement conditionnel métier dans les controllers.

Historique confirmé par `git log` sur les controllers : les commits `move session existence checks to SessionService`, `move teacher existence check to TeacherService`, `move user business logic to UserService`, `restreint GET /api/user/{id} au compte de l'appelant` et `compare les id plutôt que les emails pour la propriété du compte` ont tous ajouté leur logique dans la couche service, jamais dans les controllers. Les commits de typage `ResponseEntity<X>` (les 4 plus récents sur ce périmètre) sont des changements de signature purs, sans logique ajoutée.
