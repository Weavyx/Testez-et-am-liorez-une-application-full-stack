# Audit résidus de debug / dette technique — revérification post Phase 1-5

**Date** : 2026-08-07
**Branche** : `chore/verification-finale-livrable-p4`
**Contexte** : Cet audit revérifie les conclusions de `AUDIT_DETTE_PHASE0.md` (0 marqueur trouvé) à la lumière du volume de commits intervenus depuis (Phases 1 à 5 : ResponseEntity, `@WithMockUser`, renommages de tests, etc.). Audit en **lecture seule** — aucune modification apportée au code.

## Périmètre exploré

- `front/src/**/*.ts` (y compris `*.spec.ts`, y compris Cypress) — hors `node_modules` — **42 fichiers**
- `back/src/main/java/**/*.java` — **35 fichiers**
- `back/src/test/java/**/*.java` — **17 fichiers**

Exclus : `node_modules/`, `target/`, `dist/`.

---

## 1. Résidus de debug actif

### Front — `console.log` / `console.debug` / `console.warn` / `console.error`

**1 occurrence trouvée**, 0 résidu réel :

| Fichier | Ligne | Extrait | Avis |
|---|---|---|---|
| `front/src/main.ts` | 6 | `.catch((err) => console.error(err));` | **Faux positif légitime** — c'est le handler d'erreur standard d'Angular pour `bootstrapApplication()`, généré par le CLI Angular. Ce n'est pas un `console.log` de debug oublié mais la seule voie de remontée d'erreur possible à ce stade (avant que l'app ne soit démarrée). |

Aucun autre `console.*` (log/debug/warn) trouvé, y compris dans les fichiers `*.spec.ts`.

### Back — `System.out.println` / `System.err.println` / `e.printStackTrace()`

**0 occurrence** dans `back/src/main/java` et `back/src/test/java`. Recherche exhaustive, catégorie confirmée à zéro.

---

## 2. Tests désactivés/isolés oubliés

### Front — `.only(`, `.skip(`, `xit(`, `xdescribe(` (Jest et Cypress)

**0 occurrence** dans `front/src` (specs Jest et tests Cypress inclus).

### Back — `@Disabled`, `@Ignore` (JUnit)

**0 occurrence** dans `back/src/main/java` et `back/src/test/java`.

---

## 3. Marqueurs de dette non traités

### Front — `TODO`, `FIXME`, `XXX`, `HACK`

**0 occurrence** (recherche insensible à la casse) dans `front/src`.

### Back — `TODO`, `FIXME`, `XXX`, `HACK`

**0 occurrence** (recherche insensible à la casse) dans `back/src/main/java` et `back/src/test/java`.

Aucun faux positif à signaler (pas de chaîne contenant incidemment "todo").

---

## 4. Code mort commenté

### Front

Aucun bloc `/* ... */` (hors JSDoc `/** */`) ni séquence de lignes `//` ressemblant à du code (assignations, accolades, `import`, `return`, etc.) trouvé dans `front/src`. **0 occurrence.**

### Back

Une correspondance de regex a été détectée sur `WebSecurityConfig.java` mais il s'agit d'un **faux positif de recherche** : le motif `/\*` matchait la chaîne `/api/session/*` (wildcard de route Spring Security), pas un commentaire de bloc.

Plusieurs lignes `//` ont été repérées par la recherche automatisée dans les fichiers de tests d'intégration, mais elles sont toutes des **commentaires explicatifs légitimes** (titres de section ou justification de choix de test), pas du code mort :

| Fichier | Ligne | Extrait | Avis |
|---|---|---|---|
| `back/src/test/java/.../SessionControllerIT.java` | 145, 295, 392, 481, 563 | `// ---------- GET /api/session/{id} ----------` (et variantes) | **Faux positif légitime** — séparateurs de section entre groupes de tests, pas du code commenté. |
| `back/src/test/java/.../SessionControllerIT.java` | 449, 485 | Commentaires expliquant l'ordre des tests / un fix de contrôle de propriété | **Faux positif légitime** — documentation du "pourquoi" d'un test, conforme aux règles du projet. |
| `back/src/test/java/.../UserControllerIT.java` | 81, 150, 172 | `// ---------- GET /api/user/{id} ----------` et note sur un bug Postman du 24/07/2026 | **Faux positif légitime** — séparateur de section et contexte historique justifiant un test de régression. |
| `back/src/test/java/.../UserServiceTest.java` | 90 | Commentaire expliquant la logique de comparaison par id | **Faux positif légitime** — explique un choix de design, pas du code mort. |

**Conclusion catégorie 4** : 0 résidu de code mort commenté, dans les deux sous-projets.

---

## 5. Suppressions de règles de lint (inventaire, sans jugement de "problème")

### Front — `eslint-disable`

**0 occurrence** dans `front/src`.

### Back — `@SuppressWarnings`

**0 occurrence** dans `back/src/main/java` et `back/src/test/java`.

Aucune suppression de règle de lint à examiner — catégorie vide des deux côtés.

---

## Synthèse

| Catégorie | Front | Back | Résidu réel à nettoyer |
|---|---|---|---|
| Logs de debug actifs | 1 match (faux positif) | 0 | Aucun |
| Tests désactivés/isolés | 0 | 0 | Aucun |
| TODO/FIXME/XXX/HACK | 0 | 0 | Aucun |
| Code mort commenté | 0 | 0 (1 faux positif regex) | Aucun |
| Suppressions de lint | 0 | 0 | N/A |

**Conclusion générale** : la revérification confirme le constat de `AUDIT_DETTE_PHASE0.md` — **aucun résidu de debug ou marqueur de dette technique classique** n'est présent dans `front/src` ni dans `back/src` (main + test), malgré le volume de commits intervenus depuis la Phase 0. Le seul appel `console.error` trouvé est le handler d'erreur standard généré par Angular CLI dans `main.ts`, et la seule correspondance de "bloc commenté" détectée en back est un faux positif de regex sur une route wildcard Spring Security.
