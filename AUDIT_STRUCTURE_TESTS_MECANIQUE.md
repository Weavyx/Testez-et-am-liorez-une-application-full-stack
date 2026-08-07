# Audit — Défauts structurels mécaniques dans les tests (P4)

Audit **lecture seule**, aucun fichier modifié. Chaque fichier du périmètre a été lu intégralement (pas d'échantillonnage par grep) pour éviter les faux positifs/négatifs sur le corps de chaque test.

Périmètre déclaré :
- `front/src/app/**/*.spec.ts` — 13 fichiers
- `front/cypress/e2e/**/*.cy.ts` — 9 fichiers
- `back/src/test/java/**/*.java` — 17 fichiers

---

## A. Tests sans assertion réelle

**Périmètre scanné : 13 fichiers `.spec.ts` (tous les `it(...)`) + 17 fichiers `.java` (toutes les méthodes `@Test`/`@ParameterizedTest`).**

### Front (Jest)

0 occurrence. Tous les `it(...)` des 13 fichiers contiennent au moins un `expect(...)`. Aucun test avec assertion volontairement négative (`expect(x).not.toHaveBeenCalled()` type) n'a été trouvé isolé sans autre assertion — les cas de ce type observés (ex. `should_redirect a non-admin user to /sessions on init` dans `form.component.spec.ts:208-210`) comportent bien un `expect(...)`.

### Back (JUnit)

| Fichier | Ligne | Extrait | Avis |
|---|---|---|---|
| `back/src/test/java/com/openclassrooms/starterjwt/ApplicationContextIT.java` | 7-9 | `@Test void contextLoads() { }` | **Aucune assertion dans le corps** — le test réussit dès lors que le contexte Spring démarre sans exception levée pendant `@SpringBootTest`. C'est un **faux positif plausible** : `contextLoads()` est le pattern conventionnel Spring Boot pour vérifier que l'ApplicationContext se charge sans erreur de câblage (beans, configuration) ; l'assertion implicite est « pas d'exception au démarrage », détectée par le framework de test lui-même, pas par du code utilisateur. Ce n'est donc pas un test structurellement mort (il échoue bien si le contexte ne démarre pas), mais il ne contient techniquement aucun `assertThat`/`assertEquals`/`verify`. Signalé pour transparence, pas de correction recommandée. |

Toutes les autres méthodes `@Test` des 16 autres fichiers JUnit (`AuthControllerIT`, `SessionControllerIT`, `TeacherControllerIT`, `UserControllerIT`, `GlobalExceptionHandlerTest`, `SessionTest`, `TeacherTest`, `UserTest`, `AuthTokenFilterTest`, `JwtUtilsTest`, `UserDetailsImplTest`, `UserDetailsServiceImplTest`, `SessionServiceTest`, `TeacherServiceTest`, `UserServiceTest`) contiennent au moins un `.andExpect(...)`, `assertThat(...)`, `assertThatThrownBy(...)`, ou `verify(...)`.

**Total A : 1 occurrence (faux positif plausible — pattern Spring conventionnel), sur 13 fichiers Jest + 17 fichiers JUnit scannés.**

---

## B. `httpMock.verify()` manquant

**Périmètre scanné : 5 fichiers `.spec.ts` injectant `HttpTestingController`** (sur les 13 fichiers Jest du périmètre — les 8 autres n'importent pas `HttpTestingController`) :
`auth.service.spec.ts`, `user.service.spec.ts`, `pages/sessions/components/detail/detail.component.spec.ts`, `pages/sessions/components/form/form.component.spec.ts`, `pages/sessions/components/list/list.component.spec.ts`.

| Fichier | `httpMock.verify()` |
|---|---|
| `core/service/auth.service.spec.ts` | `afterEach(() => { httpMock.verify(); })` — ligne 48-50 ✅ |
| `core/service/user.service.spec.ts` | `afterEach(() => { httpMock.verify(); })` — ligne 55-57 ✅ |
| `pages/sessions/components/detail/detail.component.spec.ts` | 3 `describe` imbriqués (admin / non-admin non participant / non-admin participant), **chacun** avec son propre `afterEach(() => { httpMock.verify(); })` — lignes 97-99, 174-176, 256-258 ✅ |
| `pages/sessions/components/form/form.component.spec.ts` | 3 `describe` imbriqués (Create / Non-admin / Edit), **chacun** avec `afterEach(() => httpMock.verify())` — lignes 103, 191, 251 ✅ |
| `pages/sessions/components/list/list.component.spec.ts` | `afterEach(() => { httpMock.verify(); })` global — ligne 75-77 ✅ |

0 occurrence manquante.

**Total B : 0 occurrence, 5 fichiers scannés (sur 13 fichiers `.spec.ts` au total ; les 8 restants n'utilisent pas `HttpTestingController` et sont donc hors périmètre de cette catégorie).**

---

## C. Attentes hardcodées Cypress (`cy.wait(<nombre>)`)

**Périmètre scanné : 9 fichiers `.cy.ts`.**

Recherche exhaustive de tous les `cy.wait(...)` dans les 9 fichiers : `account.cy.ts`, `login.cy.ts`, `logout.cy.ts`, `register.cy.ts`, `sessions-create.cy.ts`, `sessions-delete.cy.ts`, `sessions-detail.cy.ts`, `sessions-list.cy.ts`, `sessions-update.cy.ts`.

Chaque occurrence de `cy.wait(...)` trouvée utilise systématiquement un alias nommé (`cy.wait('@xxxRequest')`), jamais un délai numérique fixe. Exemples représentatifs : `login.cy.ts:20` (`cy.wait('@loginRequest')`), `sessions-detail.cy.ts:208-209` (`cy.wait('@participateRequest')` puis `cy.wait('@sessionDetailRefreshRequest')`).

**Total C : 0 occurrence, 9 fichiers scannés.**

---

## D. Mocks Jest non nettoyés entre tests

**Périmètre scanné : 7 fichiers `.spec.ts` utilisant `jest.fn()`/`jest.spyOn(...)`/un objet mock partagé entre plusieurs `it(...)`** (sur les 13 fichiers Jest — les 6 autres, `session-api.service.spec.ts`, `session.service.spec.ts`, `teacher.service.spec.ts`, `user.service.spec.ts`, `auth.service.spec.ts`, `not-found.component.spec.ts`, n'utilisent aucun mock/spy Jest, ou aucun partagé entre tests) :
`app.component.spec.ts`, `components/me/me.component.spec.ts`, `pages/login/login.component.spec.ts`, `pages/register/register.component.spec.ts`, `pages/sessions/components/detail/detail.component.spec.ts`, `pages/sessions/components/form/form.component.spec.ts`, `pages/sessions/components/list/list.component.spec.ts`.

| Fichier | Mécanisme de nettoyage | Avis |
|---|---|---|
| `app.component.spec.ts` | `jest.spyOn(sessionService, 'logOut')` / `jest.spyOn(router, 'navigate')` créés localement à l'intérieur d'un seul `it(...)` (lignes 102-103), sur un composant/service recréés à chaque `beforeEach` via `TestBed.createComponent` | ✅ Pas de fuite possible : spy local à un seul test, instance fraîche à chaque test. |
| `components/me/me.component.spec.ts` | `mockSessionService.logOut = jest.fn()` et `mockMatSnackBar.open = jest.fn()` définis une fois au niveau `describe` (lignes 43, 47) et **réutilisés dans plusieurs `it(...)`** | ✅ `afterEach(() => { mockSessionService.logOut.mockClear(); mockMatSnackBar.open.mockClear(); })` — lignes 87-90. Nettoyage explicite couvrant les deux mocks partagés. |
| `pages/login/login.component.spec.ts` | Spies (`authService.login`, `sessionService.logIn`, `router.navigate`) créés localement dans chaque `it(...)`, jamais au niveau `describe` | ✅ Pas de mock partagé entre tests. |
| `pages/register/register.component.spec.ts` | Idem login : spies locaux par test | ✅ Pas de mock partagé entre tests. |
| `pages/sessions/components/detail/detail.component.spec.ts` | `jest.spyOn(router, 'navigate')` recréé dans le `beforeEach` de **chaque** `describe` (admin / non-admin x2), sur un `router` réinjecté via `TestBed.inject` à chaque fois | ✅ Spy recréé à chaque test (nouveau `TestBed`), pas d'accumulation d'appels entre tests. |
| `pages/sessions/components/form/form.component.spec.ts` | `mockRouter = { navigate: jest.fn() }` défini une fois par `describe` (Create/Non-admin/Edit) et réutilisé par les `it(...)` de ce bloc | ✅ Chaque `beforeEach` de chacun des 3 `describe` appelle `jest.clearAllMocks()` en première ligne (lignes 83, 172, 225) avant de recréer le `TestBed`. |
| `pages/sessions/components/list/list.component.spec.ts` | Pas de `jest.fn()` partagé entre `it(...)` ; chaque test appelle `setup(...)` qui reconfigure un `TestBed` frais | ✅ Pas de mock partagé nécessitant un reset. |

**Total D : 0 occurrence, 7 fichiers scannés (sur 13 fichiers `.spec.ts` au total).**

---

## E. Tests JUnit avec mock stubbé mais jamais exploité

**Périmètre scanné : 6 fichiers JUnit utilisant `@Mock`/`when(...).thenReturn(...)` (Mockito)** (sur les 17 fichiers JUnit — les 11 autres sont soit des tests d'intégration `@SpringBootTest`/MockMvc sans mock Mockito, soit des tests de modèle purs sans dépendance à mocker) :
`AuthTokenFilterTest`, `UserDetailsServiceImplTest`, `SessionServiceTest`, `TeacherServiceTest`, `UserServiceTest`. (`JwtUtilsTest` et `UserDetailsImplTest` n'utilisent aucun `@Mock`/Mockito — instanciation directe des classes testées — donc hors périmètre de cette catégorie malgré leur présence dans le dossier `security`.)

Vérification systématique, pour chaque `when(...).thenReturn(...)`, de sa présence soit dans une assertion sur le résultat, soit dans un `verify(...)` :

- **`AuthTokenFilterTest`** (3 tests) : chaque `when(...)` (sur `jwtUtils`, `userDetailsService`, `request`) conditionne directement le comportement vérifié par `verify(filterChain).doFilter(...)` et l'assertion sur `SecurityContextHolder`. ✅ Tous exploités.
- **`UserDetailsServiceImplTest`** (2 tests) : `when(userRepository.findByEmail(...))` → résultat casté et directement asserté (`assertThat(userDetails.getId())...`) ou l'exception est asserté via `assertThatThrownBy`. ✅ Tous exploités.
- **`TeacherServiceTest`** (4 tests) : chaque `when(teacherRepository....)` → résultat asserté (`assertThat(result)...`) et/ou `verify(teacherRepository)...`. ✅ Tous exploités.
- **`UserServiceTest`** (17 tests) : tous les stubs (`userRepository`, `sessionRepository`, `passwordEncoder`) sont soit repris dans le résultat asserté, soit capturés via `ArgumentCaptor` et assertés, soit vérifiés via `verify(...)`. ✅ Tous exploités.
- **`SessionServiceTest`** (17 tests) : idem, avec deux cas limites à signaler ci-dessous (non retenus comme défaut, mais documentés pour transparence).

| Fichier | Ligne | Extrait | Avis |
|---|---|---|---|
| `back/.../services/SessionServiceTest.java` | 84-89 | `when(sessionRepository.existsById(1L)).thenReturn(true); sessionService.delete(1L); verify(sessionRepository).deleteById(1L);` | **Faux positif plausible.** Le retour de `existsById(1L)` n'est ni asserté directement, ni vérifié via `verify(sessionRepository).existsById(1L)`. Mais ce stub pilote le flux de contrôle interne de `delete()` : sans lui `deleteById` ne serait jamais atteint. Le test vérifie bien le comportement résultant (`verify(sessionRepository).deleteById(1L)`), donc le stub n'est pas « mort » au sens fonctionnel — il est exploité indirectement, pas explicitement vérifié en tant qu'appel. Pas de correction recommandée. |
| `back/.../services/SessionServiceTest.java` | 139-150 | `when(sessionRepository.existsById(1L)).thenReturn(true); ... Session result = sessionService.update(1L, session); verify(sessionRepository).save(captor.capture());` | Même schéma que ci-dessus (`existsById` en garde de flux, jamais vérifié explicitement) — même avis : faux positif plausible, non retenu comme défaut. |

**Total E : 0 défaut réel confirmé ; 2 occurrences de stubs « garde de flux de contrôle » relevées par prudence mais classées faux positifs plausibles (le comportement qu'ils conditionnent est bien vérifié en aval), sur 6 fichiers JUnit utilisant Mockito scannés (sur 17 fichiers JUnit au total).**

---

## Résumé final

| Catégorie | Occurrences retenues | Faux positifs plausibles relevés | Périmètre scanné |
|---|---|---|---|
| A — Tests sans assertion | 0 défaut confirmé | 1 (`ApplicationContextIT.contextLoads()`, pattern Spring conventionnel) | 13 fichiers Jest + 17 fichiers JUnit |
| B — `httpMock.verify()` manquant | 0 | 0 | 5 fichiers `.spec.ts` avec `HttpTestingController` (sur 13) |
| C — `cy.wait(<ms>)` hardcodé | 0 | 0 | 9 fichiers `.cy.ts` |
| D — Mocks Jest non nettoyés | 0 | 0 | 7 fichiers `.spec.ts` avec mocks partagés (sur 13) |
| E — Mock stubbé jamais exploité | 0 défaut confirmé | 2 (stubs de garde de flux, comportement vérifié en aval) | 6 fichiers JUnit avec Mockito (sur 17) |

**Constat global : aucun défaut structurel mécanique confirmé sur les 5 catégories.** Les 3 signalements faits (1 en A, 2 en E) sont documentés par transparence mais correspondent à des patterns légitimes (smoke-test Spring conventionnel ; stub pilotant un flux de contrôle dont l'effet est vérifié via un `verify()` en aval), pas à des tests structurellement incapables d'échouer.
