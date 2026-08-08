# Audit de jugement — sur-vérification des mocks (H) et sélecteurs Cypress (I)

Audit en lecture seule, sur la branche `chore/verification-finale-livrable-p4`.
Aucun fichier de test n'a été modifié. Portée : `front/src/app/**/*.spec.ts`,
`back/src/test/java/**/*.java` (H) et `front/cypress/e2e/**/*.cy.ts` (I).

---

## H. Sur-vérification des mocks

### Méthode

- **Front (Jest)** : recherche exhaustive de `toHaveBeenCalledWith(...)` dans
  `front/src/app/**/*.spec.ts` (15 occurrences, 8 fichiers). Chaque appel a été
  lu avec son contexte (le test entier, pas la ligne seule).
- **Back (Mockito)** : recherche de `verify(...)` (33 occurrences) puis
  filtrage des vérifications avec arguments précis (`eq(...)`, `argThat`,
  assertions sur `ArgumentCaptor`). **Aucune occurrence de `verify(mock).method(eq(...), eq(...), ...)`
  avec plusieurs matchers précis n'existe dans ce dépôt** — le style dominant
  est `verify(repo).save(session)` (référence d'objet directe) ou
  `verify(repo, never()).save(any())` (assertion de non-appel). Les seuls cas
  d'inspection fine passent par `ArgumentCaptor`, examinés ci-dessous.

### Front — tableau

| Fichier | Ligne | Extrait | Verdict | Raisonnement |
|---|---|---|---|---|
| `app.component.spec.ts` | 108 | `expect(navigateSpy).toHaveBeenCalledWith([''])` | Comportement observable légitime | La destination de navigation après logout est le contrat fonctionnel testé (l'utilisateur doit atterrir sur `/` → redirigé vers `/login`). |
| `me.component.spec.ts` | 96 | `expect(userService.getById).toHaveBeenCalledWith('1')` | Comportement observable légitime | Vérifie que l'id de l'utilisateur **connecté** (et non un id arbitraire) est bien transmis à l'API — c'est le cœur du contrat de la page Account (voir le commentaire d'en-tête d'`account.cy.ts` : l'id vient de la session, jamais d'un paramètre de route). |
| `me.component.spec.ts` | 140 | `expect(userService.delete).toHaveBeenCalledWith('1')` | Comportement observable légitime | Même raisonnement : le bon compte doit être supprimé. |
| `me.component.spec.ts` | 141 | `expect(mockMatSnackBar.open).toHaveBeenCalledWith('Your account has been deleted !', 'Close', { duration: 3000 })` | Comportement observable légitime | Le message est ce que l'utilisateur voit réellement à l'écran (repris à l'identique dans `account.cy.ts:144`) ; ce n'est pas un détail interne. |
| `me.component.spec.ts` | 143 | `expect(navigateSpy).toHaveBeenCalledWith(['/'])` | Comportement observable légitime | Destination de navigation post-suppression, contrat fonctionnel. |
| `login.component.spec.ts` | 107 | `expect(authService.login).toHaveBeenCalledWith({ email: ..., password: ... })` | Comportement observable légitime | Le payload envoyé au service d'auth est directement dérivé de la saisie utilisateur — c'est le contrat testé (le formulaire transmet bien ce qui a été saisi). |
| `login.component.spec.ts` | 108 | `expect(logInSpy).toHaveBeenCalledWith(sessionInfo)` | Comportement observable légitime | Vérifie que la session renvoyée par le back est bien celle stockée par `SessionService` — pas un détail d'implémentation interne, c'est l'état d'authentification de l'app. |
| `login.component.spec.ts` | 109 | `expect(navigateSpy).toHaveBeenCalledWith(['/sessions'])` | Comportement observable légitime | Redirection post-login, contrat fonctionnel. |
| `register.component.spec.ts` | 109-114 | `expect(authService.register).toHaveBeenCalledWith({ email, firstName, lastName, password })` | Comportement observable légitime | Idem login : le payload correspond exactement aux 4 champs du formulaire, tous fonctionnellement significatifs (aucun champ technique/interne mélangé). |
| `register.component.spec.ts` | 115 | `expect(navigateSpy).toHaveBeenCalledWith(['/login'])` | Comportement observable légitime | Redirection post-inscription. |
| `detail.component.spec.ts` | 149 | `expect(snackBarSpy).toHaveBeenCalledWith('Session deleted !', 'Close', { duration: 3000 })` | Comportement observable légitime | Message affiché à l'utilisateur, repris à l'identique côté Cypress. |
| `detail.component.spec.ts` | 150 | `expect(router.navigate).toHaveBeenCalledWith(['sessions'])` | Comportement observable légitime | Redirection post-suppression. |
| `form.component.spec.ts` | 157, 209, 284 | `expect(mockRouter.navigate).toHaveBeenCalledWith(['sessions'])` / `(['/sessions'])` | Comportement observable légitime | Redirections post-submit (create/update) et redirection de garde non-admin — comportements attendus par l'utilisateur, pas des détails internes. |

**Aucun cas front à surveiller.** Point notable : dans tous les cas ci-dessus,
l'objet vérifié (payload de formulaire, route de navigation, message de
snackbar) est **entièrement composé de champs significatifs pour le test** —
il n'y a jamais de reconstruction d'objet "riche" avec des champs accessoires
noyés dans l'assertion (ce qui aurait été le signal d'alerte typique de cette
catégorie). Les payloads `toHaveBeenCalledWith({...})` de login/register
correspondent exactement aux champs du formulaire, sans champ technique
supplémentaire — donc pas de sur-spécification.

### Back — tableau (captors et vérifications ciblées)

| Fichier | Ligne | Extrait | Verdict | Raisonnement |
|---|---|---|---|---|
| `SessionServiceTest.java` | 141-148 | `ArgumentCaptor<Session> captor ...; verify(sessionRepository).save(captor.capture()); assertThat(captor.getValue().getId()).isEqualTo(1L);` | Comportement observable légitime | Seul le champ pertinent (`id`) est vérifié sur l'objet capturé, pas une reconstruction champ par champ de l'entité `Session` entière. C'est exactement le pattern recommandé (extraire uniquement ce qui compte pour le test) plutôt qu'un `eq(session)` global fragile. |
| `UserServiceTest.java` | 127-130 | `ArgumentCaptor<Session> sessionCaptor ...; assertThat(sessionCaptor.getValue().getId()).isEqualTo(10L); assertThat(sessionCaptor.getValue().getUsers()).extracting(User::getId).containsExactly(2L);` | Comportement observable légitime | Vérifie l'effet fonctionnel réel (l'utilisateur 2 retiré de la liste des participants de la session 10), pas l'ensemble des champs de l'entité (date, description, teacher, etc. ignorés à raison). |
| `UserServiceTest.java` | ~302-310 | `ArgumentCaptor<User> userCaptor ...` + `verify(passwordEncoder, times(1)).encode("rawPassword")` | Comportement observable légitime | `encode("rawPassword")` vérifie un contrat de sécurité réel (le mot de passe en clair est bien celui fourni, pas un autre) — précision nécessaire, pas accessoire. |
| `SessionServiceTest.java` / `UserServiceTest.java` | multiples | `verify(sessionRepository).save(session)` (référence directe, pas de matcher champ par champ) | Comportement observable légitime | Utilise l'objet de test lui-même comme référence — équivalent fonctionnel à "la bonne session a été sauvegardée", sans sur-spécifier des champs individuels. |
| `AuthTokenFilterTest.java` | 57, 67, 78 | `verify(filterChain).doFilter(request, response)` | Comportement observable légitime | Vérifie la propagation de la requête dans la chaîne de filtres (comportement du filtre JWT), avec les objets `request`/`response` du test — pas de détail d'implémentation interne. |

**Aucun cas back à surveiller.** Le style de test du projet évite
structurellement ce risque : les captors sont utilisés pour extraire un
sous-ensemble de champs pertinents plutôt que pour comparer l'objet entier
champ par champ, et les `verify(...)` sans captor utilisent soit l'objet de
test tel quel (référence directe), soit `any()`/`never()` (assertion
d'appel/non-appel sans précision excessive). Il n'y a aucune vérification de
l'**ordre** d'appels internes (pas d'usage de `InOrder`), donc pas de risque
de ce sous-cas non plus.

### Résumé H

| | Comportement observable légitime | Détail d'implémentation à surveiller |
|---|---|---|
| Front (Jest) | 15 / 15 | 0 |
| Back (Mockito) | 12 / 12 (verify ciblés + captors) | 0 |

---

## I. Sélecteurs Cypress fragiles

### Méthode

Inventaire de tous les `cy.get(...)`, `cy.contains(...)` et `.find(...)`
(aucun `.find()` n'est utilisé dans ce projet — uniquement `cy.get`/`cy.contains`)
dans les 9 fichiers de `front/cypress/e2e/`. Le projet n'utilise **aucun**
attribut `data-cy`/`data-testid` (vérifié par recherche sur tout `front/`) —
il n'existe donc structurellement aucune occurrence de catégorie (a).

### Tableau (sélecteurs récurrents regroupés par motif — les lignes exactes de
chaque occurrence individuelle sont listées dans les fichiers cités)

| Fichier(s) | Sélecteur | Catégorie | Verdict |
|---|---|---|---|
| tous | `input[formControlName=email]`, `[formControlName=password]`, `[formControlName=name]`, etc. | Attribut sémantique lié au binding Reactive Forms (ni (a) dédié aux tests, ni (c) pur CSS de présentation) | Acceptable. `formControlName` est un contrat de liaison de données Angular, pas une classe de style — il ne changera pas au gré d'une refonte visuelle. Robustesse proche de (a) sans en être un, car il pourrait changer si le nom du contrôle de formulaire est renommé (refactor logique, pas visuel) — risque jugé faible et acceptable. |
| tous | `button[type=submit]` | Attribut HTML sémantique (proche de (a)) | Acceptable. Type HTML natif, stable, indépendant du framework CSS/UI. |
| `account.cy.ts`, `logout.cy.ts` | `.link` (`cy.contains('.link', 'Account')`, `.contains('.link', 'Logout')`) | **(c) classe CSS** | **À surveiller.** `.link` est un nom de classe CSS défini dans `app.component.html`/`.scss` pour le style de la toolbar. Si la classe est renommée lors d'une refonte visuelle de la toolbar (sans changement de comportement), tous ces tests cassent. Le texte (`'Account'`, `'Logout'`) est déjà suffisant et fonctionnellement pertinent (le libellé du lien fait partie du contrat testé) — `cy.contains('Account')` seul aurait été plus robuste, `.link` n'ajoute ici qu'un filtrage de présentation. |
| `sessions-list.cy.ts`, `sessions-detail.cy.ts`, `sessions-delete.cy.ts`, `sessions-update.cy.ts`, `sessions-create.cy.ts` | `.item` (`cy.get('.item')`, `cy.contains('.item', session.name)`) | **(c) classe CSS** | **À surveiller, mais avec nuance.** `.item` est la classe CSS de la carte de session dans `list.component.html`. Elle sert ici à **scoper** une recherche de texte à une carte précise (ex. `cy.contains('.item', session.name).within(() => cy.contains('button', 'Detail').click())`), ce qui est nécessaire quand plusieurs sessions partagent des boutons au libellé identique ("Detail", "Edit") — sans ce scope, `cy.contains('button', 'Detail')` cliquerait sur la première carte de la page, pas forcément la bonne. Le besoin fonctionnel (isoler une carte parmi plusieurs) est réel, mais le moyen utilisé (classe de présentation) reste fragile à un renommage CSS. Un `data-cy="session-card"` serait plus robuste pour le même besoin. |
| `sessions-list.cy.ts:36,54,76,104,107` | `cy.get('.item').should('have.length', ...)`, `.first()`, `.each(...)` | **(c) classe CSS** | **À surveiller**, même raisonnement que ci-dessus — ici `.item` sert en plus à compter le nombre de cartes rendues, ce qui est un vrai test de comportement (le bon nombre de sessions s'affiche), mais reste couplé à une classe de style plutôt qu'à un attribut dédié. |
| `sessions-detail.cy.ts:65` | `cy.get('.description').should('contain.text', session.description)` | **(c) classe CSS** | **À surveiller.** `.description` est une classe de présentation dans `detail.component.html`. Il n'y a pas d'alternative textuelle simple ici (le texte de la description est la donnée elle-même, on ne peut pas faire `cy.contains(session.description)` sans risquer une ambiguïté si le texte apparaît ailleurs — en pratique ce serait équivalent et plus robuste, donc le couplage à `.description` n'est pas strictement nécessaire). |
| tous | `cy.contains('button', 'Create'|'Edit'|'Detail'|'Delete'|'Participate'|'Do not participate')` | (b) texte visible | Acceptable — fonctionnellement pertinent. Ces libellés sont eux-mêmes le comportement testé (ex. `sessions-list.cy.ts:103-107` vérifie explicitement que "Create"/"Edit" sont absents et "Detail" présent pour un non-admin : le texte du bouton **est** l'assertion). |
| tous | `cy.contains('h1', ...)`, `cy.contains('mat-card-title', ...)` | Sélecteur de tag HTML/composant Material combiné à texte — proche de (b) | Acceptable. Le tag `h1`/`mat-card-title` est structurel mais générique (pas une classe de style spécifique à ce projet) ; combiné à un texte qui est lui-même le contrat testé (titre de page, libellé de carte). Risque de casse faible : ne changerait qu'avec une refonte de structure de titre, peu probable indépendamment du texte. |
| `login.cy.ts:38`, `register.cy.ts:40` | `cy.get('.error').should('be.visible').and('contain.text', 'An error occurred')` | **(c) classe CSS** | **À surveiller, mais faible risque.** `.error` est une classe CSS dédiée à l'affichage d'erreur. Contrairement à `.link`/`.item`, il n'existe ici aucune alternative textuelle pratique : `cy.contains('An error occurred')` seul fonctionnerait tout aussi bien et serait strictement plus robuste (le texte est déjà l'assertion), donc le couplage à `.error` est un choix accessoire évitable plutôt qu'un besoin de scope comme pour `.item`. |
| `sessions-create.cy.ts`, `sessions-update.cy.ts` | `mat-select[formControlName=teacher_id]`, `mat-option` | Sélecteur de composant Angular Material (tag custom, pas classe CSS) | Acceptable. Ce sont des sélecteurs de balises de composants (API publique de la librairie UI), pas des classes de style internes — plus proches d'un contrat de composant que d'un détail de présentation. Risque : changerait seulement si le composant Material `mat-select` était remplacé par un autre widget, ce qui serait un changement fonctionnel majeur de toute façon. |
| `sessions-create.cy.ts:49,83,121,152` | `input[type=date][formControlName=date]` | Attribut HTML sémantique + binding forms | Acceptable, même raisonnement que `formControlName` plus haut. |

### Cas ambigus discutés

- **`.item` (le plus fréquent, ~15 occurrences)** : c'est le cas le plus
  intéressant du lot. Il n'est pas utilisé pour un jugement esthétique
  ("est-ce que ça a l'air d'une carte") mais pour **désambiguïser entre
  plusieurs sessions affichées simultanément** — un besoin fonctionnel réel
  puisque la page liste N sessions avec des boutons au libellé identique.
  Le classer strictement en (c) "le plus fragile" serait injuste sans
  contexte : le risque de casse ne vient pas d'un changement de comportement
  testé, mais d'un renommage de classe CSS indépendant. Verdict : à
  surveiller pour la robustesse à long terme (un `data-cy="session-item"`
  serait préférable), mais pas un signe de mauvais test — le besoin qu'il
  sert est légitime.
- **`.link`** : contrairement à `.item`, il n'y a ici aucun besoin de scope
  (un seul lien "Account"/"Logout" par page) — le texte seul aurait suffi.
  C'est donc un couplage plus évitable que celui de `.item`.
- **`.error`** : même remarque que `.link` — aucun besoin de scope, le texte
  du message est déjà l'assertion complète, `.error` n'ajoute rien de
  fonctionnel.
- **`h1`/`mat-card-title`** : techniquement des sélecteurs de structure, mais
  jugés acceptables car (1) ce sont des tags génériques/composants publics,
  pas des classes de style propres au projet, et (2) toujours combinés à un
  texte qui porte l'assertion réelle.

### Résumé I

| Catégorie | Occurrences (motifs distincts) | Verdict |
|---|---|---|
| (a) attribut dédié aux tests (`data-cy`) | 0 | — (absent du projet) |
| (b) texte visible, fonctionnellement pertinent | `cy.contains('button', ...)`, `h1`/`mat-card-title` + texte | Acceptable — aucune occurrence de (b) incident détectée |
| (c) classe CSS / structure de présentation | `.link`, `.item`, `.description`, `.error` (4 motifs distincts) | À surveiller — dont 1 motif (`.item`) sert un besoin fonctionnel réel (scope), et 3 motifs (`.link`, `.description`, `.error`) sont couplés à une classe de style sans nécessité (le texte seul aurait suffi) |
| formControlName / type HTML / composants Material | `input[formControlName=...]`, `button[type=submit]`, `mat-select`/`mat-option` | Acceptable — liés au contrat de binding/composant, pas à la présentation visuelle |

**Aucun `data-cy`/`data-testid` n'existe dans ce projet** : c'est un choix
structurel antérieur à cet audit, pas une omission locale à corriger
fichier par fichier — à traiter comme une décision d'ensemble si elle doit
changer.

---

## Synthèse

- **H (sur-vérification des mocks)** : 0 cas à surveiller sur 27 vérifications
  précises examinées (15 front + 12 back). Le style de test du projet évite
  structurellement ce risque (captors ciblés sur les champs pertinents,
  jamais de comparaison d'objet champ par champ complet, jamais de
  vérification d'ordre d'appel).
- **I (sélecteurs Cypress)** : 0 occurrence de catégorie (a), aucune
  occurrence de (b) incident. 4 motifs de catégorie (c) identifiés
  (`.link`, `.item`, `.description`, `.error`), à traiter avec nuance :
  `.item` répond à un besoin fonctionnel réel (scope entre sessions) même
  s'il reste fragile ; `.link`, `.description` et `.error` sont des
  couplages évitables (le texte seul suffisait). À arbitrer ensemble —
  aucune correction n'a été appliquée par cet audit.
