# Audit — Convention de nommage des tests (back, JUnit)

Audit en **lecture seule**, réalisé sur la branche `chore/verification-finale-livrable-p4`, suite
au même retour mentor que l'audit front (voir `AUDIT_NOMMAGE_TESTS_FRONT.md`). Aucun fichier n'a
été modifié. Les classifications ci-dessous sont établies à partir du **corps** de chaque test (ce
qu'il configure, mocke et vérifie réellement), pas de son nom actuel.

Principe rappelé :
- **Nominal** (happy path / comportement par défaut/attendu) → nom le plus simple possible, sans
  qualificatif de condition puisqu'il n'y en a pas.
- **Cas limite / erreur** (tout ce qui dévie du nominal : validation, id manquant, droits
  insuffisants, ressource absente, doublon, non-propriétaire, non-admin...) → nom qui explicite
  précisément ce qui distingue ce cas du nominal (`whenX`/`returnsX` clair et spécifique — déjà en
  place dans ce projet, ex. `findById_returns404_whenSessionDoesNotExist`).

Note méthodologique sur les "features à deux branches également légitimes" (ex. admin/non-admin,
self/not-self) : quand aucune des deux branches n'est un "défaut implicite" au sens du principe
(les deux sont des chemins également légitimes d'une même fonctionnalité conditionnelle — ex.
`create_returns200_whenCalledByAdmin` / `create_returns403_whenCalledByNonAdmin`,
`findById_returns200AndUserDto_whenUserReadsOwnAccount` /
`findById_returns403_whenUserReadsAnotherUsersAccount`), les deux noms explicites sont conformes —
ce n'est **pas** une violation tant que chacun dit précisément quelle branche il couvre. Seul le
véritable couple "opération réussie (implicite) / échec (déviation réelle : ressource absente,
donnée invalide, doublon...)" a un vrai candidat "nominal" dont le nom ne doit pas répéter le
succès — ex. `register_returns200AndSuccessMessage_whenDataIsValid`, où `whenDataIsValid` répète
un cas déjà implicite (le cas d'erreur de validation est couvert par des tests dédiés).

Même remarque pour les méthodes purement prédicat/lookup à deux issues égales (`existsByEmail`,
`isAdmin`, `findByEmail` retournant un `Optional`) : les deux branches sont traitées comme des
branches légitimes, pas comme un couple succès/erreur.

Les classes de modèles (`SessionTest`, `TeacherTest`, `UserTest`, `UserDetailsImplTest`) testent
majoritairement le contrat `equals`/`hashCode`/validation de constructeur : chaque cas est une
branche distincte et nécessairement qualifiée (aucun "cas par défaut" implicite entre "equals avec
la même instance" et "equals avec un id différent"), donc conforme par nature.

## Fichiers hors périmètre (0 ou 1 test)

| Fichier | Tests |
|---|---|
| `AbstractIntegrationTest.java` | 0 — classe de base abstraite, aucune méthode `@Test` |
| `ApplicationContextIT.java` | 1 — `contextLoads` |
| `exception/GlobalExceptionHandlerTest.java` | 1 méthode `@Test` (`@ParameterizedTest` avec 3 sources de données) — `should_returnBadRequestWithoutBody_when_handleBadRequestExceptionIsCalled_and_messageIsNullOrBlank` |

## `controllers/AuthControllerIT.java`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| register_returns200AndSuccessMessage_whenDataIsValid | nominal | **non** (type 1) | `register_returns200AndSuccessMessage` — "whenDataIsValid" répète un succès déjà implicite ; les cas d'erreur de validation ont chacun leur propre test |
| register_returns400AndMessage_whenEmailIsAlreadyTaken | cas limite | oui | — |
| register_returns400_whenEmailIsBlank | cas limite | oui | — |
| register_returns400_whenEmailFormatIsInvalid | cas limite | oui | — |
| register_returns400_whenPasswordIsTooShort | cas limite | oui | — |
| register_returns400AndListsAllFieldErrors_whenMultipleFieldsAreInvalid | cas limite | oui | — |
| login_returns200AndJwtResponse_whenCredentialsAreValid | nominal | **non** (type 1) | `login_returns200AndJwtResponse` — même raison que pour register |
| login_returns401_whenPasswordIsWrong | cas limite | oui | — |
| login_returns401_whenEmailIsUnknown | cas limite | oui | — |
| login_returns400_whenPasswordIsBlank | cas limite | oui | — |

## `controllers/SessionControllerIT.java`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| findById_returns200AndSession_whenSessionExists | nominal | **non** (type 1) | `findById_returns200AndSession` |
| findById_returns404_whenSessionDoesNotExist | cas limite | oui | — |
| findById_returns400_whenIdIsNotNumeric | cas limite | oui | — |
| findById_returns401_whenNotAuthenticated | cas limite | oui | — |
| findAll_returns200AndAllSessions_whenAuthenticated | nominal | **non** (type 1) | `findAll_returns200AndAllSessions` — l'authentification est le prérequis normal de la route, pas une condition à qualifier côté succès |
| findAll_returns401_whenNotAuthenticated | cas limite | oui | — |
| findAll_returns401_whenTokenIsMalformed | cas limite | oui | — |
| findAll_returns401_whenTokenIsExpired | cas limite | oui | — |
| create_returns200_whenCalledByAdmin | nominal | oui | branche admin/non-admin légitime (cf. note méthodologique) |
| create_returns403_whenCalledByNonAdmin | cas limite | oui | — |
| create_returns400_whenNameIsMissing | cas limite | oui | — |
| create_returns401_whenNotAuthenticated | cas limite | oui | — |
| update_returns200_whenCalledByAdmin | nominal | oui | branche admin/non-admin légitime |
| update_returns403_whenCalledByNonAdmin | cas limite | oui | — |
| update_returns400_whenIdIsNotNumeric | cas limite | oui | — |
| update_returns403NotBadRequest_whenCalledByNonAdminWithInvalidId | cas limite | oui | test de contrat précis (verrouille l'ordre d'évaluation sécurité/validation) |
| update_returns404_whenSessionDoesNotExist | cas limite | oui | — |
| update_returns400_whenValidationFails | cas limite | oui | — |
| update_returns401_whenNotAuthenticated | cas limite | oui | — |
| delete_returns200_whenCalledByAdmin | nominal | oui | branche admin/non-admin légitime |
| delete_returns403_whenCalledByNonAdmin | cas limite | oui | — |
| delete_returns400_whenIdIsNotNumeric | cas limite | oui | — |
| delete_returns404_whenSessionDoesNotExist | cas limite | oui | — |
| delete_returns401_whenNotAuthenticated | cas limite | oui | — |
| delete_returns200AndClearsParticipations_whenSessionHasParticipants | cas limite | oui | scénario précis (effet de bord sur la table de jointure), nom déjà spécifique |
| participate_returns200_whenNonAdminParticipatesForThemselves | nominal | oui | branche self/not-self légitime (cf. note méthodologique) |
| participate_returns403_whenUserIdDoesNotMatchAuthenticatedPrincipal | cas limite | oui | — |
| participate_returns400_whenIdIsNotNumeric | cas limite | oui | — |
| participate_returns404_whenSessionDoesNotExist | cas limite | oui | — |
| participate_returns404_whenUserDoesNotExist | cas limite | oui | — |
| participate_returns400_whenAlreadyParticipating | cas limite | oui | — |
| noLongerParticipate_returns200_whenNonAdminParticipatesForThemselves | nominal | oui | branche self/not-self légitime |
| noLongerParticipate_returns403_whenUserIdDoesNotMatchAuthenticatedPrincipal | cas limite | oui | — |
| noLongerParticipate_returns400_whenIdIsNotNumeric | cas limite | oui | — |
| noLongerParticipate_returns404_whenSessionDoesNotExist | cas limite | oui | — |
| noLongerParticipate_returns404_whenUserDoesNotExist | cas limite | oui | — |
| noLongerParticipate_returns400_whenNotParticipating | cas limite | oui | — |

## `controllers/TeacherControllerIT.java`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| findById_returns200AndTeacher_whenTeacherExists | nominal | **non** (type 1) | `findById_returns200AndTeacher` |
| findById_returns404_whenTeacherDoesNotExist | cas limite | oui | — |
| findById_returns400_whenIdIsNotNumeric | cas limite | oui | — |
| findById_returns401_whenNotAuthenticated | cas limite | oui | — |
| findAll_returns200AndAllTeachers_whenAuthenticated | nominal | **non** (type 1) | `findAll_returns200AndAllTeachers` |
| findAll_returns401_whenNotAuthenticated | cas limite | oui | — |

## `controllers/UserControllerIT.java`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| findById_returns200AndUserDto_whenUserReadsOwnAccount | nominal | oui | branche self/not-self légitime |
| findById_returns403_whenUserReadsAnotherUsersAccount | cas limite | oui | — |
| findById_returns404_whenUserDoesNotExist | cas limite | oui | — |
| findById_returns400_whenIdIsNotNumeric | cas limite | oui | — |
| findById_returns401_whenNotAuthenticated | cas limite | oui | — |
| delete_returns200_whenUserDeletesOwnAccount | nominal | oui | branche self/not-self légitime |
| delete_returns200AndClearsParticipations_whenUserDeletesOwnAccountWhileEnrolledInASession | cas limite | oui | scénario précis (effet de bord participations), nom déjà spécifique |
| delete_returns403_whenUserTriesToDeleteAnotherUsersAccount | cas limite | oui | — |
| delete_returns404_whenUserDoesNotExist | cas limite | oui | — |
| delete_returns400_whenIdIsNotNumeric | cas limite | oui | — |
| delete_returns401_whenNotAuthenticated | cas limite | oui | — |

## `models/SessionTest.java`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should_returnTrue_when_equalsIsCalledWithSameInstance | cas limite | oui | branche du contrat equals() |
| should_returnFalse_when_equalsIsCalledWithNull | cas limite | oui | — |
| should_returnFalse_when_equalsIsCalledWithDifferentClass | cas limite | oui | — |
| should_returnTrue_when_equalsIsCalledWithSameId_and_differentOtherFields | cas limite | oui | — |
| should_returnSameHashCode_when_idsAreEqual_and_differentOtherFields | cas limite | oui | — |
| should_returnFalse_when_equalsIsCalledWithDifferentId | cas limite | oui | — |
| should_returnTrue_when_equalsIsCalledWithBothIdsNull | cas limite | oui | — |
| should_returnFalse_when_equalsIsCalledWithOneIdNull | cas limite | oui | — |
| should_assignAllFields_when_settersAreCalled | nominal | oui | — |
| should_containFieldValues_when_builderToStringIsCalled | nominal | oui | — |

## `models/TeacherTest.java`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should_returnTrue_when_equalsIsCalledWithSameInstance | cas limite | oui | — |
| should_returnFalse_when_equalsIsCalledWithNull | cas limite | oui | — |
| should_returnFalse_when_equalsIsCalledWithDifferentClass | cas limite | oui | — |
| should_returnTrue_when_equalsIsCalledWithSameId_and_differentOtherFields | cas limite | oui | — |
| should_returnSameHashCode_when_idsAreEqual_and_differentOtherFields | cas limite | oui | — |
| should_returnFalse_when_equalsIsCalledWithDifferentId | cas limite | oui | — |
| should_returnTrue_when_equalsIsCalledWithBothIdsNull | cas limite | oui | — |
| should_returnFalse_when_equalsIsCalledWithOneIdNull | cas limite | oui | — |
| should_assignAllFields_when_settersAreCalled | nominal | oui | — |
| should_containFieldValues_when_builderToStringIsCalled | nominal | oui | — |

## `models/UserTest.java`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should_returnTrue_when_equalsIsCalledWithSameInstance | cas limite | oui | — |
| should_returnFalse_when_equalsIsCalledWithNull | cas limite | oui | — |
| should_returnFalse_when_equalsIsCalledWithDifferentClass | cas limite | oui | — |
| should_returnTrue_when_equalsIsCalledWithSameId_and_differentOtherFields | cas limite | oui | — |
| should_returnSameHashCode_when_idsAreEqual_and_differentOtherFields | cas limite | oui | — |
| should_returnFalse_when_equalsIsCalledWithDifferentId | cas limite | oui | — |
| should_returnTrue_when_equalsIsCalledWithBothIdsNull | cas limite | oui | — |
| should_returnFalse_when_equalsIsCalledWithOneIdNull | cas limite | oui | — |
| should_containFieldValues_when_toStringIsCalled | nominal | oui | — |
| should_assignAllFields_when_settersAreCalled | nominal | oui | — |
| should_throwNullPointerException_when_requiredArgsConstructorIsCalled_and_emailIsNull | cas limite | oui | — |
| should_throwNullPointerException_when_requiredArgsConstructorIsCalled_and_lastNameIsNull | cas limite | oui | — |
| should_throwNullPointerException_when_requiredArgsConstructorIsCalled_and_firstNameIsNull | cas limite | oui | — |
| should_throwNullPointerException_when_requiredArgsConstructorIsCalled_and_passwordIsNull | cas limite | oui | — |
| should_throwNullPointerException_when_allArgsConstructorIsCalled_and_emailIsNull | cas limite | oui | — |
| should_throwNullPointerException_when_allArgsConstructorIsCalled_and_lastNameIsNull | cas limite | oui | — |
| should_throwNullPointerException_when_allArgsConstructorIsCalled_and_firstNameIsNull | cas limite | oui | — |
| should_throwNullPointerException_when_allArgsConstructorIsCalled_and_passwordIsNull | cas limite | oui | — |
| should_throwNullPointerException_when_builderFieldIsSetToNull_and_emailIsNull | cas limite | oui | — |
| should_throwNullPointerException_when_builderFieldIsSetToNull_and_lastNameIsNull | cas limite | oui | — |
| should_throwNullPointerException_when_builderFieldIsSetToNull_and_firstNameIsNull | cas limite | oui | — |
| should_throwNullPointerException_when_builderFieldIsSetToNull_and_passwordIsNull | cas limite | oui | — |
| should_containFieldValues_when_builderToStringIsCalled | nominal | oui | — |

## `security/jwt/AuthTokenFilterTest.java`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should_continueChainWithNullAuthentication_when_userDetailsServiceThrows | cas limite | oui | — |
| should_notAuthenticate_and_continueChain_when_authorizationHeaderIsPresentButNotBearerPrefixed | cas limite | oui | — |
| should_notAuthenticate_and_continueChain_when_tokenIsPresentButInvalid | cas limite | oui | — |

Aucun test de succès (authentification effectivement posée dans le `SecurityContext`) n'existe
dans cette classe — les 3 tests couvrent chacun une déviation distincte, donc aucun n'est un
candidat "nominal" à simplifier.

## `security/jwt/JwtUtilsTest.java`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should_generateValidDecodableToken_when_generateJwtTokenIsCalled | nominal | oui | seul test de cette méthode, pas de condition répétée |
| should_extractSubject_when_getUserNameFromJwtTokenIsCalled | nominal | oui | seul test de cette méthode |
| should_returnTrue_when_validateJwtTokenIsCalled_with_validToken | nominal | **non** (type 1) | `should_returnTrue_when_validateJwtTokenIsCalled` — "with_validToken" répète le cas par défaut ; les 6 tests suivants couvrent chacun une déviation précise |
| should_returnFalse_when_validateJwtTokenIsCalled_with_invalidSignature | cas limite | oui | — |
| should_returnFalse_when_validateJwtTokenIsCalled_with_malformedToken | cas limite | oui | — |
| should_returnFalse_when_validateJwtTokenIsCalled_with_expiredToken | cas limite | oui | — |
| should_returnFalse_when_validateJwtTokenIsCalled_with_unsupportedToken | cas limite | oui | — |
| should_returnFalse_when_validateJwtTokenIsCalled_with_nullToken | cas limite | oui | — |
| should_returnFalse_when_validateJwtTokenIsCalled_with_emptyToken | cas limite | oui | — |

## `security/services/UserDetailsImplTest.java`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should_returnRoleAdmin_when_adminIsTrue | cas limite | oui | branche admin légitime |
| should_returnRoleUser_when_adminIsFalse | nominal | oui | — |
| should_returnRoleUser_when_adminIsNull | cas limite | oui | cas limite explicite (valeur null non attendue en usage normal) |
| should_returnTrue_when_isAccountNonExpiredIsCalled | nominal | oui | seule branche existante pour cette méthode |
| should_returnTrue_when_isAccountNonLockedIsCalled | nominal | oui | — |
| should_returnTrue_when_isCredentialsNonExpiredIsCalled | nominal | oui | — |
| should_returnTrue_when_isEnabledIsCalled | nominal | oui | — |
| should_returnTrue_when_equalsIsCalledWithSameInstance | cas limite | oui | — |
| should_returnFalse_when_equalsIsCalledWithNull | cas limite | oui | — |
| should_returnFalse_when_equalsIsCalledWithDifferentClass | cas limite | oui | — |
| should_returnTrue_when_equalsIsCalledWithSameId | cas limite | oui | — |
| should_returnFalse_when_equalsIsCalledWithDifferentId | cas limite | oui | — |

## `security/services/UserDetailsServiceImplTest.java`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should_returnUserDetails_when_loadUserByUsernameIsCalled_and_userExists | nominal | **non** (type 1) | `should_returnUserDetails_when_loadUserByUsernameIsCalled` |
| should_throwUsernameNotFoundException_when_loadUserByUsernameIsCalled_and_userDoesNotExist | cas limite | oui | — |

## `services/SessionServiceTest.java`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should_saveSession_when_createIsCalled | nominal | oui | — |
| should_deleteSession_when_deleteIsCalled_and_sessionExists | nominal | **non** (type 1) | `should_deleteSession_when_deleteIsCalled` |
| should_throwNotFoundException_when_deleteIsCalled_and_sessionDoesNotExist | cas limite | oui | — |
| should_returnAllSessions_when_findAllIsCalled | nominal | oui | — |
| should_returnEmptyList_when_findAllIsCalled_and_noSessionExists | cas limite | oui | — |
| should_returnSession_when_getByIdIsCalled_and_sessionExists | nominal | **non** (type 1) | `should_returnSession_when_getByIdIsCalled` |
| should_throwNotFoundException_when_getByIdIsCalled_and_sessionDoesNotExist | cas limite | oui | — |
| should_setIdFromParameter_when_updateIsCalled | nominal | oui | — |
| should_throwNotFoundException_when_updateIsCalled_and_sessionDoesNotExist | cas limite | oui | — |
| should_addUserToSession_when_participateIsCalled_and_notAlreadyParticipating | nominal | **non** (type 1) | `should_addUserToSession_when_participateIsCalled` — "notAlreadyParticipating" est la négation d'une condition d'erreur déjà couverte séparément |
| should_throwNotFoundException_when_participateIsCalled_and_sessionDoesNotExist | cas limite | oui | — |
| should_throwNotFoundException_when_participateIsCalled_and_userDoesNotExist | cas limite | oui | — |
| should_throwBadRequestException_when_participateIsCalled_and_userAlreadyParticipating | cas limite | oui | — |
| should_throwForbiddenException_when_participateIsCalled_and_userIdDoesNotMatchAuthenticatedPrincipal | cas limite | oui | — |
| should_removeUserFromSession_when_noLongerParticipateIsCalled_and_userIsParticipating | nominal | **non** (type 1) | `should_removeUserFromSession_when_noLongerParticipateIsCalled` |
| should_throwNotFoundException_when_noLongerParticipateIsCalled_and_sessionDoesNotExist | cas limite | oui | — |
| should_throwNotFoundException_when_noLongerParticipateIsCalled_and_userDoesNotExist | cas limite | oui | — |
| should_throwBadRequestException_when_noLongerParticipateIsCalled_and_userIsNotParticipating | cas limite | oui | — |
| should_throwForbiddenException_when_noLongerParticipateIsCalled_and_userIdDoesNotMatchAuthenticatedPrincipal | cas limite | oui | — |

## `services/TeacherServiceTest.java`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should_returnAllTeachers_when_findAllIsCalled | nominal | oui | — |
| should_returnEmptyList_when_findAllIsCalled_and_noTeacherExists | cas limite | oui | — |
| should_returnTeacher_when_findByIdIsCalled_and_teacherExists | nominal | **non** (type 1) | `should_returnTeacher_when_findByIdIsCalled` |
| should_throwNotFoundException_when_findByIdIsCalled_and_teacherDoesNotExist | cas limite | oui | — |

## `services/UserServiceTest.java`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should_throwForbiddenException_when_deleteByIdIsCalled_and_requesterIsNotTheOwner | cas limite | oui | — |
| should_throwForbiddenException_when_deleteByIdIsCalled_and_onlyTheEmailMatches | cas limite | oui | garde-fou de régression précis (bascule email → id) |
| should_removeUserFromParticipatedSessions_when_deleteByIdIsCalled | nominal | oui | — |
| should_notTouchAnySession_when_deleteByIdIsCalled_and_userParticipatesInNone | cas limite | oui | variante légitime (aucune session à nettoyer) |
| should_throwNotFoundException_when_deleteByIdIsCalled_and_userDoesNotExist | cas limite | oui | — |
| should_returnUser_when_findByIdIsCalled_and_userExists | nominal | **non** (type 1) | `should_returnUser_when_findByIdIsCalled` |
| should_throwNotFoundException_when_findByIdIsCalled_and_userDoesNotExist | cas limite | oui | — |
| should_returnUser_when_findOwnProfileIsCalled_and_requesterIsTheOwner | nominal | oui | branche self/not-self légitime |
| should_throwForbiddenException_when_findOwnProfileIsCalled_and_requesterIsNotTheOwner | cas limite | oui | — |
| should_throwNotFoundException_when_findOwnProfileIsCalled_and_userDoesNotExist | cas limite | oui | — |
| should_returnUser_when_findByIdIsCalled_regardlessOfRequester | cas limite | oui | documente explicitement l'absence intentionnelle de contrôle de propriété (contrat), nom déjà précis |
| should_returnUser_when_findByEmailIsCalled_and_userExists | nominal | oui | lookup à deux branches légitimes (retourne un `Optional`, pas d'exception) |
| should_returnEmptyOptional_when_findByEmailIsCalled_and_userDoesNotExist | cas limite | oui | — |
| should_returnTrue_when_existsByEmailIsCalled_and_emailIsTaken | nominal | oui | prédicat à deux branches légitimes |
| should_returnFalse_when_existsByEmailIsCalled_and_emailIsNotTaken | cas limite | oui | — |
| should_returnTrue_when_isAdminIsCalled_and_userIsAdmin | nominal | oui | prédicat à branches légitimes |
| should_returnFalse_when_isAdminIsCalled_and_userIsNotAdmin | cas limite | oui | — |
| should_returnFalse_when_isAdminIsCalled_and_userDoesNotExist | cas limite | oui | — |
| should_saveUserWithEncodedPassword_when_registerIsCalled_and_emailIsNotTaken | nominal | **non** (type 1) | `should_saveUserWithEncodedPassword_when_registerIsCalled` |
| should_throwBadRequestException_when_registerIsCalled_and_emailIsAlreadyTaken | cas limite | oui | — |

## Résumé

- **Fichiers audités** : 17 au total ; **14** avec ≥2 tests (classifiés ci-dessus), **3** hors
  périmètre (`AbstractIntegrationTest` — 0 test, `ApplicationContextIT` et
  `GlobalExceptionHandlerTest` — 1 seul test chacun).
- **Tests classifiés** : 172
  - Nominal : 45
  - Cas limite / erreur : 127
- **Violations**
  - Type 1 (nominal trop qualifié) : **15**
  - Type 2 (cas limite trop vague) : **0**
  - Total : **15**

### Liste priorisée des fichiers avec violations

| Rang | Fichier | Violations |
|---|---|---|
| 1 | `services/SessionServiceTest.java` | 4 (type 1) |
| 2 | `controllers/AuthControllerIT.java` | 2 (type 1) |
| 2 | `controllers/SessionControllerIT.java` | 2 (type 1) |
| 2 | `controllers/TeacherControllerIT.java` | 2 (type 1) |
| 2 | `services/UserServiceTest.java` | 2 (type 1) |
| 6 | `security/jwt/JwtUtilsTest.java` | 1 (type 1) |
| 6 | `security/services/UserDetailsServiceImplTest.java` | 1 (type 1) |
| 6 | `services/TeacherServiceTest.java` | 1 (type 1) |

`controllers/UserControllerIT.java`, les trois classes de `models/`, `security/jwt/AuthTokenFilterTest.java`
et `security/services/UserDetailsImplTest.java` sont entièrement conformes (0 violation).

### Constat général

Comme pour le front, **aucune violation de type 2** : les cas limites/erreurs du backend sont déjà
nommés avec précision (`whenSessionDoesNotExist`, `whenIdIsNotNumeric`, `whenUserIdDoesNotMatchAuthenticatedPrincipal`,
`whenAlreadyParticipating`, etc.), cohérent avec le niveau de rigueur déjà observé lors des audits
de non-régression précédents sur ce dépôt.

Les 15 violations de type 1 suivent toutes le même schéma : un test qui vérifie le succès normal
d'une opération de lecture/écriture/authentification porte un suffixe qui répète une condition
déjà implicite par défaut (`whenDataIsValid`, `whenCredentialsAreValid`, `whenSessionExists`,
`whenTeacherExists`, `whenAuthenticated`, `with_validToken`, `and_userExists`,
`and_notAlreadyParticipating`, `and_userIsParticipating`, `and_emailIsNotTaken`), alors que le cas
d'erreur réel est déjà couvert par un test dédié et précisément nommé juste à côté. `SessionServiceTest`
concentre le plus de cas (4/19 tests) car c'est le fichier avec le plus grand nombre de méthodes
`findById`/`getById`/`delete`/`participate`/`noLongerParticipate` suivant ce schéma
succès-implicite/échec-explicite.

À l'inverse, les paires admin/non-admin (`whenCalledByAdmin`/`whenCalledByNonAdmin`) et self/not-self
(`whenUserReadsOwnAccount`/`whenUserReadsAnotherUsersAccount`, `whenNonAdminParticipatesForThemselves`/
`whenUserIdDoesNotMatchAuthenticatedPrincipal`, `whenUserDeletesOwnAccount`/`whenUserTriesToDeleteAnotherUsersAccount`,
`requesterIsTheOwner`/`requesterIsNotTheOwner`) ainsi que les prédicats à deux issues égales
(`existsByEmail`, `isAdmin`, `findByEmail`) sont **correctement traités comme des branches
également légitimes**, conformément à la note méthodologique — ce ne sont pas des violations,
même si chaque nom porte un qualificatif.

Une correction groupée des 15 occurrences (retirer le suffixe redondant sur le cas nominal)
réglerait l'intégralité des violations identifiées dans cet audit.
