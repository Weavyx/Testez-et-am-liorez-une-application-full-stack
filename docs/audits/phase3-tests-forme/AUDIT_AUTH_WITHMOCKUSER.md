# Audit — tests d'intégration en authentification manuelle vs candidats `@WithMockUser`

Audit **en lecture seule**. Aucun fichier de test ni de production n'a été modifié.
Aucune migration n'est décidée ici : ce document sert de base à l'arbitrage.

## Constat préalable important (déviation par rapport à l'hypothèse initiale)

Le grep sur `/api/auth/login` ne remonte qu'**un seul fichier** :
`AuthControllerIT.java`. Aucun autre test d'intégration n'appelle réellement
`/api/auth/login` puis ne réutilise le token obtenu pour attaquer un autre
endpoint — le pattern "login réel → extraction du token → réutilisation en
header sur les requêtes suivantes" décrit dans la consigne n'existe donc pas
tel quel dans ce repo.

Le mécanisme réellement utilisé par `TeacherControllerIT`, `UserControllerIT`
et `SessionControllerIT` est différent : une méthode utilitaire partagée dans
`AbstractIntegrationTest` (`generateStandardUserToken()`,
`generateAdminUserToken()`, `generateTokenForUser(User)`) **fabrique
directement un JWT réel via `JwtUtils.generateJwtToken(...)`**, à partir d'un
`UserDetailsImpl` construit en mémoire, sans passer par une requête HTTP
`/api/auth/login`. Le token produit est cryptographiquement valide (signé,
avec expiration réelle) et est ensuite envoyé en header
`Authorization: Bearer <token>` sur la requête testée, ce qui fait donc bien
passer cette requête par le vrai `AuthTokenFilter` (parsing + validation +
chargement de l'utilisateur via `UserDetailsServiceImpl`).

C'est ce pattern (token réel généré hors HTTP, réutilisé en header à chaque
requête) qui constitue, dans ce repo, l'équivalent fonctionnel de
"l'authentification manuelle" visée par la demande d'audit — il a donc été
traité comme tel ci-dessous. `AuthControllerIT` est traité séparément
puisqu'il ne fabrique/réutilise aucun token pour appeler un endpoint tiers :
il *est* le test du parcours d'authentification lui-même.

**Méthode utilitaire partagée** (référencée une seule fois ici, non répétée
ligne par ligne) : `AbstractIntegrationTest.generateStandardUserToken()`,
`.generateAdminUserToken()`, `.generateTokenForUser(User)`,
`.persistStandardUser()`, `.persistAdminUser()`.

## Résumé chiffré

- Fichiers concernés : 4 (`AuthControllerIT`, `TeacherControllerIT`,
  `UserControllerIT`, `SessionControllerIT`).
- Total de tests utilisant un token réel (helper partagé) en header
  `Authorization`, ou appartenant au flux d'authentification lui-même : **51**
  - `AuthControllerIT` : 9 tests (login + register), tous catégorie (b) par
    nature (ils sont le test du parcours d'auth, pas des consommateurs de
    token pour tester une autre ressource).
  - `TeacherControllerIT` : 6 tests.
  - `UserControllerIT` : 11 tests.
  - `SessionControllerIT` : 34 tests utilisant un token (les tests
    `*_returns401_whenNotAuthenticated` n'envoient pas de token, mais sont
    inclus car ils testent directement le comportement du filtre en son
    absence — pertinents pour la catégorie (b)).
- Répartition :
  - **(a) candidats `@WithMockUser`** : 34 tests (logique métier / autorisation
    applicative / RBAC Spring Security déclaratif).
  - **(b) doivent rester en JWT réel** : 17 tests (9 dans `AuthControllerIT` +
    8 tests `*_returns401_whenNotAuthenticated` répartis dans les 3 autres
    contrôleurs).

## Flux critiques couverts par les tests catégorie (b)

| Flux critique | Couvert par | Statut |
|---|---|---|
| Login avec identifiants valides (émission d'un JWT réel, signature HS512, subject correct) | `AuthControllerIT.login_returns200AndJwtResponse_whenCredentialsAreValid` | ✅ couvert |
| Login avec mot de passe invalide → 401 | `AuthControllerIT.login_returns401_whenPasswordIsWrong` | ✅ couvert |
| Login avec email inconnu → 401 | `AuthControllerIT.login_returns401_whenEmailIsUnknown` | ✅ couvert |
| Login avec champ requis manquant/vide → 400 (validation de payload, pas le filtre JWT) | `AuthControllerIT.login_returns400_whenPasswordIsBlank` | ✅ couvert |
| Inscription (register) réussie / erreurs de validation | `AuthControllerIT.register_*` (5 tests) | ✅ couvert (flux adjacent à l'auth, pas le JWT lui-même) |
| Accès à une ressource protégée **sans token** → 401 via `AuthTokenFilter`/`authenticationEntryPoint` | `TeacherControllerIT.findById_returns401_whenNotAuthenticated`, `.findAll_returns401_whenNotAuthenticated`, `UserControllerIT.findById_returns401_whenNotAuthenticated`, `.delete_returns401_whenNotAuthenticated`, `SessionControllerIT.findById_returns401_whenNotAuthenticated`, `.findAll_returns401_whenNotAuthenticated`, `.create_returns401_whenNotAuthenticated`, `.update_returns401_whenNotAuthenticated`, `.delete_returns401_whenNotAuthenticated` | ✅ couvert (redondant : 9 occurrences du même comportement de filtre à travers les contrôleurs — voir note ci-dessous) |
| Accès avec un **token invalide/malformé/expiré** → rejet par `AuthTokenFilter` | — | ❌ **non couvert au niveau intégration** (seulement en test unitaire : `JwtUtilsTest`, `AuthTokenFilterTest`, hors périmètre `*IT.java`) |

**Note** : le flux "sans token → 401" est actuellement dupliqué 9 fois à
l'identique (un par contrôleur/endpoint) alors qu'il teste toujours le même
mécanisme (`AuthTokenFilter` + `authenticationEntryPoint`). Point à signaler
pour l'arbitrage : conserver au moins un test réel de ce flux est nécessaire,
mais rien n'impose d'en garder 9 — c'est cependant une décision de
suppression/consolidation, hors périmètre de cet audit read-only.

**Trou identifié** : aucun test d'intégration (`*IT.java`) n'exerce un token
expiré ou signé avec une mauvaise clé contre un vrai endpoint protégé via
`MockMvc` bout-en-bout ; ce cas n'est vérifié qu'en unitaire
(`JwtUtilsTest`, `AuthTokenFilterTest`). À garder en tête si des tests
`*_returns401_whenNotAuthenticated` sont supprimés/consolidés : ils ne
couvrent que le cas "absence de token", pas "token présent mais invalide".

## Tableau détaillé

### `AuthControllerIT.java`

| Fichier | Nom du test | Endpoint(s) testé(s) | Ce que l'assertion finale vérifie réellement | Catégorie | Justification |
|---|---|---|---|---|---|
| AuthControllerIT | register_returns200AndSuccessMessage_whenDataIsValid | POST /api/auth/register | Persistance réelle de l'utilisateur, mot de passe bien encodé (BCrypt), admin=false par défaut | b | Fait partie du parcours d'inscription/auth ; endpoint public sans notion de token à mocker, hors périmètre `@WithMockUser` |
| AuthControllerIT | register_returns400AndMessage_whenEmailIsAlreadyTaken | POST /api/auth/register | Message d'erreur métier "Email is already taken!" | b | Idem — logique du flux d'auth, pas un endpoint protégé par JWT |
| AuthControllerIT | register_returns400_whenEmailIsBlank | POST /api/auth/register | Message de validation Bean Validation sur `email` | b | Idem |
| AuthControllerIT | register_returns400_whenEmailFormatIsInvalid | POST /api/auth/register | Message de validation format email | b | Idem |
| AuthControllerIT | register_returns400_whenPasswordIsTooShort | POST /api/auth/register | Message de validation taille mot de passe | b | Idem |
| AuthControllerIT | register_returns400AndListsAllFieldErrors_whenMultipleFieldsAreInvalid | POST /api/auth/register | Agrégation de toutes les erreurs de champ dans un seul message | b | Idem |
| AuthControllerIT | login_returns200AndJwtResponse_whenCredentialsAreValid | POST /api/auth/login | Émission d'un JWT réel : structure JWS, signature HS512 valide via `JwtUtils.validateJwtToken`, subject == email authentifié | b | C'est le test du parcours JWT réel lui-même (génération + validation cryptographique) — @WithMockUser ne peut pas s'y substituer |
| AuthControllerIT | login_returns401_whenPasswordIsWrong | POST /api/auth/login | 401 produit par `AuthenticationManager#authenticate` (BadCredentialsException) traduit par `authenticationEntryPoint` | b | Comportement de la chaîne d'authentification Spring Security, pas de logique métier d'un endpoint protégé |
| AuthControllerIT | login_returns401_whenEmailIsUnknown | POST /api/auth/login | 401 pour email inconnu (UsernameNotFoundException masquée en BadCredentialsException) | b | Idem |
| AuthControllerIT | login_returns400_whenPasswordIsBlank | POST /api/auth/login | 400 de Bean Validation (`@NotBlank`), avant même l'authentification | b | Bien que ce soit un 400 de validation et non un rejet JWT, le test fait partie du contrat de l'endpoint `/login` lui-même |

### `TeacherControllerIT.java`

| Fichier | Nom du test | Endpoint(s) testé(s) | Ce que l'assertion finale vérifie réellement | Catégorie | Justification |
|---|---|---|---|---|---|
| TeacherControllerIT | findById_returns200AndTeacher_whenTeacherExists | GET /api/teacher/{id} | Sérialisation correcte du DTO `Teacher` (id, prénom, nom, dates) | a | Logique métier de lecture, l'identité de l'appelant n'est pas testée au-delà de "authentifié" |
| TeacherControllerIT | findById_returns404_whenTeacherDoesNotExist | GET /api/teacher/{id} | 404 renvoyé par `GlobalExceptionHandler` pour ressource absente | a | Logique métier (gestion d'erreur), pas le filtre JWT |
| TeacherControllerIT | findById_returns400_whenIdIsNotNumeric | GET /api/teacher/{id} | 400 sur `NumberFormatException` (id non numérique) | a | Logique métier de validation d'entrée |
| TeacherControllerIT | findById_returns401_whenNotAuthenticated | GET /api/teacher/{id} | 401 en l'absence totale de header `Authorization` | b | Comportement du filtre `AuthTokenFilter`/`authenticationEntryPoint` en l'absence de token — ne peut pas être simulé avec `@WithMockUser` (qui injecte justement un principal) |
| TeacherControllerIT | findAll_returns200AndAllTeachers_whenAuthenticated | GET /api/teacher | Liste complète des enseignants persistés, contenu exact | a | Logique métier de lecture |
| TeacherControllerIT | findAll_returns401_whenNotAuthenticated | GET /api/teacher | 401 sans token | b | Idem findById_returns401 |

### `UserControllerIT.java`

| Fichier | Nom du test | Endpoint(s) testé(s) | Ce que l'assertion finale vérifie réellement | Catégorie | Justification |
|---|---|---|---|---|---|
| UserControllerIT | findById_returns200AndUserDto_whenUserReadsOwnAccount | GET /api/user/{id} | DTO utilisateur complet renvoyé pour son propre compte, mot de passe absent de la réponse | a | Logique métier + contrôle de propriété applicatif (comparaison d'identité en couche service), simulable via un principal `@WithMockUser` porteur du bon id |
| UserControllerIT | findById_returns403_whenUserReadsAnotherUsersAccount | GET /api/user/{id} | 403 applicatif (comparaison d'identité dans `UserService`), pas de fuite de données de la cible | a | Autorisation métier (service), pas une règle Spring Security déclarative ni le filtre JWT |
| UserControllerIT | findById_returns404_whenUserDoesNotExist | GET /api/user/{id} | 404 pour utilisateur absent | a | Logique métier |
| UserControllerIT | findById_returns400_whenIdIsNotNumeric | GET /api/user/{id} | 400 sur id non numérique | a | Logique métier de validation |
| UserControllerIT | findById_returns401_whenNotAuthenticated | GET /api/user/{id} | 401 sans token | b | Comportement du filtre JWT en l'absence de token |
| UserControllerIT | delete_returns200_whenUserDeletesOwnAccount | DELETE /api/user/{id} | Suppression effective du compte propre en base | a | Logique métier |
| UserControllerIT | delete_returns200AndClearsParticipations_whenUserDeletesOwnAccountWhileEnrolledInASession | DELETE /api/user/{id} | Suppression du compte + nettoyage de la table de jointure `participate` (régression FK) | a | Logique métier / cascade applicative, sans lien avec le mécanisme JWT |
| UserControllerIT | delete_returns403_whenUserTriesToDeleteAnotherUsersAccount | DELETE /api/user/{id} | 403 applicatif, la cible n'est pas supprimée | a | Autorisation métier (service) |
| UserControllerIT | delete_returns404_whenUserDoesNotExist | DELETE /api/user/{id} | 404 pour utilisateur absent | a | Logique métier |
| UserControllerIT | delete_returns400_whenIdIsNotNumeric | DELETE /api/user/{id} | 400 sur id non numérique | a | Logique métier |
| UserControllerIT | delete_returns401_whenNotAuthenticated | DELETE /api/user/{id} | 401 sans token | b | Comportement du filtre JWT |

### `SessionControllerIT.java`

| Fichier | Nom du test | Endpoint(s) testé(s) | Ce que l'assertion finale vérifie réellement | Catégorie | Justification |
|---|---|---|---|---|---|
| SessionControllerIT | findById_returns200AndSession_whenSessionExists | GET /api/session/{id} | Sérialisation correcte de la session | a | Logique métier |
| SessionControllerIT | findById_returns404_whenSessionDoesNotExist | GET /api/session/{id} | 404 pour session absente | a | Logique métier |
| SessionControllerIT | findById_returns400_whenIdIsNotNumeric | GET /api/session/{id} | 400 sur id non numérique | a | Logique métier |
| SessionControllerIT | findById_returns401_whenNotAuthenticated | GET /api/session/{id} | 401 sans token | b | Comportement du filtre JWT |
| SessionControllerIT | findAll_returns200AndAllSessions_whenAuthenticated | GET /api/session | Liste exacte des sessions persistées | a | Logique métier |
| SessionControllerIT | findAll_returns401_whenNotAuthenticated | GET /api/session | 401 sans token | b | Comportement du filtre JWT |
| SessionControllerIT | create_returns200_whenCalledByAdmin | POST /api/session | Création réussie par un admin, DTO retourné conforme | a | RBAC déclaratif Spring Security (`hasRole("ADMIN")`), simulable via `@WithMockUser(roles="ADMIN")` |
| SessionControllerIT | create_returns403_whenCalledByNonAdmin | POST /api/session | 403 pour un utilisateur authentifié non-admin | a | RBAC déclaratif, simulable via `@WithMockUser` sans rôle ADMIN |
| SessionControllerIT | create_returns400_whenNameIsMissing | POST /api/session | 400 de validation de payload (nom manquant) | a | Logique métier de validation |
| SessionControllerIT | create_returns401_whenNotAuthenticated | POST /api/session | 401 sans token | b | Comportement du filtre JWT |
| SessionControllerIT | update_returns200_whenCalledByAdmin | PUT /api/session/{id} | Mise à jour réussie par un admin | a | RBAC déclaratif |
| SessionControllerIT | update_returns403_whenCalledByNonAdmin | PUT /api/session/{id} | 403 pour non-admin | a | RBAC déclaratif |
| SessionControllerIT | update_returns400_whenIdIsNotNumeric | PUT /api/session/{id} | 400 sur id non numérique (avec token admin) | a | Logique métier de validation |
| SessionControllerIT | update_returns403NotBadRequest_whenCalledByNonAdminWithInvalidId | PUT /api/session/{id} | Ordre d'évaluation Spring Security (`hasRole`) avant le parsing d'id : 403 et non 400 | a | Test de contrat sur la config de sécurité déclarative (matchers), reproductible avec `@WithMockUser` sans rôle ADMIN — ne dépend pas du mécanisme JWT en tant que tel |
| SessionControllerIT | update_returns404_whenSessionDoesNotExist | PUT /api/session/{id} | 404 pour session absente (avec token admin) | a | Logique métier |
| SessionControllerIT | update_returns400_whenValidationFails | PUT /api/session/{id} | 400 de validation de payload | a | Logique métier |
| SessionControllerIT | update_returns401_whenNotAuthenticated | PUT /api/session/{id} | 401 sans token | b | Comportement du filtre JWT |
| SessionControllerIT | delete_returns200_whenCalledByAdmin | DELETE /api/session/{id} | Suppression effective par un admin | a | RBAC déclaratif + logique métier |
| SessionControllerIT | delete_returns403_whenCalledByNonAdmin | DELETE /api/session/{id} | 403 pour non-admin | a | RBAC déclaratif |
| SessionControllerIT | delete_returns400_whenIdIsNotNumeric | DELETE /api/session/{id} | 400 sur id non numérique (token admin) | a | Logique métier |
| SessionControllerIT | delete_returns404_whenSessionDoesNotExist | DELETE /api/session/{id} | 404 pour session absente | a | Logique métier |
| SessionControllerIT | delete_returns401_whenNotAuthenticated | DELETE /api/session/{id} | 401 sans token | b | Comportement du filtre JWT |
| SessionControllerIT | delete_returns200AndClearsParticipations_whenSessionHasParticipants | DELETE /api/session/{id} | Suppression + nettoyage automatique de la table `participate` (cascade Hibernate) | a | Logique métier/persistance, sans lien avec le mécanisme JWT |
| SessionControllerIT | participate_returns200_whenNonAdminParticipatesForThemselves | POST /api/session/{id}/participate/{userId} | Inscription réussie quand le token porte l'identité de `{userId}` | a | Autorisation métier "self" (comparaison id path vs principal), simulable avec un principal `@WithMockUser` custom portant le bon id |
| SessionControllerIT | participate_returns403_whenUserIdDoesNotMatchAuthenticatedPrincipal | POST /api/session/{id}/participate/{userId} | 403 quand `{userId}` ≠ principal authentifié | a | Autorisation métier "self" |
| SessionControllerIT | participate_returns400_whenIdIsNotNumeric | POST /api/session/{id}/participate/{userId} | 400 sur id de session non numérique | a | Logique métier |
| SessionControllerIT | participate_returns404_whenSessionDoesNotExist | POST /api/session/{id}/participate/{userId} | 404 session absente | a | Logique métier |
| SessionControllerIT | participate_returns404_whenUserDoesNotExist | POST /api/session/{id}/participate/{userId} | 404 utilisateur absent | a | Logique métier |
| SessionControllerIT | participate_returns400_whenAlreadyParticipating | POST /api/session/{id}/participate/{userId} | 400 déjà inscrit | a | Logique métier |
| SessionControllerIT | noLongerParticipate_returns200_whenNonAdminParticipatesForThemselves | DELETE /api/session/{id}/participate/{userId} | Désinscription réussie pour soi-même | a | Autorisation métier "self" |
| SessionControllerIT | noLongerParticipate_returns403_whenUserIdDoesNotMatchAuthenticatedPrincipal | DELETE /api/session/{id}/participate/{userId} | 403 si `{userId}` ≠ principal | a | Autorisation métier "self" |
| SessionControllerIT | noLongerParticipate_returns400_whenIdIsNotNumeric | DELETE /api/session/{id}/participate/{userId} | 400 id session non numérique | a | Logique métier |
| SessionControllerIT | noLongerParticipate_returns404_whenSessionDoesNotExist | DELETE /api/session/{id}/participate/{userId} | 404 session absente | a | Logique métier |
| SessionControllerIT | noLongerParticipate_returns404_whenUserDoesNotExist | DELETE /api/session/{id}/participate/{userId} | 404 utilisateur absent | a | Logique métier |
| SessionControllerIT | noLongerParticipate_returns400_whenNotParticipating | DELETE /api/session/{id}/participate/{userId} | 400 non inscrit | a | Logique métier |

## Points d'attention pour l'arbitrage (pas des recommandations de correction)

1. Les 9 tests `*_returns401_whenNotAuthenticated` sont le seul verrou
   d'intégration bout-en-bout sur "requête sans token → rejet par le filtre" ;
   au moins un doit être conservé en conditions réelles après tout arbitrage.
2. Aucun test d'intégration ne couvre "token présent mais invalide/expiré/mal
   signé" contre un vrai endpoint protégé (`MockMvc` + `AuthTokenFilter`
   réel) — actuellement seulement en unitaire. À considérer indépendamment
   de la présente simplification.
3. Pour les tests catégorie (a) qui dépendent de l'identité précise du
   principal (`findById_returns200AndUserDto_whenUserReadsOwnAccount`,
   `delete_returns200_whenUserDeletesOwnAccount`, `participate_*ForThemselves`,
   `noLongerParticipate_*ForThemselves`, et les cas croisés 403 "another
   account"/"userId does not match"), une migration vers `@WithMockUser`
   nécessiterait un mécanisme pour injecter un principal avec un id métier
   précis (ex. `@WithUserDetails` ou une `WithSecurityContextFactory`
   custom) — un simple `@WithMockUser(username = "...")` sans id explicite
   ne suffirait pas tel quel, vu que `UserDetailsImpl` porte un champ `id`
   utilisé dans les comparaisons de propriété.
4. Les tests RBAC (`create/update/delete_returns403_whenCalledByNonAdmin`,
   `update_returns403NotBadRequest_whenCalledByNonAdminWithInvalidId`)
   dépendent de l'autorité `ROLE_ADMIN` portée par le token — `@WithMockUser(roles = "ADMIN")`
   ou son absence reproduit cette autorité sans passer par un JWT réel,
   dans le respect strict de `hasRole("ADMIN")` déclaré dans
   `WebSecurityConfig`.
