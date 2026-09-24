# Audit — Typage explicite des retours de méthode

Date : 2026-08-07
Branche : `chore/verification-finale-livrable-p4`
Périmètre : `front/src/app/**/*.ts`, hors `*.spec.ts`
Type d'audit : lecture seule — aucun fichier modifié (y compris config).

## Méthode utilisée

Les deux approches outillées suggérées n'ont pas pu être utilisées sans risquer une modification
implicite ou un résultat non exploitable :
- `npx tsc --noEmit --noImplicitAny --strict` aurait nécessité de passer des flags en plus de la
  config existante — écarté par prudence (pas de garantie de non-effet de bord, et le strict mode
  fait remonter énormément de diagnostics sans rapport avec le typage de retour).
- `npx eslint ... --no-eslintrc --parser-options=...` : le projet utilise la configuration plate
  ESLint (`eslint.config.js`), qui ne supporte plus les flags CLI historiques `--no-eslintrc` /
  `--parser-options` (confirmé par erreur CLI : *"You're using eslint.config.js, some command line
  flags are no longer available"*).

**Audit manuel** effectué à la place : lecture intégrale de chacun des 24 fichiers `.ts` non-spec
de `front/src/app`, recensement de chaque méthode de classe, fonction exportée et accesseur
(`get`/`set`), vérification de la présence d'une annotation de type de retour explicite.
`git status` vérifié propre avant et après (aucune commande de lecture n'a modifié de fichier).

## Fichiers examinés (24, hors specs)

`app.component.ts`, `app.config.ts`, `app.routes.ts`, `components/me/me.component.ts`,
`core/models/*.interface.ts` (5 fichiers — interfaces uniquement, aucune méthode),
`core/service/auth.service.ts`, `core/service/session-api.service.ts`,
`core/service/session.service.ts`, `core/service/teacher.service.ts`,
`core/service/user.service.ts`, `guards/auth.guard.ts`, `guards/unauth.guard.ts`,
`interceptors/customJwtInterceptorFn.ts`, `pages/login/login.component.ts`,
`pages/not-found/not-found.component.ts`, `pages/register/register.component.ts`,
`pages/sessions/components/detail/detail.component.ts`,
`pages/sessions/components/form/form.component.ts`,
`pages/sessions/components/list/list.component.ts`, `shared/material.module.ts`.

Fichiers sans aucune méthode de classe (exclus du décompte) : `app.config.ts`, `app.routes.ts`,
`shared/material.module.ts`, `pages/not-found/not-found.component.ts`, et les 5 fichiers
`*.interface.ts`.

## Résultat

**37 méthodes/fonctions/accesseurs examinés — 0 sans type de retour explicite.**

Répartition (méthode : type de retour déclaré) :

| Fichier | Méthodes | Type(s) de retour déclaré(s) |
|---|---|---|
| `app.component.ts` | `$isLogged()`, `logout()` | `Observable<boolean>`, `void` |
| `core/service/session.service.ts` | `$isLogged()`, `logIn()`, `logOut()`, `next()` (privée) | `Observable<boolean>`, `void`, `void`, `void` |
| `core/service/auth.service.ts` | `register()`, `login()` | `Observable<void>`, `Observable<SessionInformation>` |
| `core/service/user.service.ts` | `getById()`, `delete()` | `Observable<User>`, `Observable<void>` |
| `core/service/session-api.service.ts` | `all()`, `detail()`, `delete()`, `create()`, `update()`, `participate()`, `unParticipate()` | `Observable<Session[]>`, `Observable<Session>`, `Observable<void>`, `Observable<Session>`, `Observable<Session>`, `Observable<void>`, `Observable<void>` |
| `core/service/teacher.service.ts` | `all()`, `detail()` | `Observable<Teacher[]>`, `Observable<Teacher>` |
| `guards/auth.guard.ts` | `canActivate()` | `boolean` |
| `guards/unauth.guard.ts` | `canActivate()` | `boolean` |
| `interceptors/customJwtInterceptorFn.ts` | `customJwtInterceptorFn()` (fonction, hors classe) | `Observable<HttpEvent<unknown>>` |
| `pages/login/login.component.ts` | `submit()` | `void` |
| `pages/register/register.component.ts` | `submit()` | `void` |
| `components/me/me.component.ts` | `ngOnInit()`, `back()`, `delete()` | `void`, `void`, `void` |
| `pages/sessions/components/form/form.component.ts` | `ngOnInit()`, `submit()`, `initForm()` (privée), `exitPage()` (privée) | `void`, `void`, `void`, `void` |
| `pages/sessions/components/detail/detail.component.ts` | `ngOnInit()`, `back()`, `delete()`, `participate()`, `unParticipate()`, `fetchSession()` (privée) | `void` ×6 |
| `pages/sessions/components/list/list.component.ts` | accesseur `get user()` | `SessionInformation \| undefined` |

Note : les **constructeurs** (`AuthGuard`, `UnauthGuard`, `DetailComponent`) sont exclus du
décompte — en TypeScript, un constructeur ne peut pas déclarer de type de retour (interdit par le
langage), donc la consigne ne s'y applique pas.

Aucun callback inline (`.subscribe()`, `.pipe()`) n'a été comptabilisé, conformément au périmètre
défini par la consigne — seules les méthodes de classe et fonctions exportées sont concernées.

## Conclusion

Conformité totale confirmée par lecture manuelle exhaustive : les 37 méthodes/fonctions de classe
recensées dans les 24 fichiers `.ts` applicatifs (hors tests) déclarent toutes un type de retour
explicite, y compris `void`. La pratique appliquée lors d'Ex1 reste respectée après les refactors
post-Ex2. Aucune correction nécessaire — audit clos sans action.
