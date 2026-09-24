# Audit Phase 1 — Zone autorisation (WebSecurityConfig + SessionController)

> Généré en lecture seule, conformément à METHODE_AUDIT.md. Aucune modification de fichier. Aucun jugement A/B/C/D ici — les faits et scénarios sont posés, l'arbitrage se fait en conversationnel (Phase 2). Zone couverte : autorisation transverse (`WebSecurityConfig`) + `SessionController` complet (7 endpoints, y compris `participate`/`noLongerParticipate`). Hors périmètre : `auth` (login/register), `user`.

## Cartographie des règles de sécurité (ordre du code)

Source : `back/src/main/java/com/openclassrooms/starterjwt/security/WebSecurityConfig.java`, méthode `securityFilterChain()`, lignes 60-69. Spring Security évalue les règles `authorizeHttpRequests` **dans l'ordre de déclaration**, première correspondance gagnante.

| Ordre | Pattern exact | Rôle/règle | Note |
|---|---|---|---|
| 1 | `/api/auth/**` | `permitAll()` | Hors périmètre de cette passe (zone auth). |
| 2 | `POST /api/session` | `hasRole("ADMIN")` | Chemin exact, aucun wildcard. Ne capture que `POST /api/session` strictement. |
| 3 | `PUT /api/session/*` | `hasRole("ADMIN")` | `*` (AntPathMatcher) capture **exactement un segment** sans `/`. Capture `/api/session/5`, ne capture PAS `/api/session/5/participate/10` (2 segments supplémentaires). |
| 4 | `DELETE /api/session/*` | `hasRole("ADMIN")` | Même sémantique que la règle 3. |
| 5 | `/api/**` | `authenticated()` | Catch-all pour tout ce qui n'a pas matché une règle plus spécifique au-dessus, y compris `GET /api/session`, `GET /api/session/{id}`, et **`POST`/`DELETE /api/session/{id}/participate/{userId}`**. |
| 6 | `anyRequest()` | `authenticated()` | Filet de sécurité final, hors chemins `/api/**` (non pertinent ici). |

**Note transverse confirmée par le code lui-même** : le commentaire ligne 63 du fichier (« Ecriture sur les sessions reservee aux admins (sans capturer /api/session/{id}/participate/{userId}) ») indique que cette non-couverture est **connue et documentée dans le code**, pas un oubli silencieux. Aucun problème d'ordre de règles au sens strict (une règle générale ne masque pas une règle spécifique ici) : les règles 3 et 4 ne matchent tout simplement jamais les chemins `participate`, indépendamment de leur position.

Vérification des rôles : `UserDetailsImpl.getAuthorities()` (`security/services/UserDetailsImpl.java:34-38`) retourne `SimpleGrantedAuthority("ROLE_ADMIN")` pour un admin, `"ROLE_USER"` sinon — cohérent avec `hasRole("ADMIN")` qui préfixe automatiquement `ROLE_`. Aucun mismatch de nommage.

**Réponse tranchée à la question de couverture** : **CONFIRMÉ par lecture directe du pattern.** `POST /api/session/{id}/participate/{userId}` et `DELETE /api/session/{id}/participate/{userId}` ne sont capturés par aucune règle `hasRole("ADMIN")`. Ils retombent sur la règle 5 (`/api/**` → `authenticated()`), donc **tout utilisateur authentifié, admin ou non, peut les appeler**. Confirmé empiriquement par les tests d'intégration existants : `SessionControllerIT.participate_returns200_whenCalledByNonAdmin` (lignes 351-361) et `SessionControllerIT.noLongerParticipate_returns200_whenCalledByNonAdmin` (lignes 401-413) — si ces routes avaient été capturées par `hasRole("ADMIN")`, ces appels avec un token non-admin auraient échoué en 403, or ils réussissent en 200. Le commentaire de tête de la classe de test (lignes 34-40) indique explicitement que cette absence de restriction de rôle est **voulue** (l'inscription doit rester ouverte à tout utilisateur authentifié).

Ce point est distinct de la question de **propriété** (Q6 ci-dessous) : l'absence de `hasRole("ADMIN")` sur ces routes est intentionnelle et correcte (un non-admin doit pouvoir s'inscrire) ; ce qui n'est en revanche vérifié nulle part, c'est que le `{userId}` du path corresponde à l'utilisateur authentifié.

---

## Endpoints SessionController — grille des 7 questions

### GET /api/session/{id}

- **Q1** : Authentification requise (règle 5, `/api/**` → `authenticated()`), aucun rôle spécifique. Le code ne vérifie ni ne restreint côté serveur qui appelle au-delà de l'authentification — cohérent avec le besoin métier (une session est visible par tout utilisateur connecté). `SessionController.findById()` (`controllers/SessionController.java:36-39`) ne lit aucun principal.
- **Q2** : `sessionService.getById()` → `sessionRepository.findById(id).orElseThrow(NotFoundException::new)` (`services/SessionService.java:40-42`) → `GlobalExceptionHandler.handleNotFoundException` → HTTP 404 (`exception/GlobalExceptionHandler.java:25-28`). Confirmé par test.
- **Q3** : `Long.valueOf(id)` (`SessionController.java:38`) lève `NumberFormatException` sur id non numérique → `GlobalExceptionHandler.handleNumberFormatException` → HTTP 400 (`GlobalExceptionHandler.java:12-15`). Confirmé par test.
- **Q4** : N/A (lecture seule, pas de notion d'action déjà faite).
- **Q5** : N/A.
- **Q6** : N/A — pas de notion de propriétaire pour une lecture ouverte à tout utilisateur authentifié.
- **Q7** : Interprétable : 404 (id inexistant), 400 (id malformé), 401 (non authentifié). Aucun 500 identifié dans les cas couverts par les tests.
- **Scénario reproductible si problème identifié** : aucun problème identifié.
- **Test existant couvrant ce point** : `SessionControllerIT.findById_returns200AndSession_whenSessionExists`, `findById_returns404_whenSessionDoesNotExist`, `findById_returns400_whenIdIsNotNumeric`, `findById_returns401_whenNotAuthenticated` (lignes 105-147). Couvre 200/404/400/401.

### GET /api/session

- **Q1** : Authentification requise (règle 5), aucun rôle. `SessionController.findAll()` (`SessionController.java:41-46`) délègue directement à `sessionRepository.findAll()` sans condition.
- **Q2** : N/A (pas d'identifiant en entrée).
- **Q3** : N/A (pas de `@PathVariable`).
- **Q4** : N/A.
- **Q5** : N/A.
- **Q6** : N/A.
- **Q7** : 401 si non authentifié, testé. Pas de 500 identifié.
- **Scénario reproductible si problème identifié** : aucun problème identifié — endpoint marqué « zéro marqueur » dans l'inventaire (ligne 12), confirmé par lecture.
- **Test existant couvrant ce point** : `SessionControllerIT.findAll_returns200AndAllSessions_whenAuthenticated`, `findAll_returns401_whenNotAuthenticated` (lignes 151-167).

### POST /api/session (create)

- **Q1** : `hasRole("ADMIN")` explicite, règle 2 (`WebSecurityConfig.java:64`), chemin exact `POST /api/session`. Vérifié côté serveur par Spring Security avant d'atteindre le controller. Confirmé par test : un non-admin authentifié reçoit 403.
- **Q2** : N/A (création, pas d'id en entrée).
- **Q3** : Validation Bean Validation (`@Valid @RequestBody SessionDto`, `SessionController.java:49`) — champ `name` manquant → 400. Pas de conversion de path id sur cette route.
- **Q4** : N/A au sens de la grille — `SessionService.create()` (`services/SessionService.java:25-27`) appelle `sessionRepository.save(session)` sans aucune vérification de doublon/unicité ; chaque appel crée une nouvelle ligne, ce n'est pas une action « déjà faite » au sens idempotent.
- **Q5** : N/A.
- **Q6** : N/A — pas de notion de propriétaire, création réservée à l'admin par construction (Q1).
- **Q7** : 403 (non-admin), 400 (validation DTO), 401 (non authentifié) interprétables et testés. Pas de 500 identifié.
- **Scénario reproductible si problème identifié** : aucun problème identifié.
- **Test existant couvrant ce point** : `SessionControllerIT.create_returns200_whenCalledByAdmin`, `create_returns403_whenCalledByNonAdmin`, `create_returns400_whenNameIsMissing`, `create_returns401_whenNotAuthenticated` (lignes 171-218).

### PUT /api/session/{id} (update)

- **Q1** : `hasRole("ADMIN")` explicite, règle 3 (`WebSecurityConfig.java:65`), pattern `/api/session/*` — un seul segment, capture bien `/api/session/{id}`. Vérifié côté serveur **avant** le parsing de l'id par le controller : le test `update_returns403NotBadRequest_whenCalledByNonAdminWithInvalidId` (lignes 269-279) prouve qu'un id invalide combiné à un appelant non-admin reste bloqué en 403 (jamais 400), donc Spring Security intercepte bien la requête avant `Long.parseLong`.
- **Q2** : **Point à signaler.** `SessionService.update()` (`services/SessionService.java:44-47`) ne vérifie **pas** l'existence de la session avant d'appeler `session.setId(id)` puis `sessionRepository.save(session)` — contrairement à `delete()` qui fait un `existsById()` explicite (`SessionService.java:30`). Aucun test (IT ou unitaire) ne couvre le cas « update d'un id inexistant » : absence confirmée par lecture exhaustive de `SessionControllerIT.java` (pas de `update_returns404_whenSessionDoesNotExist`, contrairement à l'équivalent côté `delete`, lignes 330-337) et de `SessionServiceTest.java` (le seul test sur `update`, `should_setIdFromParameter_when_updateIsCalled`, lignes 108-119, mocke `sessionRepository.save()` pour toujours réussir et ne teste donc pas ce cas). `Session.id` utilise `GenerationType.IDENTITY` (`models/Session.java:45-47`) ; un `save()` avec un id non-null explicite passe par un `merge()` JPA — le comportement exact quand aucune ligne n'existe pour cet id (upsert silencieux vs exception Hibernate) n'est **pas déterminable par lecture de code seule**. **Indéterminé, nécessite un test manuel/Postman ou d'intégration dédié.**
- **Q3** : `Long.parseLong(id)` (`SessionController.java:60`) → `NumberFormatException` → 400. Confirmé par test.
- **Q4** : Rejoint Q2 — pas de notion « déjà fait » distincte pour un update, mais un second appel sur un id qui n'existait pas au premier suivrait le même comportement indéterminé.
- **Q5** : N/A.
- **Q6** : N/A — pas de propriétaire personnel d'une session, modification réservée à l'admin (Q1).
- **Q7** : 403 (non-admin), 400 (id malformé ou validation DTO), 401 (non authentifié) interprétables et testés. Le cas id inexistant (Q2) n'a pas de traitement explicite dans le code (ni 404 ni message métier) et n'est couvert par aucun test — risque de réponse non interprétable (200 avec création implicite, ou 500) non vérifié.
- **Scénario reproductible si problème identifié** :
  1. Obtenir un token admin valide (`generateAdminUserToken()`).
  2. Envoyer `PUT /api/session/999999` (id garanti inexistant en base) avec un `SessionDto` valide en payload.
  3. Résultat observé : indéterminé par lecture de code — le code ne contient aucune branche gérant ce cas explicitement (ni `orElseThrow`, ni `existsById`), donc soit la session est créée silencieusement avec l'id 999999 (comportement PUT-as-upsert non documenté pour un endpoint sémantiquement « update »), soit une exception JPA/SQL non catchée remonte en 500. Le scénario pour **provoquer** la situation est reproductible en 3 étapes ; le résultat exact ne l'est pas sans exécution réelle.
- **Test existant couvrant ce point** : aucun (absence confirmée, voir ci-dessus).

### DELETE /api/session/{id}

- **Q1** : `hasRole("ADMIN")` explicite, règle 4 (`WebSecurityConfig.java:66`), pattern `/api/session/*`. Vérifié côté serveur, confirmé par test (403 pour non-admin).
- **Q2** : `SessionService.delete()` vérifie `existsById()` avant `deleteById()` → `NotFoundException` → 404 (`SessionService.java:29-34`). C'est le fix Ex2-Back mentionné dans le contexte de ce prompt — confirmé par test.
- **Q3** : `Long.parseLong(id)` (`SessionController.java:67`) → `NumberFormatException` → 400 via `GlobalExceptionHandler`. **Pas de test IT dédié trouvé** pour cette route précise (grep confirmé : absent de `SessionControllerIT.java`, contrairement à `findById`/`update` qui ont chacun un test `_returns400_whenIdIsNotNumeric`). Comportement déductible par la mécanique commune du `GlobalExceptionHandler` (même pattern que les deux autres routes testées), donc raisonnablement fiable, mais **non vérifié empiriquement pour cette route**.
- **Q4** : Se supprimer deux fois → la 2ᵉ fois `existsById()` renvoie `false` → `NotFoundException` → 404. Comportement cohérent et couvert indirectement par le même mécanisme que Q2, mais aucun test dédié « double delete » (deux appels consécutifs dans le même test) n'existe.
- **Q5** : N/A.
- **Q6** : N/A — pas de propriétaire personnel d'une session, suppression réservée à l'admin (Q1).
- **Q7** : 403/404/401 interprétables et testés. Pas de 500 identifié.
- **Scénario reproductible si problème identifié** : aucun problème identifié — endpoint déjà corrigé et couvert (fix Ex2-Back).
- **Test existant couvrant ce point** : `SessionControllerIT.delete_returns200_whenCalledByAdmin`, `delete_returns403_whenCalledByNonAdmin`, `delete_returns404_whenSessionDoesNotExist`, `delete_returns401_whenNotAuthenticated` (lignes 307-346).

### POST /api/session/{id}/participate/{userId}

- **Q1** : Aucun rôle requis (voir cartographie ci-dessus) — authentification simple suffit, comportement **intentionnel et documenté** (commentaire code + commentaire de tête de test). Ce qui n'est en revanche vérifié nulle part : que l'appelant soit bien la personne concernée par `{userId}` (voir Q6).
- **Q2** : `session == null || user == null` → `NotFoundException` → 404 (`services/SessionService.java:49-54`). Testé pour session inexistante et pour user inexistant séparément.
- **Q3** : 2× `Long.parseLong` (`SessionController.java:73`) → `NumberFormatException` → 400 via `GlobalExceptionHandler`. **Pas de test IT dédié trouvé** pour cette route (grep confirmé : aucun test « participate…NotNumeric » dans `SessionControllerIT.java`). Comportement déductible par analogie avec les autres routes mais **non vérifié empiriquement ici**.
- **Q4** : `alreadyParticipate` (via `session.getUsers().stream().anyMatch(...)`, `SessionService.java:56-58`) → `BadRequestException` → 400. Testé.
- **Q5** : N/A (c'est l'action directe, pas l'inverse).
- **Q6** : **Point à signaler, avec scénario reproductible.** Aucune comparaison entre `{userId}` du path et le principal JWT authentifié. `SessionController.participate()` (`SessionController.java:71-76`) transmet `id` et `userId` tels quels à `sessionService.participate()` (`SessionService.java:49-64`), qui ne lit jamais le `SecurityContext`/`Authentication` — confirmé par lecture complète des deux fichiers : aucun import `Authentication`, `SecurityContextHolder`, ni `UserDetailsImpl` dans `SessionController.java` ou `SessionService.java`. Un utilisateur A authentifié peut donc inscrire n'importe quel `userId` B (existant en base) à une session, sans que B soit lui-même l'appelant.
- **Q7** : 404/400/401 interprétables pour les cas testés. Pas de 500 identifié. Cas id non-numérique non vérifié empiriquement (Q3).
- **Scénario reproductible si problème identifié (Q6 — usurpation de participation pour le compte d'autrui)** :
  1. Un utilisateur A (non-admin) obtient un token JWT valide via `POST /api/auth/login`.
  2. A envoie `POST /api/session/{id}/participate/{userId}` en substituant `{userId}` par l'id d'un utilisateur B (différent de A, existant en base), avec son propre token A en en-tête `Authorization`.
  3. Résultat observé (déductible du code, non exécuté empiriquement dans ce prompt read-only) : HTTP 200, B est ajouté à la liste des participants de la session — sans que B ait initié la requête ni que A soit admin. Le test existant `participate_returns200_whenCalledByNonAdmin` (lignes 352-361) prouve uniquement l'absence de restriction de rôle : il n'établit aucune relation entre le token utilisé et le `userId` du path, donc il ne couvre pas cette question de propriété (deux lacunes distinctes : absence de rôle — intentionnelle — et absence de vérification de propriété — non testée, non documentée comme intentionnelle).
- **Test existant couvrant ce point** : aucun pour la question de propriété (Q6). `participate_returns404_whenSessionDoesNotExist`, `participate_returns404_whenUserDoesNotExist`, `participate_returns400_whenAlreadyParticipating` (lignes 363-396) couvrent Q2 et Q4 uniquement.

### DELETE /api/session/{id}/participate/{userId}

- **Q1** : Symétrique à `participate` — non capturé par `hasRole("ADMIN")` (mêmes patterns), retombe sur `authenticated()` (règle 5). Confirmé par test (200 pour non-admin).
- **Q2** : `session == null` → `NotFoundException` → 404 (`SessionService.java:66-70`). Testé. **Asymétrie notée** : contrairement à `participate()`, `noLongerParticipate()` ne vérifie **jamais l'existence du user** via `userRepository` — seul le `userId` est comparé aux users déjà inscrits à la session (`stream().anyMatch`, ligne 72). Un `userId` totalement inexistant en base ne déclenche pas de `NotFoundException` dédiée à l'utilisateur ; il tombe dans la branche « non inscrit » → `BadRequestException` (400) au lieu d'un éventuel 404. C'est un comportement différent de `participate()` (asymétrie fonctionnelle), non testé explicitement pour ce cas précis (userId inexistant en base, distinct de « userId existant mais non inscrit », qui lui est testé).
- **Q3** : 2× `Long.parseLong` (`SessionController.java:80`) → `NumberFormatException` → 400. **Pas de test IT dédié trouvé** (même lacune que `participate`).
- **Q4** : N/A (c'est l'action « retour »).
- **Q5** : `!alreadyParticipate` (`SessionService.java:73-74`) → `BadRequestException` → 400. C'est directement le cas « action inverse jamais eu lieu ». Testé.
- **Q6** : **Point à signaler, avec scénario reproductible.** Même lacune que `participate()` : aucune comparaison `{userId}` vs principal JWT dans `SessionController.noLongerParticipate()` (`SessionController.java:78-83`) ni dans `SessionService.noLongerParticipate()`. Un utilisateur A peut désinscrire de force un utilisateur B.
- **Q7** : 404/400/401 interprétables et testés pour les cas couverts. Pas de 500 identifié.
- **Scénario reproductible si problème identifié (Q6 — désinscription forcée d'autrui)** :
  1. Un utilisateur B (non-admin) s'inscrit légitimement à une session (`POST participate` avec son propre id).
  2. Un utilisateur A (non-admin, différent de B) obtient son propre token JWT et envoie `DELETE /api/session/{id}/participate/{userId de B}`.
  3. Résultat observé (déductible du code, non exécuté empiriquement dans ce prompt read-only) : HTTP 200, B est retiré des participants sans avoir rien demandé et sans que A soit admin.
- **Test existant couvrant ce point** : aucun pour la question de propriété (Q6). `noLongerParticipate_returns200_whenCalledByNonAdmin`, `noLongerParticipate_returns404_whenSessionDoesNotExist`, `noLongerParticipate_returns400_whenNotParticipating` (lignes 401-434) couvrent Q1 (absence de rôle, intentionnelle), Q2 et Q5 uniquement.

---

## Récapitulatif des points relevés

| # | Endpoint | Question | Nature | Scénario reproductible ? |
|---|---|---|---|---|
| 1 | `PUT /api/session/{id}` | Q2 — pas de vérification d'existence avant `save()` | Comportement métier potentiellement anormal (upsert silencieux ou 500) | Scénario pour **provoquer** la situation : oui (3 étapes). Résultat exact : indéterminé par lecture de code seule, nécessite exécution. |
| 2 | `POST /api/session/{id}/participate/{userId}` | Q6 — `{userId}` du path jamais comparé au principal JWT | Un acteur peut agir au nom d'un autre | Oui, 3 étapes, résultat déduit du code (non exécuté) |
| 3 | `DELETE /api/session/{id}/participate/{userId}` | Q6 — idem, sens inverse | Un acteur peut agir au nom d'un autre | Oui, 3 étapes, résultat déduit du code (non exécuté) |
| 4 | `DELETE /api/session/{id}/participate/{userId}` | Q2 — asymétrie avec `participate()` (pas de vérification d'existence du user) | Fragilité / incohérence de contrat entre les deux endpoints miroirs | Non — argument (comparaison de code), pas de scénario où le comportement observé serait « faux » au sens strict (400 reste une réponse défendable) |
| 5 | `DELETE`, `POST participate`, `DELETE participate` (id non numérique) | Q3 — pas de test IT dédié à `_returns400_whenIdIsNotNumeric` sur ces 3 routes | Lacune de couverture de test, pas un bug de comportement (mécanique `GlobalExceptionHandler` commune et déjà éprouvée sur `GET`/`PUT`) | Non applicable — c'est un constat de couverture de test, pas un scénario de bug |

---

## Vérification anti-régression

`git status` (sortie observée avant écriture de ce fichier) ne montrait que `AUDIT_PHASE1_INVENTAIRE.md` en untracked, provenant d'une session précédente ; aucune modification d'aucun fichier existant n'a été effectuée par ce prompt. Seul `AUDIT_PHASE1_ZONE_AUTORISATION.md` est créé.
