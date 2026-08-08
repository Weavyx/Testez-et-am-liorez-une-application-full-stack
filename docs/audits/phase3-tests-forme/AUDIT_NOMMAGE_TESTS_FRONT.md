# Audit — Convention de nommage des tests (front, Jest)

Audit en **lecture seule**, réalisé sur la branche `chore/verification-finale-livrable-p4`.
Aucun fichier n'a été modifié. Les classifications ci-dessous sont établies à partir du **corps**
de chaque test (ce qu'il configure et vérifie réellement), pas de son nom actuel.

Principe rappelé :
- **Nominal** (happy path / comportement par défaut) → nom le plus simple possible, sans
  qualificatif de condition puisqu'il n'y en a pas.
- **Cas limite / erreur** (tout ce qui dévie du nominal) → nom qui explicite précisément ce qui
  distingue ce cas du nominal (`when<Condition>` clair et spécifique).

Note méthodologique sur les "features à deux branches" (ex. admin/non-admin, formulaire
valide/invalide, participant/non-participant) : quand aucune des deux branches n'est un
"défaut implicite" au sens du principe (les deux sont des chemins également légitimes d'une
même fonctionnalité conditionnelle), les deux noms explicites sont conformes — ce n'est **pas**
une violation de type 2 tant que chacun dit précisément quelle branche il couvre. Seul le
véritable couple "opération réussie (implicite) / opération en échec (déviation)" — ex. login,
register, submit — a un vrai candidat "nominal" dont le nom ne doit pas répéter le succès.

## Fichiers hors périmètre (1 seul test — pas de nominal/cas limite à distinguer)

| Fichier | Test unique |
|---|---|
| `core/service/session-api.service.spec.ts` | `should be created` |
| `core/service/teacher.service.spec.ts` | `should be created` |
| `pages/not-found/not-found.component.spec.ts` | `should create` |

## `app.component.spec.ts`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should create the app | nominal | oui | — |
| should reflect the session state through $isLogged() (false, then true after logIn) | nominal | oui | — |
| should display Login/Register links (not Sessions/Account/Logout) when not logged in | nominal | oui | — |
| should log out and navigate to "/" when the Logout link is clicked | nominal | oui | — |

## `components/me/me.component.spec.ts`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should fetch and display the user information in the DOM | nominal | oui | — |
| should show "You are admin" and hide the Delete account button when the displayed user is admin | cas limite | oui | — |
| should create | nominal | oui | — |
| should call window.history.back on back() | nominal | oui | — |
| should delete the account, notify the user and navigate on delete | nominal | oui | — |

## `core/service/auth.service.spec.ts`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should be created | nominal | oui | — |
| should send a POST request to api/auth/login with the credentials | nominal | oui | — |
| should send a POST request to api/auth/register with the registration data | nominal | oui | — |

## `core/service/session.service.spec.ts`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should be created | nominal | oui | — |
| should initialize as not logged in | nominal | oui | — |
| should emit false initially | nominal | oui | — |
| should emit true after logIn | cas limite | oui | — |
| should emit false after logOut | cas limite | oui | — |
| should store the user in sessionInformation | nominal | oui | — |
| should set isLogged to true | nominal | oui | — |
| should clear sessionInformation | nominal | oui | — |
| should set isLogged to false | nominal | oui | — |

## `core/service/user.service.spec.ts`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should be created | nominal | oui | — |
| should send a GET request to api/user/:id and return the user | nominal | oui | — |
| should send a DELETE request to api/user/:id | nominal | oui | — |

## `pages/login/login.component.spec.ts`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should set onError to true and display an error message when login fails | cas limite | oui | — |
| should disable the submit button when a required field is missing | cas limite | oui | — |
| should create | nominal | oui | — |
| should log in and navigate to /sessions on successful submit | nominal | **non** (type 1) | `should log in and navigate to /sessions on submit` — "successful" répète un succès déjà implicite par défaut ; le cas d'échec a son propre test (`when login fails`) |

## `pages/register/register.component.spec.ts`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should set onError to true when registration fails | cas limite | oui | — |
| should disable the submit button when a required field is missing | cas limite | oui | — |
| should create | nominal | oui | — |
| should register and navigate to /login on successful submit | nominal | **non** (type 1) | `should register and navigate to /login on submit` — même raison que pour login.component |

## `pages/sessions/components/detail/detail.component.spec.ts`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should display the session name, description, teacher and date | nominal | oui | — |
| should show the Delete button for an admin | cas limite | oui | — |
| should not show the Participate/UnParticipate buttons for an admin | cas limite | oui | — |
| should create | nominal | oui | — |
| should navigate back in browser history when back() is called | nominal | oui | — |
| should delete the session, notify the user and navigate to /sessions | nominal | oui | — |
| should not show the Delete button for a non-admin | cas limite | oui | — |
| should show the Participate button when the user has not joined | cas limite | oui | — |
| should toggle the DOM to "Do not participate" and update the attendee count when the Participate button is clicked | nominal | oui | — |
| should call the participate API with the session and user id, then reload the session | nominal | oui | — |
| should show the UnParticipate button when the user has already joined | cas limite | oui | — |
| should toggle the DOM to "Participate" and update the attendee count when the Do not participate button is clicked | nominal | oui | — |
| should call the unParticipate API with the session and user id, then reload the session | nominal | oui | — |

## `pages/sessions/components/form/form.component.spec.ts`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should disable the submit button when the form is invalid | cas limite | oui | — |
| should enable the submit button when the form is valid | nominal | oui | — |
| should create | nominal | oui | — |
| should be in create mode (onUpdate = false) | nominal | oui | — |
| should initialize an empty form in create mode | nominal | oui | — |
| should call the create API and navigate to sessions on submit | nominal | oui | — |
| should still render the form fields in the DOM even though a non-admin redirect was triggered | cas limite | oui | — |
| should redirect a non-admin user to /sessions on init | cas limite | oui | — |
| should disable the submit button when a required field is missing | cas limite | oui | — |
| should be in edit mode (onUpdate = true) | nominal | oui | — |
| should pre-fill the form with the existing session data | nominal | oui | — |
| should call the update API and navigate to sessions on submit | nominal | oui | — |

## `pages/sessions/components/list/list.component.spec.ts`

| Nom du test | Classification | Conforme ? | Suggestion |
|---|---|---|---|
| should fetch and display the sessions returned by the API in the DOM | nominal | oui | — |
| should display the "Create" button when the user is admin | cas limite | oui | — |
| should not display the "Create" button when the user is not admin | cas limite | oui | — |
| should display the Detail button even for a non-admin user (intentional: Detail also drives the participate/unparticipate flow) | cas limite | oui | — |
| should create | nominal | oui | — |

## Résumé

- **Fichiers audités** : 13 au total ; **10** avec ≥2 tests (classifiés ci-dessus), **3** hors périmètre (1 seul test).
- **Tests classifiés** : 62
  - Nominal : 43
  - Cas limite / erreur : 19
- **Violations**
  - Type 1 (nominal trop qualifié) : **2**
  - Type 2 (cas limite trop vague) : **0**
  - Total : **2**

### Liste priorisée des fichiers avec violations

| Rang | Fichier | Violations |
|---|---|---|
| 1 | `pages/login/login.component.spec.ts` | 1 (type 1) |
| 1 | `pages/register/register.component.spec.ts` | 1 (type 1) |

Tous les autres fichiers audités (8/10) sont entièrement conformes au principe.

### Constat général

Le corpus de tests est globalement propre : aucune violation de type 2 (les cas limites/erreurs
ont déjà des noms précis — `when login fails`, `when a required field is missing`, `for a
non-admin`, `when the user has not joined`, etc.), cohérent avec les audits de non-régression
déjà menés sur ce dépôt. Les deux seules violations relevées sont de même nature (`on
successful submit`) et strictement symétriques entre `login.component.spec.ts` et
`register.component.spec.ts` — probablement issues du même gabarit de test copié d'un fichier à
l'autre. Une correction groupée des deux occurrences réglerait l'intégralité des violations
identifiées dans cet audit.
