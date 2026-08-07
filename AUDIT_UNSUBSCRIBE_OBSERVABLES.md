# Audit — Désabonnement des Observables (`takeUntilDestroyed`)

Date : 2026-08-07
Périmètre : `front/src/app/**/*.ts`, hors `*.spec.ts`
Type d'audit : lecture seule — aucun fichier modifié.

## Méthode

1. `grep -rn "\.subscribe(" front/src/app --include="*.ts" | grep -v ".spec.ts"` pour lister tous
   les points d'entrée.
2. Lecture complète de chaque fichier concerné pour identifier, pour chaque `.subscribe()` :
   - la méthode contenante,
   - la source réelle de l'Observable (remontée jusqu'au service, pour vérifier qu'il s'agit bien
     d'un appel `HttpClient` one-shot et non d'un `Subject`/`BehaviorSubject` long-lived déguisé),
   - la présence ou non de `takeUntilDestroyed`.
3. Classement en catégorie a/b/c/d selon la grille définie dans la consigne.

## Fichiers scannés contenant un `.subscribe()` hors tests

- `pages/login/login.component.ts`
- `pages/register/register.component.ts`
- `components/me/me.component.ts`
- `pages/sessions/components/form/form.component.ts`
- `pages/sessions/components/detail/detail.component.ts`

Aucun `.subscribe()` trouvé dans : `app.component.ts`, `pages/sessions/components/list/list.component.ts`,
les guards (`auth.guard.ts`, `unauth.guard.ts`), l'intercepteur (`customJwtInterceptorFn.ts`), ni dans
`session.service.ts` — ces fichiers exposent leurs flux via `| async` dans le template ou ne
consomment pas d'Observable en interne (catégorie c ou non concerné).

## Détail par `.subscribe()`

| Fichier | Ligne | Contexte (méthode) | Source de l'Observable | Catégorie | Protection présente |
|---|---|---|---|---|---|
| `pages/login/login.component.ts` | 49 | `submit()` | `AuthService.login()` → `httpClient.post` (one-shot) | a | `takeUntilDestroyed(this.destroyRef)` |
| `pages/register/register.component.ts` | 61 | `submit()` | `AuthService.register()` → `httpClient.post` (one-shot) | a | `takeUntilDestroyed(this.destroyRef)` |
| `components/me/me.component.ts` | 30 | `ngOnInit()` | `UserService.getById()` → `httpClient.get` (one-shot) | a | `takeUntilDestroyed(this.destroyRef)` |
| `components/me/me.component.ts` | 41 | `delete()` | `UserService.delete()` → `httpClient.delete` (one-shot) | a | `takeUntilDestroyed(this.destroyRef)` |
| `pages/sessions/components/form/form.component.ts` | 45 | `ngOnInit()` | `SessionApiService.detail()` → `httpClient.get` (one-shot) | a | `takeUntilDestroyed(this.destroyRef)` |
| `pages/sessions/components/form/form.component.ts` | 58 | `submit()` | `SessionApiService.create()` → `httpClient.post` (one-shot) | a | `takeUntilDestroyed(this.destroyRef)` |
| `pages/sessions/components/form/form.component.ts` | 63 | `submit()` | `SessionApiService.update()` → `httpClient.put` (one-shot) | a | `takeUntilDestroyed(this.destroyRef)` |
| `pages/sessions/components/detail/detail.component.ts` | 55 | `delete()` | `SessionApiService.delete()` → `httpClient.delete` (one-shot) | a | `takeUntilDestroyed(this.destroyRef)` |
| `pages/sessions/components/detail/detail.component.ts` | 65 | `participate()` | `SessionApiService.participate()` → `httpClient.post` (one-shot) | a | `takeUntilDestroyed(this.destroyRef)` |
| `pages/sessions/components/detail/detail.component.ts` | 71 | `unParticipate()` | `SessionApiService.unParticipate()` → `httpClient.delete` (one-shot) | a | `takeUntilDestroyed(this.destroyRef)` |
| `pages/sessions/components/detail/detail.component.ts` | 77 | `fetchSession()` | `SessionApiService.detail()` → `httpClient.get` (one-shot) | a | `takeUntilDestroyed(this.destroyRef)` |
| `pages/sessions/components/detail/detail.component.ts` | 83 | `fetchSession()` (subscribe imbriqué) | `TeacherService.detail()` → `httpClient.get` (one-shot) | a | `takeUntilDestroyed(this.destroyRef)` |

Toutes les sources ont été remontées jusqu'au service (`AuthService`, `UserService`,
`SessionApiService`, `TeacherService`) : chacune retourne directement le résultat d'un appel
`HttpClient` (`get`/`post`/`put`/`delete`), donc un Observable qui complète après une seule émission.
Aucun `Subject`/`BehaviorSubject` déguisé en source.

## Résumé

- **Total `.subscribe()` hors tests : 12**
- Répartition :
  - **Catégorie a** (protégé par `takeUntilDestroyed(this.destroyRef)`) : **12 / 12**
  - Catégorie b (non géré mais non fuyant) : 0
  - Catégorie c (`| async` dans le template) : non comptabilisé ici (pas un `.subscribe()`), mais
    utilisé par ailleurs pour `sessions$` (`list.component.html`), `teachers$`
    (`form.component.html`) et `$isLogged()` (`app.component.html`) — cohérent avec la consigne.
  - **Catégorie d (fuite mémoire réelle) : 0**

## Cas (d) trouvés

**Aucun.** Tous les `.subscribe()` du code applicatif (hors specs) sont soit protégés par
`takeUntilDestroyed(this.destroyRef)` avec injection correcte de `DestroyRef` dans le constructeur
de chaque composant concerné, soit remplacés par `| async` dans les templates pour les flux
observés en continu (`sessions$`, `teachers$`, `$isLogged()`).

## Conclusion

La pratique appliquée lors d'Ex1 (`takeUntilDestroyed`) est intégralement respectée sur l'ensemble
du code applicatif actuel : les 12 `.subscribe()` recensés dans les composants (`login`, `register`,
`me`, `form`, `detail`) sont tous en catégorie (a), avec `DestroyRef` injecté et `takeUntilDestroyed`
appliqué systématiquement avant chaque `.subscribe()`, y compris pour le `.subscribe()` imbriqué de
`detail.component.ts` (ligne 83, à l'intérieur du callback de la ligne 77). Aucune régression
introduite depuis Ex1 sur ce point. Aucune correction nécessaire — audit clos sans action.
