# Audit — Découpage en couches Controller → Service → Repository

Date : 2026-08-07
Branche : `chore/verification-finale-livrable-p4`
Périmètre : `back/src/main/java/com/openclassrooms/starterjwt/controllers/`
Type d'audit : lecture seule — aucun fichier modifié, rien commité.

## Méthode

1. `grep -rln "Repository" back/src/main/java/com/openclassrooms/starterjwt/controllers` pour
   repérer toute mention du mot "Repository" (import, usage, commentaire) dans les controllers.
2. Lecture intégrale des 4 fichiers controllers (`AuthController`, `SessionController`,
   `TeacherController`, `UserController`) pour distinguer un éventuel import/usage direct d'une
   interface `*Repository` d'une simple mention en commentaire/Javadoc.
3. Vérification positive : confirmation que chaque controller injecte bien un ou plusieurs
   **Service**, jamais un Repository, dans son constructeur.

## Résultat

**0 violation.** Le grep sur le mot "Repository" dans tout le package `controllers/` ne retourne
**aucun résultat**, ni en import, ni en usage, ni en commentaire — les 4 controllers du projet ne
mentionnent le mot "Repository" nulle part.

Le découpage en couches établi lors d'Ex1 est donc toujours respecté à ce jour, malgré les
modifications apportées aux controllers/services lors des Phases 1 à 5 de l'audit post-Ex2
(introduction de `ForbiddenException`, `assertRequestingUserIsSelf`, etc.) : aucune de ces
modifications n'a réintroduit d'accès direct à un repository depuis un controller.

## Vérification positive — injection de Service dans chaque controller

| Controller | Champs injectés (constructeur) | Repository utilisé ? |
|---|---|---|
| `AuthController` | `AuthenticationManager`, `JwtUtils`, `UserService` | Non |
| `SessionController` | `SessionService`, `SessionMapper` | Non |
| `TeacherController` | `TeacherService`, `TeacherMapper` | Non |
| `UserController` | `UserService`, `UserMapper` | Non |

Détail :

- **`AuthController`** (`controllers/AuthController.java:25-35`) — injecte `AuthenticationManager`,
  `JwtUtils` et `UserService`. Les opérations `login`/`register` passent par
  `userService.isAdmin(...)` et `userService.register(...)`.
- **`SessionController`** (`controllers/SessionController.java:26-34`) — injecte `SessionService`
  et `SessionMapper`. Toutes les opérations CRUD (`findById`, `findAll`, `create`, `update`,
  `save`/delete, `participate`, `noLongerParticipate`) passent par `sessionService`.
- **`TeacherController`** (`controllers/TeacherController.java:18-26`) — injecte `TeacherService`
  et `TeacherMapper`. `findById`/`findAll` passent par `teacherService`.
- **`UserController`** (`controllers/UserController.java:17-25`) — injecte `UserService` et
  `UserMapper`. `findById` passe par `userService.findOwnProfile(...)`, la suppression par
  `userService.deleteById(...)`.

Aucun champ de type `*Repository` (`SessionRepository`, `UserRepository`, `TeacherRepository`)
n'apparaît dans aucun des 4 controllers.

## Conclusion

Conformité totale confirmée : les 4 controllers (`Auth`/`Session`/`Teacher`/`User`) respectent
strictement le découpage Controller → Service → Repository. Tout accès aux données transite
exclusivement par la couche service. Aucune correction nécessaire — audit clos sans action.
