# Audit — OnPush & uniformisation de `inject()` (front/)

Date : 2026-07-03
Périmètre : `front/` uniquement (aucune modification du `back/`).
Type de livrable : **audit seul, aucun code modifié**.

## 1. Rappel de la commande

Déterminer, fichier par fichier :
- si le passage en `ChangeDetectionStrategy.OnPush` est sûr pour les 7 composants + l'interceptor ;
- si l'uniformisation de `inject()` (à la place de l'injection par constructeur) est pertinente pour les 2 guards et les 4 services HTTP.

Aucune ligne de code source n'a été modifiée pour produire ce rapport.

## 2. Confirmation des chiffres de référence

```
grep -rl "inject(" src/app --include="*.ts" | grep -v spec   → 8 fichiers
grep -rn "ChangeDetectionStrategy" src/app --include="*.ts"  → 0 résultat
```

Les 8 fichiers utilisant déjà `inject()` : `app.component.ts`, `me.component.ts`,
`login.component.ts`, `register.component.ts`, `detail.component.ts` (pour ses
dépendances — le `constructor()` qui subsiste dans ce fichier est vide de paramètres,
il ne sert qu'à exécuter de la logique d'initialisation, pas de l'injection),
`form.component.ts`, `list.component.ts`, `customJwtInterceptorFn.ts`.

Aucun composant n'utilise `OnPush` actuellement. Les deux chiffres annoncés dans la
commande sont donc **confirmés** sur l'état actuel du code.

Recherche complémentaire : aucun `@Input()` n'existe dans les 7 composants audités
(`grep -rn "@Input" src/app` → aucun résultat). Le critère « @Input reçus » de la
commande est donc **sans objet** pour ce périmètre — à surveiller uniquement si des
`@Input()` sont introduits plus tard.

## 3. Tableau — Composants et `OnPush`

| Composant | Verdict | Raison précise |
|---|---|---|
| `app.component.ts` | **Sûr** | Seule donnée réactive affichée est `$isLogged() \| async` (app.component.html:4), déjà pipée avec `async` — le pipe appelle `markForCheck()` en interne à chaque émission, donc compatible OnPush nativement. Aucune assignation de propriété dans un `.subscribe()`. Remarque mineure (non bloquante) : `$isLogged()` est une méthode appelée dans le template (app.component.ts:20-22) qui retourne un nouvel objet `Observable` à chaque appel (via `asObservable()`) — non risqué pour la correction, mais générerait un réabonnement à chaque exécution de détection de changement une fois passé en OnPush. Recommandé mais optionnel : remplacer par un champ `public isLogged$ = this.sessionService.$isLogged();` assigné une seule fois. |
| `me.component.ts` | **À adapter avant** | `ngOnInit` fait `.subscribe((user: User) => this.user = user)` (me.component.ts:30) — réassignation de référence, mais dans un callback HTTP, hors template event et hors pipe `async`. Le template lit `user` directement (`@if (user)`, me.component.html:12), pas via `\| async`. Sous OnPush, cette réponse HTTP asynchrone ne marquera pas le composant "dirty" : la vue resterait figée sur l'état initial (`user` undefined) tant qu'aucun autre événement ne déclenche un contrôle. Nécessite soit `this.cdr.markForCheck()` dans le callback, soit conversion de `getById()` en `user$` consommé via `\| async` dans le template. |
| `login.component.ts` | **À adapter avant** | `error: error => this.onError = true` dans `.subscribe({...})` (login.component.ts:54), consommé par `@if (onError)` (login.component.html:21-23) sans `async`. Même risque que `me.component.ts` : l'échec de connexion HTTP ne rafraîchira pas l'affichage de l'erreur sous OnPush sans `markForCheck()`. À l'inverse, `hide = !hide` (login.component.html:14, déclenché par un `(click)` dans le template) est sûr car un événement lié au template déclenche la détection de changement locale du composant même sous OnPush. |
| `register.component.ts` | **À adapter avant** | Même schéma exact que `login.component.ts` : `error: _ => this.onError = true` dans `.subscribe({...})` (register.component.ts:63), lu par `@if (onError)` (register.component.html:22-24) sans `async`. Nécessite `markForCheck()` dans le callback d'erreur avant tout passage en OnPush. |
| `detail.component.ts` | **Risqué** | Le composant le plus exposé : `fetchSession()` assigne `this.session`, `this.isParticipate` et (dans un `.subscribe()` imbriqué) `this.teacher` (detail.component.ts:78-84), toutes lues directement dans le template sans `async` (`@if (session)`, `@if (teacher)`, detail.component.html:2 et 41). Ces trois assignations ont lieu dans des callbacks HTTP imbriqués, déclenchés indirectement par `ngOnInit`, `participate()` et `unParticipate()` — aucune n'est un événement template synchrone au moment de la mutation. Sous OnPush, aucune de ces mises à jour ne serait reflétée sans `markForCheck()` explicite à chaque callback (3 points d'injection nécessaires). Le risque de régression silencieuse (page de détail qui ne se met plus à jour après "Participate"/"Delete") est élevé si la migration est incomplète. |
| `form.component.ts` | **À adapter avant** | Pour le flux de création : `teachers$` est un champ `Observable` consommé via `(teachers$ \| async)` (form.component.html:29) — déjà compatible OnPush, sûr. Pour le flux de mise à jour (`onUpdate`) : `.subscribe((session: Session) => this.initForm(session))` (form.component.ts:44-45) réassigne `this.sessionForm` (nouvelle référence `FormGroup`, form.component.ts:68), lu par `@if (sessionForm)` (form.component.html:15) sans `async`. Sous OnPush, l'écran "Update session" resterait vide (le formulaire ne s'afficherait jamais) tant que `markForCheck()` n'est pas appelé dans ce callback. Le flux de création seul serait sûr ; c'est le flux update qui bloque une migration directe. |
| `list.component.ts` | **Sûr (avec réserve mineure)** | `sessions$` est un champ `Observable` consommé via `(sessions$ \| async) ?? []` (list.component.html:13) — entièrement compatible OnPush. Réserve mineure : le getter `get user()` (list.component.ts:23-25) lit `sessionService.sessionInformation`, un champ mutable simple du service (pas un `Observable`/signal), consommé directement dans le template (`user!.admin`, list.component.html:5 et 34) sans `async`. En pratique sans risque aujourd'hui : `sessionInformation` est déjà fixé au login, avant l'arrivée sur cet écran, et ne change pas pendant la durée de vie du composant (un logout entraîne une navigation qui détruit le composant). À signaler comme dette potentielle si `sessionInformation` devient mutable en cours de vie du composant à l'avenir. |
| `customJwtInterceptorFn.ts` | **Non applicable** | C'est une fonction d'interception HTTP (`HttpInterceptorFn`), pas un composant Angular — la notion de `ChangeDetectionStrategy` ne s'y applique pas. Utilise déjà `inject(SessionService)` en style fonctionnel, rien à changer côté OnPush. |

### Synthèse des points de vigilance à corriger avant toute migration OnPush

Chaque callback listé ci-dessous devrait recevoir un `markForCheck()` (ou être remplacé
par un flux `Observable`/signal consommé via `\| async`) avant le passage en OnPush du
composant correspondant :

- `me.component.ts:30`
- `login.component.ts:54`
- `register.component.ts:63`
- `detail.component.ts:78-84` (3 assignations : `session`, `isParticipate`, `teacher`)
- `form.component.ts:45` (flux update uniquement)

## 4. Tableau — Guards & services et `inject()`

| Fichier | Verdict | Raison précise |
|---|---|---|
| `guards/auth.guard.ts` | **Trivial (technique) / sensible (fonctionnel)** | `constructor(private router: Router, private sessionService: SessionService)` (auth.guard.ts:8-11), aucun héritage, aucun `super()`, logique synchrone simple (`canActivate()`). La transformation en `inject()` est mécanique. **Cependant** ce guard protège toutes les routes `/sessions/**` et `/me` (app.routes.ts) — toute régression ici bloque ou expose l'accès à l'application entière. À traiter avec un test manuel dédié (connexion, accès direct par URL à une route protégée sans session, déconnexion) en plus de `npm test`. |
| `guards/unauth.guard.ts` | **Trivial (technique) / sensible (fonctionnel)** | Même structure exacte que `auth.guard.ts` (unauth.guard.ts:8-11) : constructeur simple, pas d'héritage. Migration mécanique triviale, mais guard sensible (protège `/login` et `/register`) — mêmes précautions que `auth.guard.ts`. Observation hors périmètre (à ne pas corriger dans cette tâche) : ce guard redirige vers `['rentals']` (unauth.guard.ts:16), une route qui ne semble pas exister dans `app.routes.ts` (les routes connues sont `/sessions`, `/me`, `/login`, `/register`) — signalé pour information, aucune action proposée ici. |
| `core/service/auth.service.ts` | **Trivial** | `constructor(private httpClient: HttpClient)` (auth.service.ts:15), aucun héritage, deux méthodes qui délèguent directement à `HttpClient`. Migration mécanique triviale. Ce service porte les appels `/api/auth/login` et `/api/auth/register` (chemin d'authentification) : la transformation ne change pas le comportement, mais comme pour les guards, recommandé de valider login + register manuellement après migration en plus de `npm test`. |
| `core/service/user.service.ts` | **Trivial** | `constructor(private httpClient: HttpClient)` (user.service.ts:13), aucun héritage, deux méthodes CRUD simples. Aucune logique sensible additionnelle. Migration mécanique triviale, faible risque. |
| `core/service/teacher.service.ts` | **Trivial** | `constructor(private httpClient: HttpClient)` (teacher.service.ts:13), aucun héritage, deux méthodes de lecture simples. Migration mécanique triviale, faible risque. |
| `core/service/session-api.service.ts` | **Trivial** | `constructor(private httpClient: HttpClient)` (session-api.service.ts:13), aucun héritage. Sept méthodes, toutes de simples délégations à `HttpClient` (`all`, `detail`, `delete`, `create`, `update`, `participate`, `unParticipate`). Migration mécanique triviale, faible risque malgré le nombre de méthodes — aucune ne contient de logique conditionnelle. |

Remarque générale : dans les 6 fichiers ci-dessus, aucun ne présente d'héritage de
classe ni de `super()` avec dépendances — le seul facteur de risque n'est pas
technique mais fonctionnel (guards + `AuthService` touchent le chemin d'authentification).

## 5. Recommandation de périmètre

### `inject()` (guards + services)

Ordre suggéré, du moins risqué au plus sensible :

1. **`user.service.ts`, `teacher.service.ts`, `session-api.service.ts`** — à migrer en premier. Aucune surface auth, aucun héritage, changement mécanique pur.
2. **`auth.service.ts`** — migration tout aussi mécanique, mais à faire dans un commit séparé et à valider avec un test manuel de login/register, car il porte le chemin d'authentification.
3. **`auth.guard.ts`, `unauth.guard.ts`** — à traiter en dernier et un par un (pas dans le même commit), avec validation manuelle explicite (accès à une route protégée déconnecté, accès à `/login` connecté) en plus de la suite de tests, car une régression ici a l'impact le plus large (accès à toute l'application).

### `OnPush`

Ordre suggéré, du moins risqué au plus sensible :

1. **`list.component.ts`** — candidat le plus sûr : déjà 100% piloté par `async` pipe pour ses données de flux, seule réserve mineure et non bloquante sur le getter `user`.
2. **`app.component.ts`** — sûr, seule amélioration facultative sur `$isLogged()` recommandée en même temps (éviter le réabonnement par cycle) mais non bloquante.
3. **`login.component.ts`, `register.component.ts`** — à adapter avant migration : ajouter `markForCheck()` (ou équivalent signal) dans le callback `error` de `.subscribe()`, puis migrer. Structure quasi identique entre les deux, peuvent être traités ensemble.
4. **`me.component.ts`** — à adapter avant migration : `markForCheck()` (ou `user$ | async`) dans le callback `ngOnInit`.
5. **`form.component.ts`** — à adapter avant migration, uniquement pour le flux `onUpdate` (le flux création est déjà sûr).
6. **`detail.component.ts`** — à reporter en dernier / traiter isolément : c'est le composant avec le plus de callbacks à corriger (3 assignations dans des `.subscribe()` imbriqués) et le plus de scénarios utilisateurs à revalider manuellement (delete, participate, unParticipate, affichage initial). Le risque de régression silencieuse (écran qui ne se rafraîchit plus après une action) y est le plus élevé.

`customJwtInterceptorFn.ts` est hors sujet pour `OnPush` (ce n'est pas un composant) et
déjà conforme pour `inject()`.

## 6. Vérifications anti-régression

Aucune vérification n'a été exécutée dans le cadre de cette tâche : c'est un audit,
aucun fichier source n'a été modifié.

Pour rappel, toute migration ultérieure (OnPush et/ou `inject()`) réalisée à partir de
ce rapport devra être suivie systématiquement de :

```bash
npm test            # vérifier que la couverture reste ≥ 80% statements et inchangée sur Ex1/Ex2-Front
npx cypress run     # si des e2e existent déjà sur les écrans touchés
```

Aucun test existant (Ex1, Ex2-Front — 55 tests, couverture 98.16/100/93.54/97.99%) n'a
été modifié ou exécuté par cet audit.
