# Audit — Migration `*ngIf`/`*ngFor`/`*ngSwitch` → `@if`/`@for`/`@switch`

Date : 2026-08-07
Périmètre : `front/src/app/` (hors `node_modules`, hors fichiers générés)
Type d'audit : lecture seule — aucun fichier modifié.

## Méthode

1. `grep -rn "\*ngIf\|\*ngFor\|\*ngSwitch" front/src/app` sur l'ensemble de l'arborescence.
2. Recherche de templates inline (`template:` dans les `.ts`) — aucun trouvé : tous les composants
   utilisent `templateUrl` vers un fichier `.html` séparé.
3. Recherche complémentaire de `@if`/`@for`/`@switch` pour confirmer que la migration a bel et bien
   eu lieu (et pas seulement l'absence de logique conditionnelle).

## Périmètre scanné (preuve d'exhaustivité)

- **8 fichiers `.html`** de templates, tous recensés :
  - `app.component.html`
  - `pages/not-found/not-found.component.html`
  - `pages/login/login.component.html`
  - `pages/register/register.component.html`
  - `pages/sessions/components/detail/detail.component.html`
  - `pages/sessions/components/form/form.component.html`
  - `pages/sessions/components/list/list.component.html`
  - `components/me/me.component.html`
- **0 template inline** (`template:` dans un `.ts`) — tous les composants applicatifs utilisent
  `templateUrl`, donc aucun template supplémentaire à couvrir côté `.ts`.

## Résultat — anciennes directives structurelles

**0 occurrence** de `*ngIf`, `*ngFor`, `*ngSwitch`, `*ngSwitchCase`, `*ngSwitchDefault` dans
`front/src/app` (recherche exacte, aucun faux positif à écarter : le grep n'a renvoyé aucun résultat,
donc pas de commentaire ou de chaîne de caractères à trier).

La migration annoncée lors d'Ex1 est donc toujours respectée à ce jour : aucune régression
n'a été introduite par le code ajouté/modifié depuis (Phase 1 à 5 de l'audit post-Ex2, corrections
diverses).

## Vérification positive — usage de `@if`/`@for`/`@switch`

La migration est réelle (pas une absence de logique conditionnelle) : **18 occurrences** de
`@if`/`@for` réparties sur **6 des 8** fichiers `.html` (aucun `@switch` dans le projet, ce qui est
cohérent — aucune logique de type switch n'est nécessaire ici).

Échantillon (4 fichiers) :

**`pages/sessions/components/list/list.component.html:5,13`**
```html
@if (user!.admin) {
  ...
}
@for (session of (sessions$ | async) ?? []; track session.id) {
  ...
}
```

**`pages/sessions/components/detail/detail.component.html:2,13,19,21,27,41`**
```html
@if (session) {
  ...
  @if (isAdmin) { ... }
  @if (!isAdmin) {
    @if (!isParticipate) { ... }
    @if (isParticipate) { ... }
  }
  ...
  @if (teacher) { ... }
}
```

**`pages/sessions/components/form/form.component.html:8,15,29`**
```html
@if (!onUpdate) { ... }
@if (sessionForm) {
  ...
  @for (teacher of (teachers$ | async) ?? []; track teacher.id) { ... }
}
```

**`components/me/me.component.html:12,16,19`**
```html
@if (user) {
  ...
  @if (user.admin) { ... }
  @if (!user.admin) { ... }
}
```

Autres fichiers utilisant la nouvelle syntaxe : `app.component.html` (`@if` sur l'état de connexion),
`pages/login/login.component.html` et `pages/register/register.component.html` (`@if (onError)`).

`pages/not-found/not-found.component.html` ne contient aucune logique conditionnelle/de répétition
(page statique) — absence de `@if`/`@for` normale et non un signe de non-migration.

## Conclusion

- **Conformité totale confirmée** : 0 directive structurelle obsolète (`*ngIf`/`*ngFor`/`*ngSwitch`)
  dans l'ensemble du code source applicatif front (8/8 fichiers `.html`, 0 template inline).
- La nouvelle syntaxe de contrôle de flux (`@if`/`@for`) est effectivement utilisée dans 6 fichiers
  sur 8, ce qui prouve que la migration a été réalisée et non simplement qu'il n'y avait rien à
  migrer.
- Aucune correction nécessaire — audit clos sans action.
