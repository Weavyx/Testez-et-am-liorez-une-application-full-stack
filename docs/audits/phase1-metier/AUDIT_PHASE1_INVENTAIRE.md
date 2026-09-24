# Audit Phase 1 — Inventaire des contrats (endpoints back + actions front)

> Généré en lecture seule, conformément à METHODE_AUDIT.md. Aucune modification de fichier. Aucun jugement A/B/C/D ici — l'inventaire liste les marqueurs mécaniques ; l'arbitrage se fait en conversationnel (Phase 1 zone par zone / Phase 2).

## Endpoints back

| Méthode HTTP | Chemin | Classe.méthode | Rôle requis | Marqueurs de risque | Zéro marqueur ? |
|---|---|---|---|---|---|
| POST | /api/auth/login | AuthController.authenticateUser | Aucune (route publique — `permitAll` sur `/api/auth/**` dans WebSecurityConfig) | Lecture du principal JWT (cast `(UserDetailsImpl) authentication.getPrincipal()`) ; appelle `userService.isAdmin()` qui contient `Optional.map(...).orElse(false)` | Non |
| POST | /api/auth/register | AuthController.registerUser | Aucune (route publique — `permitAll`) | Appelle `userService.register()` qui contient une condition (`if existsByEmail`) et une écriture BDD (`save`) | Non |
| GET | /api/session/{id} | SessionController.findById | Authentifié (aucune restriction visible dans le controller ; fallback `/api/**` → `authenticated()` dans WebSecurityConfig) | Conversion `Long.valueOf(id)` sur `@PathVariable` ; appelle `sessionService.getById()` qui utilise `Optional.orElseThrow()` | Non |
| GET | /api/session | SessionController.findAll | Authentifié (aucune restriction visible) | Aucun (délègue à `sessionRepository.findAll()` sans condition/parsing/écriture) | **Oui** |
| POST | /api/session | SessionController.create | `hasRole(ADMIN)` (règle explicite dans WebSecurityConfig, méthode HTTP + chemin exacts) | Écriture BDD (`sessionService.create()` → `save`) | Non |
| PUT | /api/session/{id} | SessionController.update | `hasRole(ADMIN)` (règle explicite WebSecurityConfig sur `/api/session/*`) | Conversion `Long.parseLong(id)` ; écriture BDD (`sessionService.update()` → `session.setId(id)` puis `save`, sans vérifier que la session existe déjà) | Non |
| DELETE | /api/session/{id} | SessionController.save (méthode delete) | `hasRole(ADMIN)` (règle explicite WebSecurityConfig sur `/api/session/*`) | Conversion `Long.parseLong(id)` ; condition (`if !existsById → NotFoundException`) ; écriture BDD (`deleteById`) | Non |
| POST | /api/session/{id}/participate/{userId} | SessionController.participate | **Aucune restriction de rôle visible** — le pattern WebSecurityConfig `hasRole(ADMIN)` ne cible que `/api/session` et `/api/session/*` (un seul segment), donc ne capture pas `/api/session/*/participate/*` ; cette route retombe sur `/api/**` → `authenticated()` simple. Aucune vérification que `userId` correspond à l'utilisateur courant. | 2× conversion `Long.parseLong` ; appelle `sessionService.participate()` qui contient 2 conditions (`session==null\|\|user==null`, `alreadyParticipate`), une opération sur collection (`stream().anyMatch`), un ajout en collection (`list.add`) et une écriture BDD (`save`) | Non |
| DELETE | /api/session/{id}/participate/{userId} | SessionController.noLongerParticipate | Idem ligne précédente — non capturé par les règles `hasRole(ADMIN)`, retombe sur `authenticated()` simple. Aucune vérification que `userId` correspond à l'utilisateur courant. | 2× conversion `Long.parseLong` ; appelle `sessionService.noLongerParticipate()` qui contient 2 conditions, une opération sur collection (`stream().anyMatch`), un filtrage de collection (`stream().filter`) et une écriture BDD (`save`) | Non |
| GET | /api/teacher/{id} | TeacherController.findById | Authentifié (aucune restriction visible) | Conversion `Long.valueOf(id)` ; appelle `teacherService.findById()` qui utilise `Optional.orElseThrow()` | Non |
| GET | /api/teacher | TeacherController.findAll | Authentifié (aucune restriction visible) | Aucun (délègue à `teacherRepository.findAll()` sans condition/parsing/écriture) | **Oui** |
| GET | /api/user/{id} | UserController.findById | Authentifié (aucune restriction visible) | Conversion `Long.valueOf(id)` ; appelle `userService.findById()` qui utilise `Optional.orElseThrow()` | Non |
| DELETE | /api/user/{id} | UserController.save (méthode delete) | Authentifié + contrôle de propriété explicite dans le controller/service (voir marqueurs) | Conversion `Long.parseLong(id)` ; lecture du principal JWT dans le controller (cast `(UserDetails) SecurityContextHolder...getPrincipal()`) ; appelle `userService.deleteById()` qui contient une condition d'autorisation (`if !Objects.equals(currentUsername, user.getEmail()) → UnauthorizedException`) et une écriture BDD (`deleteById`) | Non |

**Note transverse** : aucune annotation `@PreAuthorize` n'est utilisée dans le repo — toutes les restrictions de rôle sont centralisées dans `WebSecurityConfig.securityFilterChain()` (matchers par méthode HTTP + chemin), jamais au niveau des méthodes de controller elles-mêmes.

## Actions front

| Composant.méthode | Appel(s) API | Marqueurs de risque | Zéro marqueur ? |
|---|---|---|---|
| ListComponent (init de champ `sessions$`) | GET /api/session (via `SessionApiService.all()`) | Aucun dans le composant TS lui-même | **Oui** |
| ListComponent (template `list.component.html`) | — (affichage conditionnel, pas d'appel API) | Assertion non-null `user!.admin` (×2) utilisée pour l'affichage conditionnel des boutons Create/Edit (rôle admin) | Non |
| DetailComponent (constructeur) | — | 2× assertion non-null : `this.sessionService.sessionInformation!.admin`, `this.sessionService.sessionInformation!.id` ; `this.route.snapshot.paramMap.get('id')!` | Non |
| DetailComponent.fetchSession (privée, appelée par `ngOnInit`) | GET /api/session/{id} (`SessionApiService.detail`) puis GET /api/teacher/{id} (`TeacherService.detail`, imbriqué dans le `subscribe`) | Assertion non-null `sessionService.sessionInformation!.id` ; opération sur collection `session.users.some(...)` ; souscriptions imbriquées (fragilité, hors liste stricte des marqueurs mais à noter) | Non |
| DetailComponent.delete | DELETE /api/session/{id} (`SessionApiService.delete`) | Aucun condition/assertion propre à la méthode (navigation + snackbar seulement) | **Oui** |
| DetailComponent.participate | POST /api/session/{id}/participate/{userId} (`SessionApiService.participate`) | Aucun | **Oui** |
| DetailComponent.unParticipate | DELETE /api/session/{id}/participate/{userId} (`SessionApiService.unParticipate`) | Aucun | **Oui** |
| DetailComponent (template `detail.component.html`) | — | Affichage conditionnel lié au rôle : `@if (isAdmin)` / `@if (!isAdmin)` | Non |
| FormComponent.ngOnInit | GET /api/teacher (init de champ `teachers$`) ; GET /api/session/{id} (`SessionApiService.detail`) si mode update | Condition `if (!sessionService.sessionInformation!.admin) → navigate` (garde de rôle **côté client uniquement**, assertion non-null incluse) ; condition `if (url.includes('update'))` ; `this.route.snapshot.paramMap.get('id')!` | Non |
| FormComponent.submit | POST /api/session (`create`) **ou** PUT /api/session/{id} (`update`) selon `onUpdate` | Condition `if (!this.onUpdate)` ; assertion non-null `this.id!` | Non |
| MeComponent.ngOnInit | GET /api/user/{id} (`UserService.getById`) | Assertion non-null `sessionService.sessionInformation!.id` | Non |
| MeComponent.delete | DELETE /api/user/{id} (`UserService.delete`) | Assertion non-null `sessionService.sessionInformation!.id` ; enchaîne `sessionService.logOut()` + navigation après succès | Non |
| LoginComponent.submit | POST /api/auth/login (`AuthService.login`) | Branchement `next`/`error` de l'`Observable` (positionne `onError`) | Non |
| RegisterComponent.submit | POST /api/auth/register (`AuthService.register`) | Branchement `next`/`error` de l'`Observable` (positionne `onError`) | Non |
| AppComponent.logout | Aucun appel API — `SessionService.logOut()` est purement local (aucune invalidation serveur du token), suivi d'une navigation | Aucun | **Oui** |
| NotFoundComponent | Aucun appel API, aucune action | — | **Oui** |

