# AUDIT QUALITÉ INTRINSÈQUE DES TESTS

Diagnostic **read-only** de la qualité intrinsèque de chaque test du projet :
granularité, clarté des assertions, duplication, setup/arrange, valeur du test.

**Hors périmètre volontairement** (déjà traité par les audits précédents) :
- classification unitaire/intégration → `AUDIT_PHASE3_CLASSEMENT_TESTS.md`
- comportements manquants / couverture fonctionnelle → `AUDIT_PHASE4_COMPORTEMENTS_TESTES.md`

**Convention de sévérité**
- *notable* : nuit réellement à la fiabilité du diagnostic en cas d'échec, ou masque une régression possible.
- *mineur* : dette de lisibilité/maintenance, sans risque de faux vert immédiat.

**Rappel de la règle d'analyse appliquée** : plusieurs `jsonPath`/`expect` portant sur
un même corps de réponse ou sur les conséquences d'une seule action ne sont **pas**
comptés comme un mélange de comportements. Seuls sont signalés les tests qui vérifient
des comportements réellement indépendants.

---

## 1. Back (`back/src/test/java/**`)

| Fichier | Test | Problème identifié | Catégorie | Sévérité |
|---|---|---|---|---|
| `services/SessionServiceTest.java` | `should_returnNull_when_getByIdIsCalled_and_sessionDoesNotExist` (l.130) | Le nom annonce un retour `null`, le corps vérifie une exception : `assertThatThrownBy(() -> sessionService.getById(99L)).isInstanceOf(NotFoundException.class)`. Le nom décrit l'ancien comportement, pas le comportement testé. | clarté | notable |
| `services/SessionServiceTest.java` | Tous les tests `participate` / `noLongerParticipate` | Le `@BeforeEach authenticateAs(10L)` (l.55-57) pose un principal implicite pour toute la classe ; le commentaire l'assume (« les tests existants appellent tous ces méthodes avec userId=10L, donc on authentifie par défaut ce même id ici pour ne pas avoir à modifier chaque test individuel »). Le lien « userId=10L dans l'appel ↔ principal authentifié » n'est visible dans aucun des tests nominaux ; les tests 403 doivent écraser ce défaut par `authenticateAs(999L)` en première ligne (l.213, l.278). | setup | notable |
| `services/UserServiceTest.java` | `should_returnUser_when_findByIdIsCalled_and_userExists` (l.167) et `should_returnUser_when_findByIdIsCalled_and_requesterIsAnotherUser` (l.217) | Même stub (`when(userRepository.findById(1L)).thenReturn(Optional.of(user))`) et même assertion (`isEqualTo(user)`) ; seule différence : `authenticateAs(2L)`. Le second n'ajoute une garantie que si l'absence de contrôle de propriété est le point testé — ce que l'assertion ne dit pas. | duplication | mineur |
| `services/UserServiceTest.java` | `should_deleteUser_when_deleteByIdIsCalled_and_requesterIsTheOwner` (l.71) | Entièrement inclus dans `should_removeUserFromParticipatedSessions_when_deleteByIdIsCalled` (l.117), qui refait `verify(userRepository).deleteById(1L)` (l.138) en plus de ses propres assertions. | duplication | mineur |
| `services/UserServiceTest.java` | `should_throwForbiddenException_when_deleteByIdIsCalled_and_onlyTheEmailMatches` (l.99) | Reconstruit à la main le `UserDetailsImpl` + `SecurityContextHolder.getContext().setAuthentication(...)` (l.103-105) alors que le helper `authenticateAs(Long)` existe (l.62) — divergence de setup non nécessaire (le helper force `username = "user"+id+"@studio.com"`, d'où le contournement, mais rien ne le signale). | setup | mineur |
| `models/UserTest.java` | `should_throwNullPointerException_when_builderBuildIsCalled_and_*IsNull` (l.153, 159, 165, 171) | Les 4 noms annoncent `builderBuildIsCalled`, mais `build()` n'est jamais appelé : `isThrownBy(() -> User.builder().email(null))`. C'est le *setter du builder* qui est testé, pas `build()`. | clarté | notable |
| `models/UserTest.java` | `should_containFieldValues_when_builderToStringIsCalled` (l.177) | Construit un builder à 8 champs pour n'asserter que `contains("a@studio.com")` : teste le `toString()` généré par Lombok sur le builder (jamais utilisé en production), et une seule valeur sur 8. | valeur | mineur |
| `models/TeacherTest.java` | `should_containFieldValues_when_builderToStringIsCalled` (l.84) | Idem : 5 champs alimentés, une seule assertion `contains("Doe")` sur un `toString()` de builder Lombok. | valeur | mineur |
| `models/SessionTest.java` | `should_containFieldValues_when_builderToStringIsCalled` (l.90) | Idem, avec en plus un `.users(Collections.emptyList())` (l.94) qui n'intervient dans aucune assertion. | valeur / setup | mineur |
| `models/UserTest.java`, `TeacherTest.java`, `SessionTest.java` | `should_assignAllFields_when_settersAreCalled` (l.77 / l.66 / l.68) | Vérifie 5 à 8 setters/getters générés par Lombok en un seul test : ne peut échouer que si Lombok lui-même est cassé, et en cas d'échec ne dit pas quel champ est en cause. | valeur / granularité | mineur |
| `models/SessionTest.java`, `TeacherTest.java`, `UserTest.java` | `should_returnTrue_when_equalsIsCalledWithSameId_and_differentOtherFields` | Le test vérifie deux contrats distincts : `assertThat(session1.equals(session2)).isTrue()` **et** `assertThat(session1.hashCode()).isEqualTo(session2.hashCode())`. Le nom ne mentionne que `equals`. | granularité | mineur |
| `security/services/UserDetailsImplTest.java` | `should_returnTrue_forAllAccountStatusFlags` (l.58) | Quatre méthodes indépendantes assertées dans un seul test (`isAccountNonExpired`, `isAccountNonLocked`, `isCredentialsNonExpired`, `isEnabled`) ; ce sont par ailleurs quatre `return true` en dur dans l'implémentation. Un échec ne pointe pas le flag fautif. | granularité / valeur | notable |
| `security/jwt/JwtUtilsTest.java` | `should_returnFalse_when_validateJwtTokenIsCalled_with_nullOrEmptyToken` (l.116) | Deux cas d'entrée dans un seul test : `validateJwtToken(null)` puis `validateJwtToken("")`. Si le premier échoue, le second n'est jamais exécuté et le diagnostic est ambigu. | granularité | mineur |
| `security/jwt/AuthTokenFilterTest.java` | `should_logErrorAndContinueChain_when_userDetailsServiceThrows` (l.47) | Le nom promet un log (`logError`) qui n'est vérifié par aucune assertion ; le test ne contrôle que `getAuthentication()).isNull()` et `verify(filterChain).doFilter(...)`. | clarté | mineur |
| `ApplicationContextIT.java` | `contextLoads` (l.8) | Corps entièrement vide, aucune assertion. Smoke test conventionnel, mais qui ne documente rien de ce qui est censé démarrer. | valeur | mineur |
| `controllers/SessionControllerIT.java` | `findAll_returns200AndAllSessions_whenAuthenticated` (l.167) | Assertion tautologique : `jsonPath("$[*].name", containsInAnyOrder("Hatha Yoga", "Hatha Yoga"))` — `persistSession()` (l.78) code le nom en dur, les deux sessions portent donc le même nom et l'assertion ne peut discriminer aucune inversion ni aucun mauvais mapping. | clarté | notable |
| `controllers/SessionControllerIT.java` | `noLongerParticipate_returns400_whenIdIsNotNumeric` (l.553) | Arrange inutile : `persistTeacher()` + `persistSession()` + `persistParticipant()` + `session.getUsers().add(...)` + `sessionRepository.save(session)` (l.554-558) alors que le path est `"abc"` et que `Long.parseLong` échoue avant tout accès au service. Le test frère `participate_returns400_whenIdIsNotNumeric` (l.473) ne persiste, lui, qu'un participant. | setup | notable |
| `controllers/SessionControllerIT.java` | `delete_returns200_whenCalledByAdmin` (l.347) | N'assert que `status().isOk()` : aucune vérification que la session a réellement disparu, alors que le pattern existe dans le même fichier (`assertThat(sessionRepository.existsById(session.getId())).isFalse()`, l.427). | clarté | mineur |
| `controllers/SessionControllerIT.java`, `controllers/TeacherControllerIT.java` | `findAll_*` (l.175 / l.109) | `jsonPath("$", hasSize(2))` suppose la table vide au début du test. C'est vrai aujourd'hui (rollback `@Transactional`, aucun `data.sql` dans `back/src/test/resources`), mais l'assertion est couplée à un état global plutôt qu'aux seules données du test. | setup | mineur |
| `controllers/AuthControllerIT.java` | `register_returns400_whenEmailIsBlank` (l.117), `register_returns400_whenEmailFormatIsInvalid` (l.125), `register_returns400_whenPasswordIsTooShort` (l.133) | Les trois tests n'assertent que `status().isBadRequest()`, sans aucune assertion sur le corps. Ils sont indistinguables : n'importe quel 400 (même provoqué par une autre cause) les fait passer tous les trois. Le fichier montre pourtant le pattern correct juste à côté (`jsonPath("$.message").value("Error: Email is already taken!")`, l.113). | clarté | notable |
| `controllers/UserControllerIT.java` | `findById_returns403_whenUserReadsAnotherUsersAccount` (l.110), `delete_returns403_whenUserTriesToDeleteAnotherUsersAccount` (l.203) | `assertThat(userA.getEmail()).isNotEqualTo(userB.getEmail())` (l.113 / l.206) est une assertion sur la fixture, pas sur le comportement testé : elle vérifie `AbstractIntegrationTest#persistUser`, dans un test dont le sujet est le contrôle d'autorisation. | granularité | mineur |
| `exception/GlobalExceptionHandlerTest.java` | `should_returnBadRequestWithoutBody_when_..._messageIsNull` (l.14) / `..._messageIsBlank` (l.22) | Corps strictement identiques hors argument (`new BadRequestException()` vs `new BadRequestException("   ")`), mêmes deux assertions. Cas de test paramétré non factorisé (`@ParameterizedTest`). | duplication | mineur |

---

## 2. Front (`front/src/**/*.spec.ts`)

| Fichier | Test | Problème identifié | Catégorie | Sévérité |
|---|---|---|---|---|
| 11 fichiers sur 13 | `should create` / `should be created` | Onze tests identiques dans le projet, tous de la forme `expect(component).toBeTruthy()` / `expect(service).toBeTruthy()` (`app.component.spec.ts:53`, `me:118`, `auth.service:52`, `session-api.service:36`, `session.service:44`, `teacher.service:36`, `login:86`, `not-found:36`, `register:93`, `detail:127`, `form:126`, `list:143`). Aucun ne peut échouer sans qu'un autre test du même fichier échoue d'abord ; l'assertion ne vérifie aucune valeur réelle. | valeur / duplication | notable |
| `core/service/teacher.service.spec.ts`, `core/service/session-api.service.spec.ts` | fichier entier | Les deux fichiers ne contiennent **que** le `should be created` ci-dessus. Leur en-tête l'assume (« les appels HTTP CRUD sont exercés indirectement via les tests de ListComponent/DetailComponent/FormComponent ») : le fichier n'apporte donc aucune garantie propre. | valeur | notable |
| `app.component.spec.ts` | `should reflect the session state through $isLogged() (false, then true after logIn)` (l.59) | Deux problèmes : (1) duplique `session.service.spec.ts` « should emit false initially » (l.54) + « should emit true after logIn » (l.60) — `AppComponent.$isLogged()` ne fait que déléguer ; (2) le test souscrit deux fois en réutilisant la même variable (`app.$isLogged().subscribe(...)` l.65 puis l.69) : la seconde assertion prouve seulement que le `BehaviorSubject` rejoue sa valeur courante à une **nouvelle** souscription, pas qu'il a notifié la souscription initiale. | duplication / clarté | notable |
| `pages/register/register.component.spec.ts` | `should set onError to true when registration fails` (l.60) | `expect(errorElement).toBeTruthy()` (l.74) : vérifie la présence du nœud `.error` sans son contenu. Le test frère de `login.component.spec.ts` (l.72) fait, lui, `expect(errorElement.textContent).toContain('An error occurred')`. Un message d'erreur vide ou erroné passerait ici. | clarté | notable |
| `pages/sessions/components/detail/detail.component.spec.ts` | `should toggle the DOM to "Do not participate"...` (l.194) vs `should call the participate API..., then reload the session` (l.221) | Même séquence rejouée à l'identique : POST `api/session/1/participate/1`, puis GET `api/session/1` flushé avec `{ ...mockSession, users: [1, 2] }`, puis GET `api/teacher/1`. Seule l'assertion finale diffère (DOM vs `component.isParticipate`). Idem pour la paire unParticipate (l.269 et l.296, avec `users: []`). | duplication | notable |
| `pages/sessions/components/detail/detail.component.spec.ts` | `beforeEach` des 3 blocs (l.81, l.158, l.240) | Trois blocs de 14 lignes identiques à deux valeurs près (`admin: true/false` et la session flushée). Toute évolution du composant (ex. un 3ᵉ appel HTTP à l'init) impose trois corrections. | setup | mineur |
| `pages/sessions/components/form/form.component.spec.ts` | `should redirect a non-admin user to /sessions on init` (l.208) | L'assertion `expect(mockRouter.navigate).toHaveBeenCalledWith(['/sessions'])` porte sur un appel déclenché dans le `beforeEach` (l.187), pas dans le corps du test : le test n'a plus de phase Act et dépend entièrement de l'ordre setup → assert, avec un `mockRouter` partagé au niveau du `describe` (l.168) remis à zéro par un `jest.clearAllMocks()` global (l.172). | setup | mineur |
| `pages/sessions/components/form/form.component.spec.ts` | `should call the update API and navigate to sessions on submit` (l.277) | `component.submit()` est appelé sans modifier le formulaire, et l'assertion ne porte que sur `req.request.method` et l'URL : le **corps** du PUT n'est jamais vérifié. Une régression du mapping formulaire → payload passerait inaperçue. | clarté | mineur |
| `pages/sessions/components/form/form.component.spec.ts` | `should be in create mode (onUpdate = false)` (l.130) / `should be in edit mode (onUpdate = true)` (l.265) | Assertion sur un flag interne entièrement déterminé par le `mockRouter.url` fourni par le test lui-même (`'/sessions/create'` / `'/sessions/update/1'`) : le test rejoue la condition qu'il a posée. | valeur | mineur |
| `pages/sessions/components/list/list.component.spec.ts` | `should create` (l.143) | Doit exécuter tout le setup (`await setup(...)`, `detectChanges`, `httpMock.expectOne('api/session').flush([])`) uniquement pour asserter `expect(fixture.componentInstance).toBeTruthy()` — le coût de setup est sans rapport avec ce qui est vérifié. | valeur / setup | mineur |
| `components/me/me.component.spec.ts` | `should fetch and display the user information in the DOM` (l.93) | Mélange trois niveaux de vérification indépendants : l'appel de service (`expect(userService.getById).toHaveBeenCalledWith('1')`), l'état interne (`expect(component.user).toEqual(mockUser)`) et le rendu DOM (`text).toContain('John')`). Les deux premiers relèvent d'un test unitaire de `ngOnInit`, le troisième du rendu. | granularité | mineur |
| `components/me/me.component.spec.ts` | fichier entier | `mockSessionService` et `mockMatSnackBar` (l.38-48) sont des objets mutables portés par le `describe` et réinitialisés à la main dans `afterEach` (`mockSessionService.logOut.mockClear()`, `mockMatSnackBar.open.mockClear()`, l.88-89). Tout `jest.fn()` ajouté plus tard sans ligne de nettoyage correspondante fuiterait d'un test à l'autre. | setup | mineur |
| `core/service/session.service.spec.ts` | `should initialize as not logged in` (l.48) et `$isLogged() > should emit false initially` (l.54) | Recouvrement partiel : les deux vérifient l'état initial « non connecté », l'un via la propriété `isLogged`, l'autre via l'observable adossé à la même valeur. | duplication | mineur |

---

## 3. E2E (`front/cypress/e2e/**/*.cy.ts`)

| Fichier | Test | Problème identifié | Catégorie | Sévérité |
|---|---|---|---|---|
| Les 9 fichiers | ~30 tests sur 37 | Le préambule de connexion est recopié tel quel partout : `cy.intercept('POST', '/api/auth/login', { statusCode: 200, fixture: 'login-success.json' })` + `cy.intercept('GET', '/api/session', [])` + `cy.get('input[formControlName=email]').type(...)` + `.type('test!1234')` + `cy.get('button[type=submit]').click()` + `cy.wait('@loginRequest')`. `front/cypress/support/commands.ts` est resté **intégralement commenté** (y compris l'exemple `// Cypress.Commands.add("login", (email, password) => { ... })`) : aucune commande custom n'a été créée. | duplication / setup | notable |
| `login.cy.ts`, `logout.cy.ts`, `register.cy.ts`, `account.cy.ts`, `sessions-list.cy.ts`, `sessions-create.cy.ts`, `sessions-detail.cy.ts`, `sessions-update.cy.ts`, `sessions-delete.cy.ts` | les 9 tests `(real backend)` | Dépendances externes implicites, documentées uniquement en commentaire et jamais vérifiées par une assertion préalable : backend Docker démarré, compte `yoga@studio.com` seedé « manuellement sur cet environnement local » (`login.cy.ts:65-67`), et `insert_teacher.sql` appliqué à la main (`sessions-create.cy.ts:18-20`). Un environnement neuf fait échouer ces tests pour une raison sans rapport avec le code. Seul `sessions-list.cy.ts` (l.121-124) transforme cette dépendance en message d'échec explicite. | setup | notable |
| `register.cy.ts` | `register success (real backend)` (l.65) | Le test crée un compte réel qu'il ne supprime jamais — le commentaire l'assume : « aucun cleanup API n'est disponible côté front, le compte créé reste en base après le test (le volume MySQL est persistant entre redémarrages du back) ». Chaque exécution pollue durablement la base partagée par toutes les autres suites. | setup | notable |
| `account.cy.ts` | `account (real backend) - displays real user data and delete works` (l.153) | Un seul `it()` enchaîne cinq comportements indépendants : inscription (l.159-166), connexion (l.168-172), navigation vers /me, affichage des données (l.179-181), suppression du compte (l.183-186) et vérification hors UI que le login échoue désormais en 401 (l.190-197). Le nom lui-même annonce deux sujets (« displays real user data **and** delete works »). Un échec ne localise rien. | granularité | notable |
| `sessions-update.cy.ts` | `session update (real backend) - full lifecycle` (l.168) | Duplique intégralement les assertions de préremplissage du test mocké `session update - form pre-filled...` (l.34) — le commentaire l.212 le dit : « Préremplissage réel, mêmes assertions que le test mocké ci-dessus » — avant d'enchaîner sur le scénario de mise à jour proprement dit. | duplication / granularité | notable |
| `logout.cy.ts` | `logout - protected routes become inaccessible after logout (mock)` (l.55) | Rejoue à l'identique tout l'arrange du test précédent (l.25) : mêmes deux `cy.intercept`, même saisie, même clic Logout, et refait `cy.url().should('include', '/login')` (l.70) déjà asserté l.44. Seules les deux dernières lignes (`cy.visit('/sessions')` / `cy.visit('/me')`) sont propres à ce test. | duplication | mineur |
| `sessions-list.cy.ts`, `sessions-create.cy.ts`, `sessions-detail.cy.ts`, `sessions-update.cy.ts`, `sessions-delete.cy.ts` | hook `afterEach` | Le hook de cleanup (relogin `cy.request` + `DELETE /api/session/{id}` via l'alias `@createdSessionId`) est copié à l'identique dans 5 fichiers, avec une divergence non factorisée : `failOnStatusCode: false` n'est présent que dans `sessions-detail.cy.ts` (l.284) et `sessions-delete.cy.ts` (l.178). Le hook s'exécute par ailleurs après **chaque** test de la suite, y compris les tests mockés qui n'ont rien créé. | duplication / setup | mineur |
| `sessions-list.cy.ts` | `sessions list - Create/Edit buttons hidden for non-admin (mock)` (l.79) | `cy.get('.item').each(($item) => { cy.wrap($item).contains('button', 'Edit').should('not.exist') })` : si la fixture renvoyait une liste vide, le `each` n'itérerait jamais et le test passerait sans avoir rien vérifié. Aucune assertion de garde sur la longueur (contrairement à `sessions list - displays sessions`, l.54, qui fait `should('have.length', sessions.length)`). | clarté | mineur |
| `login.cy.ts` | `login success (mock)` (l.6) | Seule assertion de résultat : `cy.url().should('include', '/sessions')`. Ni l'état connecté de la toolbar ni le payload envoyé ne sont vérifiés, alors que la variante réelle du même fichier le fait (`cy.contains('.link', 'Logout').should('be.visible')`, l.76). | clarté | mineur |
| `sessions-delete.cy.ts` | `session detail - stale/deleted session id shows empty page (mock)` (l.125) | Test dont le nom et le sujet (affichage du détail sur 404) appartiennent à `sessions-detail.cy.ts` ; il est isolé dans la suite `Session delete spec`, ce qui rend le périmètre du fichier ambigu. | clarté | mineur |
| `account.cy.ts` (l.34-42), `sessions-detail.cy.ts` (l.16-24), `sessions-list.cy.ts` (l.82-90) | constantes `nonAdminLogin` | Trois définitions locales d'un même « login non-admin », avec des valeurs différentes (`jane.doe@test.com` / `user@test.com` / `user@test.com` inliné dans le test), aucune fixture partagée alors que `login-success.json` existe pour le cas admin. Le commentaire d'`account.cy.ts` (l.15-16) reconnaît l'absence de fixture dédiée. | duplication / setup | mineur |
| `sessions-create.cy.ts` | `session create - form displayed for admin (mock)` (l.27) | Vérifie l'affichage des 4 champs du formulaire **puis** l'état initial du bouton submit (`cy.get('button[type=submit]').should('be.disabled')`, l.54), qui est précisément le sujet du test suivant `session create - submit disabled when required field missing` (l.57). | granularité / duplication | mineur |

---

## 4. Synthèse

### 4.1 Volumétrie analysée

| Partie | Fichiers | Tests |
|---|---|---|
| Back (`*Test.java` / `*IT.java`) | 17 (dont 1 classe de base abstraite sans test) | **170** |
| Front (`*.spec.ts`) | 13 | **65** |
| E2E (`*.cy.ts`) | 9 | **37** |
| **Total** | **39** | **272** |

Détail back : `SessionControllerIT` 35, `UserTest` 22, `UserServiceTest` 21, `SessionServiceTest` 19, `UserControllerIT` 11, `AuthControllerIT` 9, `SessionTest` 9, `TeacherTest` 9, `UserDetailsImplTest` 9, `JwtUtilsTest` 8, `TeacherControllerIT` 6, `TeacherServiceTest` 4, `AuthTokenFilterTest` 3, `GlobalExceptionHandlerTest` 2, `UserDetailsServiceImplTest` 2, `ApplicationContextIT` 1.

Détail front : `detail` 13, `form` 12, `session.service` 9, `me` 5, `list` 5, `app.component` 4, `login` 4, `register` 4, `auth.service` 3, `user.service` 3, `session-api.service` 1, `teacher.service` 1, `not-found` 1.

Détail e2e : `sessions-detail` 6, `sessions-list` 5, `login` 4, `register` 4, `account` 4, `sessions-create` 4, `sessions-update` 4, `logout` 3, `sessions-delete` 3.

### 4.2 Problèmes par catégorie

| Catégorie | Back | Front | E2E | Total |
|---|---|---|---|---|
| Granularité | 4 | 1 | 2 | 7 |
| Clarté des assertions | 6 | 3 | 3 | 12 |
| Duplication | 3 | 4 | 5 | 12 |
| Setup / arrange | 5 | 3 | 4 | 12 |
| Valeur du test | 5 | 4 | 0 | 9 |
| **Total (entrées de tableau, catégories multiples comptées une fois par catégorie)** | **23** | **15** | **14** | **52** |

Nombre d'entrées distinctes dans les tableaux : **22 back / 13 front / 12 e2e = 47** (certaines entrées portent deux catégories).

Répartition par sévérité : **17 notables**, **30 mineurs**.

### 4.3 Les 10 problèmes les plus notables, priorisés

1. **`AuthControllerIT` — trois tests de validation `register` indistinguables** (l.117, 125, 133). Ils n'assertent que `status().isBadRequest()` : n'importe quel 400, même provoqué par une autre cause (JSON malformé, contrainte DB), les fait passer tous les trois. C'est le seul cas de l'audit où un test peut rester vert alors que la règle métier visée a disparu. *(clarté)*

2. **`SessionControllerIT.findAll_returns200AndAllSessions_whenAuthenticated` — assertion tautologique** : `containsInAnyOrder("Hatha Yoga", "Hatha Yoga")` sur deux sessions créées avec le même nom en dur par `persistSession()`. L'assertion ne peut détecter ni inversion ni mauvais mapping de nom. *(clarté)*

3. **E2E — préambule de login recopié dans ~30 tests sur 37**, alors que `cypress/support/commands.ts` est resté intégralement commenté (l'exemple `Cypress.Commands.add("login", ...)` y figure toujours en commentaire). C'est le premier poste de dette de maintenance du périmètre e2e : tout changement de sélecteur du formulaire de login impacte 9 fichiers. *(duplication / setup)*

4. **E2E — les 9 tests `(real backend)` dépendent d'un état d'environnement non vérifié** (Docker démarré, `yoga@studio.com` seedé à la main, `insert_teacher.sql` appliqué manuellement). Seul `sessions-list.cy.ts` (l.121-124) traduit cette dépendance en message d'échec explicite ; ailleurs l'échec est muet et attribué à tort au code applicatif. *(setup)*

5. **`register.cy.ts::register success (real backend)` pollue durablement la base** : chaque exécution crée un compte jamais supprimé (commentaire l.68-70). Combiné au point 4, cela fait dériver l'environnement de test à chaque run. *(setup)*

6. **`account.cy.ts::account (real backend)` — cinq comportements dans un seul `it()`** (register, login, affichage, suppression, vérification 401 hors UI). Le nom annonce lui-même deux sujets. Un échec n'indique pas quelle étape a cassé. *(granularité)*

7. **Front — 11 tests `should create` / `should be created` à `toBeTruthy()`**, dont deux fichiers (`teacher.service.spec.ts`, `session-api.service.spec.ts`) qui ne contiennent *que* cela. Aucun ne peut échouer sans qu'un autre test du même fichier échoue d'abord. *(valeur)*

8. **`UserTest` — 4 tests nommés `builderBuildIsCalled` qui n'appellent jamais `build()`** : `isThrownBy(() -> User.builder().email(null))`. Le nom désigne le mauvais point d'entrée, ce qui fausse la lecture de ce qui est réellement garanti (le `@NonNull` sur le setter du builder, pas sur `build()`). *(clarté)*

9. **`SessionServiceTest` — principal authentifié posé globalement par `@BeforeEach authenticateAs(10L)`**, avec un commentaire qui assume le couplage à la valeur `userId=10L` codée dans chaque test. Les tests 403 doivent écraser ce défaut en première ligne. Dépendance cachée entre setup de classe et corps des tests. *(setup)*

10. **`detail.component.spec.ts` — les paires participate/unParticipate rejouent deux fois la même séquence HTTP** (l.194 vs l.221, l.269 vs l.296) : mêmes trois requêtes, mêmes payloads, seule l'assertion finale diffère (DOM vs `component.isParticipate`). *(duplication)*

**Mentions immédiatement suivantes** : `SessionServiceTest.should_returnNull_when_getByIdIsCalled...` (nom contredisant le corps), `UserDetailsImplTest.should_returnTrue_forAllAccountStatusFlags` (4 comportements en un test), `SessionControllerIT.noLongerParticipate_returns400_whenIdIsNotNumeric` (arrange à 5 lignes entièrement inutile), `register.component.spec.ts` (`toBeTruthy()` sur le nœud `.error` sans vérifier le message), `app.component.spec.ts::$isLogged()` (duplication de `session.service.spec.ts` + double souscription non concluante).

---

*Audit read-only — aucun fichier de test modifié.*
