# Audit Phase 3 — Passe 2 : classement unitaire / intégration réel

Application du **critère opérationnel de `METHODE_AUDIT.md` § Phase 3 / Passe 2** à chacun
des tests recensés en Passe 1 (`AUDIT_PHASE3_INVENTAIRE_TESTS.md`). Classification
uniquement : **aucun renommage, aucun déplacement, aucune correction**. Chaque test a été
vérifié dans son fichier source réel, au niveau de la méthode / du `it(...)`, pas seulement
au niveau de la classe ou du `describe`.

## Critère appliqué (rappel)

| Périmètre | Intégration | Unitaire |
|---|---|---|
| Back | contexte Spring démarré **ET** au moins deux couches réelles traversées (controller → service → repository/BDD réels) | tous les collaborateurs mockés (y compris `@WebMvcTest` + `@MockBean(Service)`) |
| Front | assertion sur le DOM après coordination réelle composant↔service↔HTTP, **ou** vérification via `HttpTestingController` | appel de méthode + assertion sur une propriété de classe, sans coordination multi-collaborateurs réelle |

## Convention retenue pour la colonne « Classement déclaré »

- **Back** : `*IT.java` → *Intégration (déclaré)* ; tout autre nom de fichier (`*Test.java`)
  → *Unitaire (déclaré)*.
- **Front** : le projet porte une convention explicite **dans les noms de `describe`** —
  `rendu (intégration DOM)` → *Intégration (déclaré)* ; `logique isolée (unitaire)` →
  *Unitaire (déclaré)*. Les fichiers de service sans ce regroupement (`auth.service`,
  `user.service`, `session.service`, `session-api.service`, `teacher.service`) n'ont pas de
  convention d'intégration → *Unitaire (déclaré)* par défaut, ce que confirme d'ailleurs leur
  en-tête de fichier.

> **Point structurant, à arbitrer** : les en-têtes de tous les fichiers front énoncent une
> règle *maison* explicitement plus stricte que `METHODE_AUDIT.md` — « UNITAIRE = tout le
> reste, **y compris les tests qui vérifient le contrat HTTP réel via `HttpTestingController`** ».
> `METHODE_AUDIT.md` range au contraire toute vérification via `HttpTestingController` en
> intégration. **Les 10 écarts front listés plus bas découlent tous et uniquement de ce
> conflit de conventions**, pas d'une erreur de câblage. L'arbitrage porte donc sur *quelle
> convention fait foi*, pas sur les tests eux-mêmes.

## Point de vigilance vérifié en priorité (piège `@WebMvcTest` + `@MockBean`)

Recherche exhaustive sur `back/src/test/java/**` des motifs `@MockBean`, `@MockitoBean`,
`@WebMvcTest`, `@DataJpaTest`, `Mockito.mock(`, `mock(`, `@SpyBean` : **aucune occurrence**.
Le piège classique du controller testé avec MockMvc mais service mocké **n'existe pas dans
ce projet** — aucun test nommé `*IT` n'est en réalité unitaire. Les quatre classes `*IT`
étendent toutes `AbstractIntegrationTest` (`@SpringBootTest(RANDOM_PORT)` + `@ActiveProfiles("test")`
+ conteneur MySQL Testcontainers singleton) et sont annotées `@AutoConfigureMockMvc` +
`@Transactional`, sans aucun bean substitué, méthode par méthode.

---

## 1. Tableau complet back

| Fichier | Nom du test | Classement déclaré | Classement réel | Écart | Justification classement réel |
|---|---|---|---|---|---|
| AbstractIntegrationTest.java | *(aucune méthode `@Test` — classe de base abstraite)* | Intégration (socle déclaré) | n/a — non comptabilisée | n/a | Classe abstraite portant `@SpringBootTest` + Testcontainers MySQL et les helpers JWT ; aucune méthode de test à classer. |
| ApplicationContextTest.java | contextLoads | Unitaire (déclaré) | **Intégration** | **OUI** | `extends AbstractIntegrationTest` : `@SpringBootTest` + conteneur MySQL réel démarré, aucun collaborateur mocké — intégration de profondeur nulle (aucune couche métier appelée), mais en rien un test unitaire. Nuance : `@Tag("integration")` est hérité, l'écart porte sur le nom de fichier `*Test`. |
| services/TeacherServiceTest.java | should_returnAllTeachers_when_findAllIsCalled | Unitaire (déclaré) | Unitaire | NON | `@ExtendWith(MockitoExtension)` + `@Mock TeacherRepository` : seul collaborateur mocké, aucun contexte Spring. |
| services/TeacherServiceTest.java | should_returnEmptyList_when_findAllIsCalled_and_noTeacherExists | Unitaire (déclaré) | Unitaire | NON | Idem : `@Mock TeacherRepository` stubbé, `@InjectMocks TeacherService`. |
| services/TeacherServiceTest.java | should_returnTeacher_when_findByIdIsCalled_and_teacherExists | Unitaire (déclaré) | Unitaire | NON | Idem : repository mocké, aucune BDD. |
| services/TeacherServiceTest.java | should_throwNotFoundException_when_findByIdIsCalled_and_teacherDoesNotExist | Unitaire (déclaré) | Unitaire | NON | Idem : `Optional.empty()` stubbé sur le mock, assertion sur l'exception. |
| services/UserServiceTest.java | should_deleteUser_when_deleteByIdIsCalled_and_requesterIsTheOwner | Unitaire (déclaré) | Unitaire | NON | `@Mock UserRepository/SessionRepository/PasswordEncoder`, `SecurityContextHolder` alimenté à la main : tous les collaborateurs mockés. |
| services/UserServiceTest.java | should_throwForbiddenException_when_deleteByIdIsCalled_and_requesterIsNotTheOwner | Unitaire (déclaré) | Unitaire | NON | Idem : autorisation évaluée sur un `SecurityContext` forgé, aucun contexte Spring. |
| services/UserServiceTest.java | should_throwForbiddenException_when_deleteByIdIsCalled_and_onlyTheEmailMatches | Unitaire (déclaré) | Unitaire | NON | Idem : principal forgé dans le test, repositories mockés. |
| services/UserServiceTest.java | should_removeUserFromParticipatedSessions_when_deleteByIdIsCalled | Unitaire (déclaré) | Unitaire | NON | Idem : `ArgumentCaptor<Session>` sur un `SessionRepository` mocké, aucune écriture réelle. |
| services/UserServiceTest.java | should_notTouchAnySession_when_deleteByIdIsCalled_and_userParticipatesInNone | Unitaire (déclaré) | Unitaire | NON | Idem : `verify(..., never())` sur mock. |
| services/UserServiceTest.java | should_throwNotFoundException_when_deleteByIdIsCalled_and_userDoesNotExist | Unitaire (déclaré) | Unitaire | NON | Idem : `findById` stubbé vide sur le mock. |
| services/UserServiceTest.java | should_returnUser_when_findByIdIsCalled_and_userExists | Unitaire (déclaré) | Unitaire | NON | Idem : `UserRepository` mocké. |
| services/UserServiceTest.java | should_throwNotFoundException_when_findByIdIsCalled_and_userDoesNotExist | Unitaire (déclaré) | Unitaire | NON | Idem : mock renvoyant `Optional.empty()`. |
| services/UserServiceTest.java | should_returnUser_when_findOwnProfileIsCalled_and_requesterIsTheOwner | Unitaire (déclaré) | Unitaire | NON | Idem : `authenticateAs()` forge le principal, repository mocké. |
| services/UserServiceTest.java | should_throwForbiddenException_when_findOwnProfileIsCalled_and_requesterIsNotTheOwner | Unitaire (déclaré) | Unitaire | NON | Idem. |
| services/UserServiceTest.java | should_throwNotFoundException_when_findOwnProfileIsCalled_and_userDoesNotExist | Unitaire (déclaré) | Unitaire | NON | Idem. |
| services/UserServiceTest.java | should_returnUser_when_findByIdIsCalled_and_requesterIsAnotherUser | Unitaire (déclaré) | Unitaire | NON | Idem. |
| services/UserServiceTest.java | should_returnUser_when_findByEmailIsCalled_and_userExists | Unitaire (déclaré) | Unitaire | NON | Idem : `findByEmail` stubbé sur mock. |
| services/UserServiceTest.java | should_returnEmptyOptional_when_findByEmailIsCalled_and_userDoesNotExist | Unitaire (déclaré) | Unitaire | NON | Idem. |
| services/UserServiceTest.java | should_returnTrue_when_existsByEmailIsCalled_and_emailIsTaken | Unitaire (déclaré) | Unitaire | NON | Idem : `existsByEmail` stubbé sur mock. |
| services/UserServiceTest.java | should_returnFalse_when_existsByEmailIsCalled_and_emailIsNotTaken | Unitaire (déclaré) | Unitaire | NON | Idem. |
| services/UserServiceTest.java | should_returnTrue_when_isAdminIsCalled_and_userIsAdmin | Unitaire (déclaré) | Unitaire | NON | Idem. |
| services/UserServiceTest.java | should_returnFalse_when_isAdminIsCalled_and_userIsNotAdmin | Unitaire (déclaré) | Unitaire | NON | Idem. |
| services/UserServiceTest.java | should_returnFalse_when_isAdminIsCalled_and_userDoesNotExist | Unitaire (déclaré) | Unitaire | NON | Idem. |
| services/UserServiceTest.java | should_saveUserWithEncodedPassword_when_registerIsCalled_and_emailIsNotTaken | Unitaire (déclaré) | Unitaire | NON | `PasswordEncoder` mocké et `UserRepository` mocké : aucun encodage ni écriture réels. |
| services/UserServiceTest.java | should_throwBadRequestException_when_registerIsCalled_and_emailIsAlreadyTaken | Unitaire (déclaré) | Unitaire | NON | Idem : `existsByEmail` stubbé `true` sur le mock. |
| services/SessionServiceTest.java | should_saveSession_when_createIsCalled | Unitaire (déclaré) | Unitaire | NON | `@Mock SessionRepository/UserRepository` + `@InjectMocks SessionService`, principal forgé dans `@BeforeEach`. |
| services/SessionServiceTest.java | should_deleteSession_when_deleteIsCalled_and_sessionExists | Unitaire (déclaré) | Unitaire | NON | Idem : `verify` sur repository mocké. |
| services/SessionServiceTest.java | should_throwNotFoundException_when_deleteIsCalled_and_sessionDoesNotExist | Unitaire (déclaré) | Unitaire | NON | Idem : `findById` stubbé vide. |
| services/SessionServiceTest.java | should_returnAllSessions_when_findAllIsCalled | Unitaire (déclaré) | Unitaire | NON | Idem : `findAll` stubbé sur mock. |
| services/SessionServiceTest.java | should_returnEmptyList_when_findAllIsCalled_and_noSessionExists | Unitaire (déclaré) | Unitaire | NON | Idem. |
| services/SessionServiceTest.java | should_returnSession_when_getByIdIsCalled_and_sessionExists | Unitaire (déclaré) | Unitaire | NON | Idem. |
| services/SessionServiceTest.java | should_returnNull_when_getByIdIsCalled_and_sessionDoesNotExist | Unitaire (déclaré) | Unitaire | NON | Idem. |
| services/SessionServiceTest.java | should_setIdFromParameter_when_updateIsCalled | Unitaire (déclaré) | Unitaire | NON | Idem : `ArgumentCaptor` + `thenAnswer` sur repository mocké. |
| services/SessionServiceTest.java | should_throwNotFoundException_when_updateIsCalled_and_sessionDoesNotExist | Unitaire (déclaré) | Unitaire | NON | Idem. |
| services/SessionServiceTest.java | should_addUserToSession_when_participateIsCalled_and_notAlreadyParticipating | Unitaire (déclaré) | Unitaire | NON | Idem : session et user proviennent de stubs, pas de la BDD. |
| services/SessionServiceTest.java | should_throwNotFoundException_when_participateIsCalled_and_sessionDoesNotExist | Unitaire (déclaré) | Unitaire | NON | Idem. |
| services/SessionServiceTest.java | should_throwNotFoundException_when_participateIsCalled_and_userDoesNotExist | Unitaire (déclaré) | Unitaire | NON | Idem. |
| services/SessionServiceTest.java | should_throwBadRequestException_when_participateIsCalled_and_userAlreadyParticipating | Unitaire (déclaré) | Unitaire | NON | Idem. |
| services/SessionServiceTest.java | should_throwForbiddenException_when_participateIsCalled_and_userIdDoesNotMatchAuthenticatedPrincipal | Unitaire (déclaré) | Unitaire | NON | Idem : `authenticateAs(999L)` forge le principal, aucun filtre Spring Security réel. |
| services/SessionServiceTest.java | should_removeUserFromSession_when_noLongerParticipateIsCalled_and_userIsParticipating | Unitaire (déclaré) | Unitaire | NON | Idem : repositories mockés. |
| services/SessionServiceTest.java | should_throwNotFoundException_when_noLongerParticipateIsCalled_and_sessionDoesNotExist | Unitaire (déclaré) | Unitaire | NON | Idem. |
| services/SessionServiceTest.java | should_throwNotFoundException_when_noLongerParticipateIsCalled_and_userDoesNotExist | Unitaire (déclaré) | Unitaire | NON | Idem. |
| services/SessionServiceTest.java | should_throwBadRequestException_when_noLongerParticipateIsCalled_and_userIsNotParticipating | Unitaire (déclaré) | Unitaire | NON | Idem. |
| services/SessionServiceTest.java | should_throwForbiddenException_when_noLongerParticipateIsCalled_and_userIdDoesNotMatchAuthenticatedPrincipal | Unitaire (déclaré) | Unitaire | NON | Idem : principal forgé à la main. |
| security/services/UserDetailsImplTest.java | should_returnRoleAdmin_when_adminIsTrue | Unitaire (déclaré) | Unitaire | NON | JUnit 5 pur, instanciation directe via `UserDetailsImpl.builder()` : aucun collaborateur. |
| security/services/UserDetailsImplTest.java | should_returnRoleUser_when_adminIsFalse | Unitaire (déclaré) | Unitaire | NON | Idem. |
| security/services/UserDetailsImplTest.java | should_returnRoleUser_when_adminIsNull | Unitaire (déclaré) | Unitaire | NON | Idem. |
| security/services/UserDetailsImplTest.java | should_returnTrue_forAllAccountStatusFlags | Unitaire (déclaré) | Unitaire | NON | Idem : lecture de flags sur un objet construit dans le test. |
| security/services/UserDetailsImplTest.java | should_returnTrue_when_equalsIsCalledWithSameInstance | Unitaire (déclaré) | Unitaire | NON | Idem. |
| security/services/UserDetailsImplTest.java | should_returnFalse_when_equalsIsCalledWithNull | Unitaire (déclaré) | Unitaire | NON | Idem. |
| security/services/UserDetailsImplTest.java | should_returnFalse_when_equalsIsCalledWithDifferentClass | Unitaire (déclaré) | Unitaire | NON | Idem. |
| security/services/UserDetailsImplTest.java | should_returnTrue_when_equalsIsCalledWithSameId | Unitaire (déclaré) | Unitaire | NON | Idem. |
| security/services/UserDetailsImplTest.java | should_returnFalse_when_equalsIsCalledWithDifferentId | Unitaire (déclaré) | Unitaire | NON | Idem. |
| security/services/UserDetailsServiceImplTest.java | should_returnUserDetails_when_loadUserByUsernameIsCalled_and_userExists | Unitaire (déclaré) | Unitaire | NON | `@Mock UserRepository` + `@InjectMocks` : unique collaborateur mocké, aucun contexte Spring. |
| security/services/UserDetailsServiceImplTest.java | should_throwUsernameNotFoundException_when_loadUserByUsernameIsCalled_and_userDoesNotExist | Unitaire (déclaré) | Unitaire | NON | Idem : `findByEmail` stubbé vide. |
| security/jwt/AuthTokenFilterTest.java | should_logErrorAndContinueChain_when_userDetailsServiceThrows | Unitaire (déclaré) | Unitaire | NON | `JwtUtils`, `UserDetailsServiceImpl`, `HttpServletRequest/Response` et `FilterChain` tous `@Mock` ; `doFilterInternal` appelé directement, hors chaîne de filtres réelle. |
| security/jwt/JwtUtilsTest.java | should_generateValidDecodableToken_when_generateJwtTokenIsCalled | Unitaire (déclaré) | Unitaire | NON | JUnit 5 pur, `new JwtUtils()` + `ReflectionTestUtils.setField` : aucun contexte Spring, aucune couche applicative en aval (seule la bibliothèque JJWT est réelle). |
| security/jwt/JwtUtilsTest.java | should_extractSubject_when_getUserNameFromJwtTokenIsCalled | Unitaire (déclaré) | Unitaire | NON | Idem. |
| security/jwt/JwtUtilsTest.java | should_returnTrue_when_validateJwtTokenIsCalled_with_validToken | Unitaire (déclaré) | Unitaire | NON | Idem. |
| security/jwt/JwtUtilsTest.java | should_returnFalse_when_validateJwtTokenIsCalled_with_invalidSignature | Unitaire (déclaré) | Unitaire | NON | Idem : token forgé dans le test avec un autre secret. |
| security/jwt/JwtUtilsTest.java | should_returnFalse_when_validateJwtTokenIsCalled_with_malformedToken | Unitaire (déclaré) | Unitaire | NON | Idem : chaîne littérale non-JWT. |
| security/jwt/JwtUtilsTest.java | should_returnFalse_when_validateJwtTokenIsCalled_with_expiredToken | Unitaire (déclaré) | Unitaire | NON | Idem : token expiré forgé localement. |
| security/jwt/JwtUtilsTest.java | should_returnFalse_when_validateJwtTokenIsCalled_with_unsupportedToken | Unitaire (déclaré) | Unitaire | NON | Idem : token non signé forgé localement. |
| security/jwt/JwtUtilsTest.java | should_returnFalse_when_validateJwtTokenIsCalled_with_nullOrEmptyToken | Unitaire (déclaré) | Unitaire | NON | Idem : entrées littérales, aucun collaborateur. |
| controllers/AuthControllerIT.java | register_returns200AndSuccessMessage_whenDataIsValid | Intégration (déclaré) | Intégration | NON | Contexte Spring + MySQL Testcontainers ; MockMvc → AuthController → UserRepository/PasswordEncoder réels, relecture en base pour vérifier le hash. |
| controllers/AuthControllerIT.java | register_returns400AndMessage_whenEmailIsAlreadyTaken | Intégration (déclaré) | Intégration | NON | Idem : l'utilisateur préexistant est persisté via le vrai `UserRepository`, le doublon est détecté en base. |
| controllers/AuthControllerIT.java | register_returns400_whenEmailIsBlank | Intégration (déclaré) | Intégration | NON | Contexte Spring réel, aucun mock ; rejet par la Bean Validation réelle avant le corps du controller (intégration de profondeur limitée). |
| controllers/AuthControllerIT.java | register_returns400_whenEmailFormatIsInvalid | Intégration (déclaré) | Intégration | NON | Idem : `@Valid` réel, service non atteint. |
| controllers/AuthControllerIT.java | register_returns400_whenPasswordIsTooShort | Intégration (déclaré) | Intégration | NON | Idem : `@Valid` réel, service non atteint. |
| controllers/AuthControllerIT.java | login_returns200AndJwtResponse_whenCredentialsAreValid | Intégration (déclaré) | Intégration | NON | Chaîne complète réelle : controller → `AuthenticationManager` → `UserDetailsServiceImpl` → `UserRepository` → MySQL, puis validation cryptographique par le `JwtUtils` du contexte. |
| controllers/AuthControllerIT.java | login_returns401_whenPasswordIsWrong | Intégration (déclaré) | Intégration | NON | Idem : utilisateur réellement persisté, échec produit par le vrai `PasswordEncoder`/`AuthenticationManager`. |
| controllers/AuthControllerIT.java | login_returns401_whenEmailIsUnknown | Intégration (déclaré) | Intégration | NON | Idem : `UserDetailsServiceImpl` interroge réellement MySQL et ne trouve rien. |
| controllers/AuthControllerIT.java | login_returns400_whenPasswordIsBlank | Intégration (déclaré) | Intégration | NON | Contexte réel + utilisateur persisté ; rejet par `@NotBlank` réel avant authentification. |
| controllers/TeacherControllerIT.java | findById_returns200AndTeacher_whenTeacherExists | Intégration (déclaré) | Intégration | NON | MockMvc → controller → `TeacherService` → `TeacherRepository` → MySQL réels, JWT réel validé par `AuthTokenFilter`. |
| controllers/TeacherControllerIT.java | findById_returns404_whenTeacherDoesNotExist | Intégration (déclaré) | Intégration | NON | Idem : le 404 vient d'une vraie lecture en base sur un id absent. |
| controllers/TeacherControllerIT.java | findById_returns400_whenIdIsNotNumeric | Intégration (déclaré) | Intégration | NON | Authentification réelle (filtre → `UserDetailsServiceImpl` → `UserRepository` → MySQL) puis 400 levé par `Long.parseLong` dans le controller réel ; service métier non atteint. |
| controllers/TeacherControllerIT.java | findById_returns401_whenNotAuthenticated | Intégration (déclaré) | Intégration | NON | Contexte Spring réel, aucun mock ; requête rejetée par la vraie chaîne de filtres/entry point — intégration de profondeur limitée. |
| controllers/TeacherControllerIT.java | findAll_returns200AndAllTeachers_whenAuthenticated | Intégration (déclaré) | Intégration | NON | Enseignants persistés via le vrai repository puis relus de bout en bout par la requête MockMvc. |
| controllers/TeacherControllerIT.java | findAll_returns401_whenNotAuthenticated | Intégration (déclaré) | Intégration | NON | Idem 401 : chaîne de sécurité réelle, aucun bean substitué. |
| controllers/SessionControllerIT.java | findById_returns200AndSession_whenSessionExists | Intégration (déclaré) | Intégration | NON | Teacher et Session persistés via les vrais repositories, relus via controller → `SessionService` → `SessionRepository` → MySQL. |
| controllers/SessionControllerIT.java | findById_returns404_whenSessionDoesNotExist | Intégration (déclaré) | Intégration | NON | 404 issu d'une vraie lecture en base sur id absent, JWT réel. |
| controllers/SessionControllerIT.java | findById_returns400_whenIdIsNotNumeric | Intégration (déclaré) | Intégration | NON | Authentification réelle en base, puis `Long.parseLong` → `GlobalExceptionHandler` réels ; service non atteint. |
| controllers/SessionControllerIT.java | findById_returns401_whenNotAuthenticated | Intégration (déclaré) | Intégration | NON | Données persistées via vrais repositories ; requête rejetée par la chaîne de filtres réelle — profondeur limitée côté requête. |
| controllers/SessionControllerIT.java | findAll_returns200AndAllSessions_whenAuthenticated | Intégration (déclaré) | Intégration | NON | Deux sessions réellement persistées puis comptées dans la réponse JSON. |
| controllers/SessionControllerIT.java | findAll_returns401_whenNotAuthenticated | Intégration (déclaré) | Intégration | NON | Chaîne de sécurité réelle, aucun mock ; service non atteint. |
| controllers/SessionControllerIT.java | create_returns200_whenCalledByAdmin | Intégration (déclaré) | Intégration | NON | JWT admin réel, `SessionMapper` + `SessionService` + `SessionRepository` réels, écriture effective en MySQL. |
| controllers/SessionControllerIT.java | create_returns403_whenCalledByNonAdmin | Intégration (déclaré) | Intégration | NON | Utilisateur non-admin réellement persisté ; refus par la config Spring Security réelle après authentification en base. |
| controllers/SessionControllerIT.java | create_returns400_whenNameIsMissing | Intégration (déclaré) | Intégration | NON | Authentification admin réelle, puis rejet par `@Valid` réel avant le service. |
| controllers/SessionControllerIT.java | create_returns401_whenNotAuthenticated | Intégration (déclaré) | Intégration | NON | Chaîne de filtres réelle, aucun bean substitué. |
| controllers/SessionControllerIT.java | update_returns200_whenCalledByAdmin | Intégration (déclaré) | Intégration | NON | Session persistée puis modifiée de bout en bout via controller → service → repository → MySQL. |
| controllers/SessionControllerIT.java | update_returns403_whenCalledByNonAdmin | Intégration (déclaré) | Intégration | NON | Refus par `hasRole("ADMIN")` réel après authentification via `UserDetailsServiceImpl` → MySQL. |
| controllers/SessionControllerIT.java | update_returns400_whenIdIsNotNumeric | Intégration (déclaré) | Intégration | NON | Authentification admin réelle puis `NumberFormatException` → `GlobalExceptionHandler` réels. |
| controllers/SessionControllerIT.java | update_returns403NotBadRequest_whenCalledByNonAdminWithInvalidId | Intégration (déclaré) | Intégration | NON | Vérifie l'ordre réel des filtres Spring Security (403 avant parsing) — comportement observable uniquement avec le contexte réel. |
| controllers/SessionControllerIT.java | update_returns404_whenSessionDoesNotExist | Intégration (déclaré) | Intégration | NON | 404 issu d'une vraie lecture en base sur id absent. |
| controllers/SessionControllerIT.java | update_returns400_whenValidationFails | Intégration (déclaré) | Intégration | NON | Session réellement persistée, rejet par `@Valid` réel. |
| controllers/SessionControllerIT.java | update_returns401_whenNotAuthenticated | Intégration (déclaré) | Intégration | NON | Chaîne de sécurité réelle ; service non atteint. |
| controllers/SessionControllerIT.java | delete_returns200_whenCalledByAdmin | Intégration (déclaré) | Intégration | NON | Suppression effective en MySQL via controller → service → repository réels. |
| controllers/SessionControllerIT.java | delete_returns403_whenCalledByNonAdmin | Intégration (déclaré) | Intégration | NON | Refus par la config de sécurité réelle après authentification en base. |
| controllers/SessionControllerIT.java | delete_returns400_whenIdIsNotNumeric | Intégration (déclaré) | Intégration | NON | Authentification réelle puis parsing controller réel ; service non atteint. |
| controllers/SessionControllerIT.java | delete_returns404_whenSessionDoesNotExist | Intégration (déclaré) | Intégration | NON | 404 issu d'une vraie lecture en base. |
| controllers/SessionControllerIT.java | delete_returns401_whenNotAuthenticated | Intégration (déclaré) | Intégration | NON | Chaîne de filtres réelle, aucun mock. |
| controllers/SessionControllerIT.java | participate_returns200_whenNonAdminParticipatesForThemselves | Intégration (déclaré) | Intégration | NON | Participant et session persistés réellement ; jointure `PARTICIPATE` écrite en MySQL via service + repository réels. |
| controllers/SessionControllerIT.java | participate_returns403_whenUserIdDoesNotMatchAuthenticatedPrincipal | Intégration (déclaré) | Intégration | NON | Deux utilisateurs distincts réellement persistés ; contrôle « self » évalué dans le service réel. |
| controllers/SessionControllerIT.java | participate_returns400_whenIdIsNotNumeric | Intégration (déclaré) | Intégration | NON | Authentification réelle puis parsing controller réel. |
| controllers/SessionControllerIT.java | participate_returns404_whenSessionDoesNotExist | Intégration (déclaré) | Intégration | NON | 404 issu d'une vraie lecture en base. |
| controllers/SessionControllerIT.java | participate_returns404_whenUserDoesNotExist | Intégration (déclaré) | Intégration | NON | Idem : `UserRepository` réel interrogé sur un id absent. |
| controllers/SessionControllerIT.java | participate_returns400_whenAlreadyParticipating | Intégration (déclaré) | Intégration | NON | État de participation préparé via `sessionRepository.save()` réel, doublon détecté par le service réel. |
| controllers/SessionControllerIT.java | noLongerParticipate_returns200_whenNonAdminParticipatesForThemselves | Intégration (déclaré) | Intégration | NON | Participation préparée en base puis retirée de bout en bout via controller → service → repository → MySQL. |
| controllers/SessionControllerIT.java | noLongerParticipate_returns403_whenUserIdDoesNotMatchAuthenticatedPrincipal | Intégration (déclaré) | Intégration | NON | Deux comptes réels distincts, contrôle « self » du service réel. |
| controllers/SessionControllerIT.java | noLongerParticipate_returns400_whenIdIsNotNumeric | Intégration (déclaré) | Intégration | NON | Authentification réelle puis parsing controller réel. |
| controllers/SessionControllerIT.java | noLongerParticipate_returns404_whenSessionDoesNotExist | Intégration (déclaré) | Intégration | NON | 404 issu d'une vraie lecture en base. |
| controllers/SessionControllerIT.java | noLongerParticipate_returns404_whenUserDoesNotExist | Intégration (déclaré) | Intégration | NON | Idem côté `UserRepository` réel. |
| controllers/SessionControllerIT.java | noLongerParticipate_returns400_whenNotParticipating | Intégration (déclaré) | Intégration | NON | Session réellement persistée sans participant, cas détecté par le service réel. |
| controllers/UserControllerIT.java | findById_returns200AndUserDto_whenUserReadsOwnAccount | Intégration (déclaré) | Intégration | NON | Utilisateur persisté via vrai repository, relu via controller → `UserService` → `UserRepository` → MySQL ; `$.password` vérifié absent du DTO sérialisé. |
| controllers/UserControllerIT.java | findById_returns403_whenUserReadsAnotherUsersAccount | Intégration (déclaré) | Intégration | NON | Deux utilisateurs réellement persistés, autorisation « self » évaluée dans le service réel. |
| controllers/UserControllerIT.java | findById_returns404_whenUserDoesNotExist | Intégration (déclaré) | Intégration | NON | 404 issu d'une vraie lecture en base sur id absent. |
| controllers/UserControllerIT.java | findById_returns400_whenIdIsNotNumeric | Intégration (déclaré) | Intégration | NON | Authentification réelle en base puis parsing controller réel ; service non atteint. |
| controllers/UserControllerIT.java | findById_returns401_whenNotAuthenticated | Intégration (déclaré) | Intégration | NON | Chaîne de filtres réelle, aucun mock — profondeur limitée. |
| controllers/UserControllerIT.java | delete_returns200_whenUserDeletesOwnAccount | Intégration (déclaré) | Intégration | NON | Suppression effective en MySQL, vérifiée par relecture via `UserRepository` réel. |
| controllers/UserControllerIT.java | delete_returns200AndClearsParticipations_whenUserDeletesOwnAccountWhileEnrolledInASession | Intégration (déclaré) | Intégration | NON | `EntityManager.flush()/clear()` réels puis relecture `UserRepository`/`SessionRepository` : effet de bord en base réellement observé. |
| controllers/UserControllerIT.java | delete_returns403_whenUserTriesToDeleteAnotherUsersAccount | Intégration (déclaré) | Intégration | NON | Deux comptes réels distincts ; refus par le contrôle « self » du service réel, non-suppression vérifiée en base. |
| controllers/UserControllerIT.java | delete_returns404_whenUserDoesNotExist | Intégration (déclaré) | Intégration | NON | 404 issu d'une vraie lecture en base. |
| controllers/UserControllerIT.java | delete_returns400_whenIdIsNotNumeric | Intégration (déclaré) | Intégration | NON | Authentification réelle puis parsing controller réel. |
| controllers/UserControllerIT.java | delete_returns401_whenNotAuthenticated | Intégration (déclaré) | Intégration | NON | Chaîne de filtres réelle, aucun bean substitué. |

**126 lignes** : 125 méthodes `@Test` classées + la ligne `AbstractIntegrationTest` (classe de
base sans `@Test`, non comptabilisée dans les ratios).

---

## 2. Tableau complet front

| Fichier | Nom du test | Classement déclaré | Classement réel | Écart | Justification classement réel |
|---|---|---|---|---|---|
| app.component.spec.ts | should create the app | Unitaire (déclaré) | Unitaire | NON | `TestBed.createComponent` + assertion de véracité sur l'instance ; aucun DOM lu, aucun HTTP. |
| app.component.spec.ts | should reflect the session state through $isLogged() (false, then true after logIn) | Unitaire (déclaré) | Unitaire | NON | `SessionService` réel mais aucune requête HTTP ni `detectChanges()` : assertion sur la valeur émise, pas sur le DOM. |
| app.component.spec.ts | should display Login/Register links (not Sessions/Account/Logout) when not logged in | Intégration (déclaré) | Intégration | NON | `detectChanges()` puis assertions sur `querySelectorAll('.link')` : DOM réellement rendu. |
| app.component.spec.ts | should log out and navigate to "/" when the Logout link is clicked | Intégration (déclaré) | Intégration | NON | Clic DOM réel (`logoutLink.click()`) déclenchant la coordination template → `SessionService` → `Router`. |
| me.component.spec.ts | should fetch and display the user information in the DOM | Intégration (déclaré) | Intégration | NON | Assertion sur `fixture.nativeElement.textContent` après `ngOnInit` → `UserService` ; **réserve** : la couche HTTP est court-circuitée par `jest.spyOn(userService,'getById')`. |
| me.component.spec.ts | should show "You are admin" and hide the Delete account button when the displayed user is admin | Intégration (déclaré) | Intégration | NON | Idem : DOM rendu lu via `querySelectorAll('button')`, `getById` stubbé (pas de round-trip HTTP réel). |
| me.component.spec.ts | should create | Unitaire (déclaré) | Unitaire | NON | `detectChanges()` puis assertion sur l'instance seule, aucune lecture DOM. |
| me.component.spec.ts | should call window.history.back on back() | Unitaire (déclaré) | Unitaire | NON | Appel direct `component.back()` + assertion sur un espion `window.history.back`. |
| me.component.spec.ts | should delete the account, notify the user and navigate on delete | Unitaire (déclaré) | Unitaire | NON | Appel direct `component.delete()` ; `userService.delete`, `MatSnackBar.open`, `logOut` et `router.navigate` tous mockés, aucun DOM ni `HttpTestingController`. |
| core/service/auth.service.spec.ts | should be created | Unitaire (déclaré) | Unitaire | NON | `TestBed.inject` + assertion de véracité ; aucune requête vérifiée dans le corps du test. |
| core/service/auth.service.spec.ts | should send a POST request to api/auth/login with the credentials | Unitaire (déclaré) | **Intégration** | **OUI** | `httpMock.expectOne('/api/auth/login')` + assertions sur `method`/`body` + `flush` : vérification via `HttpTestingController`, rangée en intégration par `METHODE_AUDIT.md`. |
| core/service/auth.service.spec.ts | should send a POST request to api/auth/register with the registration data | Unitaire (déclaré) | **Intégration** | **OUI** | Idem : `httpMock.expectOne('/api/auth/register')` + `flush(null)`. |
| core/service/session-api.service.spec.ts | should be created | Unitaire (déclaré) | Unitaire | NON | `TestBed.inject(SessionApiService)` + véracité ; ni `HttpTestingController` ni DOM. |
| core/service/session.service.spec.ts | should be created | Unitaire (déclaré) | Unitaire | NON | `TestBed.configureTestingModule({})` vide, aucun collaborateur. |
| core/service/session.service.spec.ts | should initialize as not logged in | Unitaire (déclaré) | Unitaire | NON | Lecture directe des propriétés `isLogged`/`sessionInformation`. |
| core/service/session.service.spec.ts | should emit false initially | Unitaire (déclaré) | Unitaire | NON | Souscription directe à l'observable du service, aucun autre collaborateur. |
| core/service/session.service.spec.ts | should emit true after logIn | Unitaire (déclaré) | Unitaire | NON | Appel direct `service.logIn()` + assertion sur la valeur émise. |
| core/service/session.service.spec.ts | should emit false after logOut | Unitaire (déclaré) | Unitaire | NON | Appels directs `logIn`/`logOut` + assertion sur la valeur émise. |
| core/service/session.service.spec.ts | should store the user in sessionInformation | Unitaire (déclaré) | Unitaire | NON | Appel de méthode + assertion sur une propriété de classe. |
| core/service/session.service.spec.ts | should set isLogged to true | Unitaire (déclaré) | Unitaire | NON | Idem. |
| core/service/session.service.spec.ts | should clear sessionInformation | Unitaire (déclaré) | Unitaire | NON | Idem. |
| core/service/session.service.spec.ts | should set isLogged to false | Unitaire (déclaré) | Unitaire | NON | Idem. |
| core/service/teacher.service.spec.ts | should be created | Unitaire (déclaré) | Unitaire | NON | `TestBed.inject(TeacherService)` + véracité ; aucun `HttpTestingController`. |
| core/service/user.service.spec.ts | should be created | Unitaire (déclaré) | Unitaire | NON | `TestBed.inject` + véracité ; aucune requête vérifiée dans le corps du test. |
| core/service/user.service.spec.ts | should send a GET request to api/user/:id and return the user | Unitaire (déclaré) | **Intégration** | **OUI** | `httpMock.expectOne('api/user/1')` + assertion sur `method` + `flush(mockUser)` : vérification via `HttpTestingController`. |
| core/service/user.service.spec.ts | should send a DELETE request to api/user/:id | Unitaire (déclaré) | **Intégration** | **OUI** | Idem : `httpMock.expectOne('api/user/1')` + `flush(null)`. |
| pages/login/login.component.spec.ts | should set onError to true and display an error message when login fails | Intégration (déclaré) | Intégration | NON | `component.submit()` puis `detectChanges()` et assertion sur `querySelector('.error')` : DOM réellement rendu (`authService.login` stubbé via `throwError`). |
| pages/login/login.component.spec.ts | should disable the submit button when a required field is missing | Intégration (déclaré) | Intégration | NON | Assertion sur `button.disabled` lu dans le DOM après propagation du form réactif au template. |
| pages/login/login.component.spec.ts | should create | Unitaire (déclaré) | Unitaire | NON | Assertion sur l'instance seule, aucune lecture DOM. |
| pages/login/login.component.spec.ts | should log in and navigate to /sessions on successful submit | Unitaire (déclaré) | Unitaire | NON | Appel direct `component.submit()` ; `authService.login`, `sessionService.logIn` et `router.navigate` espionnés, aucun DOM ni HTTP. |
| pages/not-found/not-found.component.spec.ts | should create | Unitaire (déclaré) | Unitaire | NON | Assertion sur l'instance seule ; aucun collaborateur, aucune lecture DOM. |
| pages/register/register.component.spec.ts | should set onError to true when registration fails | Intégration (déclaré) | Intégration | NON | `submit()` puis `detectChanges()` et assertion sur `querySelector('.error')` : DOM réellement rendu. |
| pages/register/register.component.spec.ts | should disable the submit button when a required field is missing | Intégration (déclaré) | Intégration | NON | Assertion sur `button.disabled` lu dans le DOM après mise à jour du form réactif. |
| pages/register/register.component.spec.ts | should create | Unitaire (déclaré) | Unitaire | NON | Assertion sur l'instance seule. |
| pages/register/register.component.spec.ts | should register and navigate to /login on successful submit | Unitaire (déclaré) | Unitaire | NON | Appel direct `component.submit()` + assertions sur espions (`register`, `navigate`), aucun DOM. |
| sessions/components/detail/detail.component.spec.ts | should display the session name, description, teacher and date | Intégration (déclaré) | Intégration | NON | DOM lu (`h1`, `.description`, `mat-card-subtitle`) après round-trip `HttpTestingController` réel sur `api/session/1` + `api/teacher/1`. |
| sessions/components/detail/detail.component.spec.ts | should show the Delete button for an admin | Intégration (déclaré) | Intégration | NON | Idem : DOM rendu à partir des réponses HTTP flushées dans le `beforeEach`. |
| sessions/components/detail/detail.component.spec.ts | should not show the Participate/UnParticipate buttons for an admin | Intégration (déclaré) | Intégration | NON | Idem : `querySelectorAll('button')` sur le DOM issu du flux HTTP réel. |
| sessions/components/detail/detail.component.spec.ts | should create | Unitaire (déclaré) | Unitaire | NON | Le corps du test se limite à `expect(component).toBeTruthy()` ; le round-trip HTTP a lieu dans le `beforeEach`, pas dans l'assertion. |
| sessions/components/detail/detail.component.spec.ts | should navigate back in browser history when back() is called | Unitaire (déclaré) | Unitaire | NON | Appel direct `component.back()` + espion sur `window.history.back`. |
| sessions/components/detail/detail.component.spec.ts | should delete the session, notify the user and navigate to /sessions | Unitaire (déclaré) | **Intégration** | **OUI** | Le corps du test fait `httpMock.expectOne({url:'api/session/1', method:'DELETE'})` + `flush` : vérification via `HttpTestingController`. |
| sessions/components/detail/detail.component.spec.ts | should not show the Delete button for a non-admin | Intégration (déclaré) | Intégration | NON | DOM lu après flush réel de `api/session/1` (`users:[2]`) et `api/teacher/1`. |
| sessions/components/detail/detail.component.spec.ts | should show the Participate button when the user has not joined | Intégration (déclaré) | Intégration | NON | Idem : DOM + état interne dérivés de la réponse HTTP flushée. |
| sessions/components/detail/detail.component.spec.ts | should toggle the DOM to "Do not participate" and update the attendee count when the Participate button is clicked | Intégration (déclaré) | Intégration | NON | Clic DOM réel → POST `api/session/1/participate/1` via `HttpTestingController` → re-flush GET → relecture du DOM : coordination complète. |
| sessions/components/detail/detail.component.spec.ts | should call the participate API with the session and user id, then reload the session | Unitaire (déclaré) | **Intégration** | **OUI** | Le corps enchaîne `httpMock.expectOne` POST puis re-flush des GET session/teacher : vérification via `HttpTestingController`. |
| sessions/components/detail/detail.component.spec.ts | should show the UnParticipate button when the user has already joined | Intégration (déclaré) | Intégration | NON | DOM lu après flush réel d'une session `users:[1]`. |
| sessions/components/detail/detail.component.spec.ts | should toggle the DOM to "Participate" and update the attendee count when the Do not participate button is clicked | Intégration (déclaré) | Intégration | NON | Clic DOM réel → DELETE via `HttpTestingController` → re-flush GET → relecture du DOM. |
| sessions/components/detail/detail.component.spec.ts | should call the unParticipate API with the session and user id, then reload the session | Unitaire (déclaré) | **Intégration** | **OUI** | Le corps fait `httpMock.expectOne` DELETE + re-flush des GET : vérification via `HttpTestingController`. |
| sessions/components/form/form.component.spec.ts | should disable the submit button when the form is invalid | Intégration (déclaré) | Intégration | NON | Assertion sur `button.disabled` lu dans le DOM après rendu du form réactif. |
| sessions/components/form/form.component.spec.ts | should enable the submit button when the form is valid | Intégration (déclaré) | Intégration | NON | `setValue` puis `detectChanges()` et lecture DOM du bouton submit. |
| sessions/components/form/form.component.spec.ts | should create | Unitaire (déclaré) | Unitaire | NON | Assertion sur l'instance seule ; le flush `api/teacher` a lieu dans le `beforeEach`. |
| sessions/components/form/form.component.spec.ts | should be in create mode (onUpdate = false) | Unitaire (déclaré) | Unitaire | NON | Assertion sur la propriété de classe `component.onUpdate`. |
| sessions/components/form/form.component.spec.ts | should initialize an empty form in create mode | Unitaire (déclaré) | Unitaire | NON | Lecture des contrôles du form réactif (état de classe), aucun DOM ni requête vérifiée. |
| sessions/components/form/form.component.spec.ts | should call the create API and navigate to sessions on submit | Unitaire (déclaré) | **Intégration** | **OUI** | Le corps fait `httpMock.expectOne('api/session')` + assertion sur `req.request.method` + `flush` : vérification via `HttpTestingController`. |
| sessions/components/form/form.component.spec.ts | should still render the form fields in the DOM even though a non-admin redirect was triggered | Intégration (déclaré) | Intégration | NON | Assertions sur `input[formControlName="name"]` et `textarea` réellement présents dans le DOM. |
| sessions/components/form/form.component.spec.ts | should redirect a non-admin user to /sessions on init | Unitaire (déclaré) | Unitaire | NON | Unique assertion sur un `Router` entièrement mocké (`jest.fn()`), aucun DOM ni requête vérifiée dans le corps. |
| sessions/components/form/form.component.spec.ts | should disable the submit button when a required field is missing | Intégration (déclaré) | Intégration | NON | Assertion sur `button.disabled` lu dans le DOM, après pré-remplissage issu du flush `api/session/1`. |
| sessions/components/form/form.component.spec.ts | should be in edit mode (onUpdate = true) | Unitaire (déclaré) | Unitaire | NON | Assertion sur la propriété de classe `component.onUpdate`. |
| sessions/components/form/form.component.spec.ts | should pre-fill the form with the existing session data | Unitaire (déclaré) | Unitaire | NON | Lecture des contrôles du form réactif ; le round-trip HTTP a lieu dans le `beforeEach`, pas dans le corps. |
| sessions/components/form/form.component.spec.ts | should call the update API and navigate to sessions on submit | Unitaire (déclaré) | **Intégration** | **OUI** | Le corps fait `httpMock.expectOne('api/session/1')` + assertion `PUT` + `flush` : vérification via `HttpTestingController`. |
| sessions/components/list/list.component.spec.ts | should fetch and display the sessions returned by the API in the DOM | Intégration (déclaré) | Intégration | NON | `httpMock.expectOne('api/session')` + `flush` dans le corps, puis assertion sur `nativeElement.textContent` : DOM + HTTP. |
| sessions/components/list/list.component.spec.ts | should display the "Create" button when the user is admin | Intégration (déclaré) | Intégration | NON | Idem : flush HTTP dans le corps puis lecture du DOM rendu. |
| sessions/components/list/list.component.spec.ts | should not display the "Create" button when the user is not admin | Intégration (déclaré) | Intégration | NON | Idem avec un `SessionService` mocké non-admin. |
| sessions/components/list/list.component.spec.ts | should display the Detail button even for a non-admin user (intentional: Detail also drives the participate/unparticipate flow) | Intégration (déclaré) | Intégration | NON | Idem : `querySelectorAll('button')` sur le DOM issu du flush `api/session`. |
| sessions/components/list/list.component.spec.ts | should create | Unitaire (déclaré) | **Intégration** | **OUI** | Le corps fait `httpMock.expectOne('api/session')` + `flush([])` avant d'asserter : vérification via `HttpTestingController`. |

**65 lignes** — l'intégralité des `it(...)` recensés en Passe 1.

---

## 3. Synthèse

### 3.1 Volumes et écarts

| Périmètre | Total tests | Tests en écart | % en écart |
|---|---|---|---|
| Back | 125 | 1 | **0,8 %** |
| Front | 65 | 10 | **15,4 %** |
| **Total** | **190** | **11** | **5,8 %** |

`AbstractIntegrationTest` est repris dans le tableau back mais exclu des totaux : classe de
base abstraite sans méthode `@Test`.

### 3.2 Ratios d'intégration RÉELS recalculés

| Périmètre | Intégration déclarée | Ratio déclaré | Intégration RÉELLE | **Ratio RÉEL** | Seuil ≥ 30 % |
|---|---|---|---|---|---|
| Back | 60 / 125 (les 4 classes `*IT`) | 48,0 % | **61 / 125** | **48,8 %** | ✅ largement au-dessus |
| Front | 24 / 65 (`describe` « rendu (intégration DOM) ») | 36,9 % | **34 / 65** | **52,3 %** | ✅ largement au-dessus |
| Global | 84 / 190 | 44,2 % | **95 / 190** | **50,0 %** | ✅ |

- **Back** : le ratio réel (48,8 %) **confirme** l'ancien ratio déclaré ≈ 48,8 % — aucun
  glissement. Le reclassement d'`ApplicationContextTest.contextLoads` fait passer le décompte
  de 60 à 61 tests d'intégration, soit exactement l'écart entre 48,0 % (calcul par nom de
  fichier) et 48,8 %.
- **Front** : le ratio réel (52,3 %) est **supérieur** au ratio déclaré (36,9 %), les 10 écarts
  allant tous dans le sens unitaire → intégration.
- **Aucun des deux ratios ne repasse sous 30 %.** Aucun test d'intégration supplémentaire
  n'est requis pour tenir le critère noté, quelle que soit l'issue de l'arbitrage.

**Marge de sécurité si l'arbitrage tranche en faveur de la convention maison du front**
(c'est-à-dire si les 10 écarts front sont rejetés) : le ratio front retomberait à 24/65 =
**36,9 %**, toujours ≥ 30 %. Le back resterait à 48,8 % (ou 48,0 % si `contextLoads` est
maintenu en unitaire). **Aucun scénario d'arbitrage ne met le seuil de 30 % en danger.**

### 3.3 Fichiers contenant au moins un écart (priorisation de l'arbitrage)

| Fichier | Nb d'écarts | Nature |
|---|---|---|
| front/src/app/pages/sessions/components/detail/detail.component.spec.ts | 3 | Unitaire déclaré → Intégration réel (`HttpTestingController` dans le corps) |
| front/src/app/core/service/auth.service.spec.ts | 2 | Idem |
| front/src/app/core/service/user.service.spec.ts | 2 | Idem |
| front/src/app/pages/sessions/components/form/form.component.spec.ts | 2 | Idem |
| front/src/app/pages/sessions/components/list/list.component.spec.ts | 1 | Idem |
| back/src/test/java/com/openclassrooms/starterjwt/ApplicationContextTest.java | 1 | Unitaire déclaré (nom `*Test`) → Intégration réel (`@SpringBootTest` + Testcontainers) |

Aucun écart dans les 12 autres fichiers back et les 8 autres fichiers front.

### 3.4 Observations complémentaires (pas des écarts, à verser à l'arbitrage)

1. **Aucun `@MockBean`/`@WebMvcTest` dans le projet** : le piège identifié dans
   `METHODE_AUDIT.md` (« controller testé avec MockMvc mais service mocké, faussement
   étiqueté intégration ») **ne se matérialise pas ici**. Aucun test nommé `*IT` n'est en
   réalité unitaire.
2. **Intégration de profondeur limitée (back)** : 15 tests d'intégration ne franchissent pas
   la couche service pendant la requête HTTP, tout en restant légitimement classés intégration
   (contexte réel, aucun mock, données préparées via repositories réels) :
   - 9 tests `*_returns401_whenNotAuthenticated` (rejet par la chaîne de filtres réelle) ;
   - 6 tests de Bean Validation (`@Valid` rejette avant le corps du controller) :
     `register_returns400_whenEmailIsBlank`, `register_returns400_whenEmailFormatIsInvalid`,
     `register_returns400_whenPasswordIsTooShort`, `login_returns400_whenPasswordIsBlank`,
     `create_returns400_whenNameIsMissing`, `update_returns400_whenValidationFails`.

   Les 7 tests `*_400_whenIdIsNotNumeric` et les 4 tests `403_whenCalledByNonAdmin` ne sont
   **pas** dans ce lot : leur authentification préalable traverse réellement
   `AuthTokenFilter` → `UserDetailsServiceImpl` → `UserRepository` → MySQL.
3. **Intégration DOM sans couche HTTP réelle (front)** : dans `me.component.spec.ts`, les
   deux tests du bloc « rendu (intégration DOM) » lisent bien le DOM rendu, mais
   `UserService.getById` est court-circuité par `jest.spyOn(...).mockReturnValue(of(...))` —
   il n'y a donc ni `HttpTestingController` ni round-trip HTTP. Classés Intégration (le DOM
   est réellement rendu et le critère unitaire — « assertion sur une propriété de classe » —
   ne s'applique pas), mais avec une profondeur de coordination moindre que
   `detail`/`list`/`form`. À signaler si l'arbitrage veut homogénéiser la méthode de stub côté
   front.
4. **Round-trip HTTP en `beforeEach`** : dans `detail.component.spec.ts` et
   `form.component.spec.ts`, plusieurs tests classés unitaires (`should create`,
   `should pre-fill the form...`, `should be in edit mode...`) s'exécutent après un flush
   `HttpTestingController` effectué dans le `beforeEach`, et sont couverts par un
   `afterEach httpMock.verify()`. Le critère ayant été appliqué **au corps du test**, ils
   restent unitaires. Un arbitrage retenant « le câblage du bloc » plutôt que « le corps du
   test » les ferait basculer en intégration et remonterait encore le ratio front — sans
   changer aucune conclusion sur le seuil.
