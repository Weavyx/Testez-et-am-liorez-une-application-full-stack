# Audit — Assertions sur le corps de réponse dans les tests d'intégration (*IT.java)

Diagnostic READ-ONLY. Aucun fichier de test n'a été modifié. Périmètre :
`back/src/test/java/com/openclassrooms/starterjwt/**/*IT.java`.

Vérification de code : pour les endpoints DELETE / participate / noLongerParticipate
de `SessionController` et `UserController`, le code source confirme un corps de
réponse réellement vide (`ResponseEntity.ok().build()`, sans `.body(...)`) —
voir `SessionController.java` lignes 65-83 et `UserController.java` lignes 32-36.
Pour ces cas, "aucun corps attendu" n'est donc pas une supposition mais un fait
vérifié dans le code de production.

## TeacherControllerIT (6 tests)

| Fichier | Nom du test | Vérifie le corps ? | Détail | Trou identifié ? |
|---|---|---|---|---|
| TeacherControllerIT | findById_returns200AndTeacher_whenTeacherExists | Oui | `$.id`, `$.firstName`("Margot"), `$.lastName`("Delahaye"), `$.createdAt` (non vide), `$.updatedAt` (non vide) | Non — DTO complet vérifié |
| TeacherControllerIT | findById_returns404_whenTeacherDoesNotExist | Non | Seul `status().isNotFound()` | Non — erreur 404, pas de donnée de sortie pertinente |
| TeacherControllerIT | findById_returns400_whenIdIsNotNumeric | Non | Seul `status().isBadRequest()` | Non — erreur de validation de format, pas de body attendu |
| TeacherControllerIT | findById_returns401_whenNotAuthenticated | Non | Seul `status().isUnauthorized()` | Non — réponse d'authentification, générée par l'entryPoint Spring Security |
| TeacherControllerIT | findAll_returns200AndAllTeachers_whenAuthenticated | Oui (partiel) | `jsonPath("$", hasSize(2))` + `$[*].id` via `containsInAnyOrder` | **Oui** — seul le champ `id` de chaque élément est vérifié ; `firstName`, `lastName`, `createdAt`, `updatedAt` de la liste ne sont jamais contrôlés |
| TeacherControllerIT | findAll_returns401_whenNotAuthenticated | Non | Seul `status().isUnauthorized()` | Non — légitime |

## AuthControllerIT (9 tests)

| Fichier | Nom du test | Vérifie le corps ? | Détail | Trou identifié ? |
|---|---|---|---|---|
| AuthControllerIT | register_returns200AndSuccessMessage_whenDataIsValid | Oui | `$.message` = "User registered successfully!" (+ assertions DB hors HTTP : password hashé, admin=false) | Non — MessageResponse ne porte qu'un champ, entièrement vérifié |
| AuthControllerIT | register_returns400AndMessage_whenEmailIsAlreadyTaken | Oui | `$.message` = "Error: Email is already taken!" | Non — champ unique vérifié |
| AuthControllerIT | register_returns400_whenEmailIsBlank | Non | Seul `status().isBadRequest()` | Non — erreur de validation Bean Validation, pas de DTO de sortie à sécuriser |
| AuthControllerIT | register_returns400_whenEmailFormatIsInvalid | Non | Seul `status().isBadRequest()` | Non — idem |
| AuthControllerIT | register_returns400_whenPasswordIsTooShort | Non | Seul `status().isBadRequest()` | Non — idem |
| AuthControllerIT | login_returns200AndJwtResponse_whenCredentialsAreValid | Oui | `$.id`, `$.username`, `$.firstName`, `$.lastName`, `$.admin`, `$.type`("Bearer"), `$.token` (non vide) + assertions hors HTTP sur la validité cryptographique du JWT | Non — JwtResponse DTO vérifié champ par champ, cas le plus complet du fichier |
| AuthControllerIT | login_returns401_whenPasswordIsWrong | Non | Seul `status().isUnauthorized()` | Non — réponse d'échec d'authentification générique (entryPoint) |
| AuthControllerIT | login_returns401_whenEmailIsUnknown | Non | Seul `status().isUnauthorized()` | Non — idem |
| AuthControllerIT | login_returns400_whenPasswordIsBlank | Non | Seul `status().isBadRequest()` | Non — erreur de validation |

## UserControllerIT (11 tests)

| Fichier | Nom du test | Vérifie le corps ? | Détail | Trou identifié ? |
|---|---|---|---|---|
| UserControllerIT | findById_returns200AndUserDto_whenUserReadsOwnAccount | Oui | `$.id`, `$.email`, `$.firstName`, `$.lastName`, `$.admin`(false), `$.createdAt`, `$.updatedAt`, `$.password` (`doesNotExist`) | Non — DTO complet vérifié, y compris l'absence du mot de passe |
| UserControllerIT | findById_returns403_whenUserReadsAnotherUsersAccount | Oui (minimal) | `$.email` (`doesNotExist`) | Non — vérification de non-fuite sur une réponse d'erreur, suffisant pour l'objectif du test |
| UserControllerIT | findById_returns404_whenUserDoesNotExist | Non | Seul `status().isNotFound()` | Non — légitime |
| UserControllerIT | findById_returns400_whenIdIsNotNumeric | Non | Seul `status().isBadRequest()` | Non — légitime |
| UserControllerIT | findById_returns401_whenNotAuthenticated | Non | Seul `status().isUnauthorized()` | Non — légitime |
| UserControllerIT | delete_returns200_whenUserDeletesOwnAccount | Non (pas de jsonPath) | `status().isOk()` + `assertThat(userRepository.findById(...)).isEmpty()` (vérif DB, pas HTTP) | Non — `DELETE` renvoie `ResponseEntity.ok().build()`, aucun corps à vérifier (confirmé dans le code source) |
| UserControllerIT | delete_returns200AndClearsParticipations_whenUserDeletesOwnAccountWhileEnrolledInASession | Non (pas de jsonPath) | `status().isOk()` + assertions DB (participations nettoyées) | Non — idem, aucun corps attendu |
| UserControllerIT | delete_returns403_whenUserTriesToDeleteAnotherUsersAccount | Non (pas de jsonPath) | `status().isForbidden()` + `assertThat(userRepository.findById(userB.getId())).isPresent()` (DB) | Non — réponse d'erreur, pas de DTO de sortie |
| UserControllerIT | delete_returns404_whenUserDoesNotExist | Non | Seul `status().isNotFound()` | Non — légitime |
| UserControllerIT | delete_returns400_whenIdIsNotNumeric | Non | Seul `status().isBadRequest()` | Non — légitime |
| UserControllerIT | delete_returns401_whenNotAuthenticated | Non | Seul `status().isUnauthorized()` | Non — légitime |

## SessionControllerIT (35 tests)

| Fichier | Nom du test | Vérifie le corps ? | Détail | Trou identifié ? |
|---|---|---|---|---|
| SessionControllerIT | findById_returns200AndSession_whenSessionExists | Oui (partiel) | `$.id`, `$.name`("Hatha Yoga"), `$.description`("Séance découverte"), `$.teacher_id`, `$.createdAt`, `$.updatedAt` | **Oui** — `$.date` et `$.users` (liste des participants) du SessionDto ne sont jamais vérifiés |
| SessionControllerIT | findById_returns404_whenSessionDoesNotExist | Non | Seul `status().isNotFound()` | Non — légitime |
| SessionControllerIT | findById_returns400_whenIdIsNotNumeric | Non | Seul `status().isBadRequest()` | Non — légitime |
| SessionControllerIT | findById_returns401_whenNotAuthenticated | Non | Seul `status().isUnauthorized()` | Non — légitime |
| SessionControllerIT | findAll_returns200AndAllSessions_whenAuthenticated | Oui (très partiel) | Seul `jsonPath("$", hasSize(2))` | **Oui** — aucun champ d'aucun élément de la liste n'est vérifié (ni id, ni name, ni teacher_id) |
| SessionControllerIT | findAll_returns401_whenNotAuthenticated | Non | Seul `status().isUnauthorized()` | Non — légitime |
| SessionControllerIT | create_returns200_whenCalledByAdmin | Oui (partiel) | `$.name`("Hatha Yoga"), `$.teacher_id` | **Oui — prioritaire** — `$.id` (généré), `$.description`, `$.date`, `$.users`, `$.createdAt`, `$.updatedAt` ne sont pas vérifiés alors que POST crée la ressource et retourne le mapping complet |
| SessionControllerIT | create_returns403_whenCalledByNonAdmin | Non | Seul `status().isForbidden()` | Non — légitime |
| SessionControllerIT | create_returns400_whenNameIsMissing | Non | Seul `status().isBadRequest()` | Non — légitime |
| SessionControllerIT | create_returns401_whenNotAuthenticated | Non | Seul `status().isUnauthorized()` | Non — légitime |
| SessionControllerIT | update_returns200_whenCalledByAdmin | Oui (très partiel) | Seul `$.id` | **Oui — prioritaire** — `$.name`, `$.description`, `$.teacher_id`, `$.date`, `$.users`, `$.updatedAt` (qui devrait changer après update) ne sont jamais vérifiés |
| SessionControllerIT | update_returns403_whenCalledByNonAdmin | Non | Seul `status().isForbidden()` | Non — légitime |
| SessionControllerIT | update_returns400_whenIdIsNotNumeric | Non | Seul `status().isBadRequest()` | Non — légitime |
| SessionControllerIT | update_returns403NotBadRequest_whenCalledByNonAdminWithInvalidId | Non | Seul `status().isForbidden()` | Non — test de contrat sur l'ordre d'évaluation sécurité/validation, le code HTTP est la seule chose testée par construction |
| SessionControllerIT | update_returns404_whenSessionDoesNotExist | Non | Seul `status().isNotFound()` | Non — légitime |
| SessionControllerIT | update_returns400_whenValidationFails | Non | Seul `status().isBadRequest()` | Non — légitime |
| SessionControllerIT | update_returns401_whenNotAuthenticated | Non | Seul `status().isUnauthorized()` | Non — légitime |
| SessionControllerIT | delete_returns200_whenCalledByAdmin | Non | Seul `status().isOk()` | Non — `DELETE` renvoie `ResponseEntity.ok().build()`, aucun corps (confirmé code source) |
| SessionControllerIT | delete_returns403_whenCalledByNonAdmin | Non | Seul `status().isForbidden()` | Non — légitime |
| SessionControllerIT | delete_returns400_whenIdIsNotNumeric | Non | Seul `status().isBadRequest()` | Non — légitime |
| SessionControllerIT | delete_returns404_whenSessionDoesNotExist | Non | Seul `status().isNotFound()` | Non — légitime |
| SessionControllerIT | delete_returns401_whenNotAuthenticated | Non | Seul `status().isUnauthorized()` | Non — légitime |
| SessionControllerIT | delete_returns200AndClearsParticipations_whenSessionHasParticipants | Non (pas de jsonPath) | `status().isOk()` + requête SQL native pour vérifier la table `participate` (DB, pas HTTP) | Non — aucun corps attendu, effet vérifié en base |
| SessionControllerIT | participate_returns200_whenNonAdminParticipatesForThemselves | Non | Seul `status().isOk()` | Non — `participate` renvoie `ResponseEntity.ok().build()`, aucun corps (confirmé code source) |
| SessionControllerIT | participate_returns403_whenUserIdDoesNotMatchAuthenticatedPrincipal | Non | Seul `status().isForbidden()` | Non — légitime |
| SessionControllerIT | participate_returns400_whenIdIsNotNumeric | Non | Seul `status().isBadRequest()` | Non — légitime |
| SessionControllerIT | participate_returns404_whenSessionDoesNotExist | Non | Seul `status().isNotFound()` | Non — légitime |
| SessionControllerIT | participate_returns404_whenUserDoesNotExist | Non | Seul `status().isNotFound()` | Non — légitime |
| SessionControllerIT | participate_returns400_whenAlreadyParticipating | Non | Seul `status().isBadRequest()` | Non — légitime |
| SessionControllerIT | noLongerParticipate_returns200_whenNonAdminParticipatesForThemselves | Non | Seul `status().isOk()` | Non — `noLongerParticipate` renvoie `ResponseEntity.ok().build()`, aucun corps (confirmé code source) |
| SessionControllerIT | noLongerParticipate_returns403_whenUserIdDoesNotMatchAuthenticatedPrincipal | Non | Seul `status().isForbidden()` | Non — légitime |
| SessionControllerIT | noLongerParticipate_returns400_whenIdIsNotNumeric | Non | Seul `status().isBadRequest()` | Non — légitime |
| SessionControllerIT | noLongerParticipate_returns404_whenSessionDoesNotExist | Non | Seul `status().isNotFound()` | Non — légitime |
| SessionControllerIT | noLongerParticipate_returns404_whenUserDoesNotExist | Non | Seul `status().isNotFound()` | Non — légitime |
| SessionControllerIT | noLongerParticipate_returns400_whenNotParticipating | Non | Seul `status().isBadRequest()` | Non — légitime |

## ApplicationContextIT (1 test)

| Fichier | Nom du test | Vérifie le corps ? | Détail | Trou identifié ? |
|---|---|---|---|---|
| ApplicationContextIT | contextLoads | N/A | Pas d'appel MockMvc/HTTP — vérifie uniquement le démarrage du contexte Spring | Non — hors périmètre (pas de réponse HTTP en jeu) |

---

## Synthèse

- **Total de tests `*IT.java` analysés : 62**
  (TeacherControllerIT : 6 · AuthControllerIT : 9 · UserControllerIT : 11 · SessionControllerIT : 35 · ApplicationContextIT : 1)
- **Tests avec un trou réel identifié (données de sortie existantes mais non, ou incomplètement, vérifiées) : 5**
- **Tests où le code HTTP seul est légitimement suffisant (delete/erreur sans corps significatif) : 56**
- **Test hors périmètre (pas de requête HTTP) : 1** (`ApplicationContextIT#contextLoads`)

### Détail des 5 trous identifiés

| # | Test | Nature du trou |
|---|---|---|
| 1 | `SessionControllerIT#create_returns200_whenCalledByAdmin` | POST crée une session ; seuls `name` et `teacher_id` vérifiés sur le DTO retourné. `id`, `description`, `date`, `users`, `createdAt`, `updatedAt` non vérifiés. |
| 2 | `SessionControllerIT#update_returns200_whenCalledByAdmin` | PUT modifie une session ; seul `id` vérifié. `name`, `description`, `teacher_id`, `date`, `users`, `updatedAt` non vérifiés. |
| 3 | `SessionControllerIT#findAll_returns200AndAllSessions_whenAuthenticated` | GET liste ; seule la taille de la liste (`hasSize(2)`) est vérifiée, aucun champ d'aucun élément. |
| 4 | `SessionControllerIT#findById_returns200AndSession_whenSessionExists` | GET par id ; la plupart des champs sont vérifiés mais `date` et `users` sont omis. |
| 5 | `TeacherControllerIT#findAll_returns200AndAllTeachers_whenAuthenticated` | GET liste ; seul `id` de chaque élément est vérifié via `containsInAnyOrder`, pas `firstName`/`lastName`/dates. |

`UserControllerIT` ne présente aucun trou : son unique test de lecture nominale (`findById_returns200AndUserDto_whenUserReadsOwnAccount`) vérifie déjà tous les champs du DTO, y compris l'absence du mot de passe. `AuthControllerIT` non plus : ses tests 200 (`register`, `login`) vérifient déjà l'intégralité de leurs DTO de sortie respectifs.

### Liste priorisée des tests à corriger en premier

Priorité la plus haute : les endpoints qui **créent ou modifient** une ressource, car ce sont eux qui sécurisent le mapping DTO d'entrée → DTO de sortie via MapStruct (objectif du retour de mentorat) :

1. **`SessionControllerIT#create_returns200_whenCalledByAdmin`** — POST /api/session : vérifier `$.id` (non nul), `$.description`, `$.date`, `$.users`, `$.createdAt`, `$.updatedAt` en plus de `name`/`teacher_id` déjà couverts.
2. **`SessionControllerIT#update_returns200_whenCalledByAdmin`** — PUT /api/session/{id} : vérifier `$.name`, `$.description`, `$.teacher_id`, `$.date`, `$.users` en plus de `id`, et idéalement que `updatedAt` a changé par rapport à la création.
3. **`SessionControllerIT#findAll_returns200AndAllSessions_whenAuthenticated`** — vérifier au moins les `id`/`name`/`teacher_id` des éléments de la liste, pas seulement sa taille.
4. **`SessionControllerIT#findById_returns200AndSession_whenSessionExists`** — compléter avec `$.date` et `$.users`.
5. **`TeacherControllerIT#findAll_returns200AndAllTeachers_whenAuthenticated`** — compléter avec `firstName`/`lastName` (ou au minimum un test dédié équivalent à celui de `findById`).

Les tests de lecture seule (`findById`, `findAll`) sont secondaires par rapport aux `create`/`update` : une lecture erronée révèle un bug de mapping déjà détectable ailleurs, alors qu'une création/modification est le point d'entrée où le DTO de sortie est construit pour la première fois à partir de l'entité persistée.
