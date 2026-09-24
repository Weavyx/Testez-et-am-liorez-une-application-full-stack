# Audit dette technique — Phase 0 (inventaire brut, read-only)

Périmètre : `front/` et `back/` (hors `node_modules`, `dist`, `target`, `.git` — exclus
automatiquement via `.gitignore` par les outils de recherche utilisés). Recherche
exhaustive, aucune modification de fichier effectuée.

## Marqueurs de code

| Marqueur | Fichier | Ligne | Extrait |
|---|---|---|---|
| TODO / FIXME / XXX / HACK | — | — | Aucune occurrence trouvée dans `front/` ou `back/` (hors code source applicatif). Deux faux positifs écartés car hors périmètre : `package-lock.json:20686` (sous-chaîne `XXX` dans un hash SHA-512, pas un marqueur) et `METHODE_AUDIT.md:122` (le mot `TODO` apparaît dans la description de la méthode d'audit elle-même, pas dans du code). |
| @Disabled / @Ignore (JUnit) | — | — | Aucune occurrence trouvée dans `back/src`. |
| .skip / xit / xdescribe (Jest/Cypress) | — | — | Aucune occurrence trouvée dans `front/src` ni `front/cypress`. |
| eslint-disable | — | — | Aucune occurrence trouvée dans `front/`. |
| @SuppressWarnings | — | — | Aucune occurrence trouvée dans `back/src`. |
| Blocs catch vides | — | — | Aucune occurrence trouvée. Tous les blocs `catch` recensés dans `back/src` (`JwtUtils.java` lignes 46-56 : `SignatureException`, `MalformedJwtException`, `ExpiredJwtException`, `UnsupportedJwtException`, `IllegalArgumentException` ; `AuthTokenFilter.java` ligne 46 : `Exception`) contiennent un appel `log.error(...)`/`logger.error(...)`, aucun n'est vide. Côté `front/src`, seule occurrence du mot « catch » : `main.ts:6` — `.catch((err) => console.error(err))`, qui n'est pas un bloc try/catch vide (gestion de promesse avec log). |
| `: any` résiduel (hors `*.spec.ts`, `*.cy.ts`) | — | — | Aucune occurrence trouvée dans `front/src`. |
| `<any>` résiduel (hors `*.spec.ts`, `*.cy.ts`) | — | — | Aucune occurrence trouvée dans `front/src`. |
| Exclusions de couverture — `front/jest.config.js` | front/jest.config.js | 12 | `coveragePathIgnorePatterns: ['<rootDir>/node_modules/']` — seule exclusion présente, portée standard (node_modules), pas de `collectCoverageFrom` avec pattern `!...` dans ce fichier. |
| Exclusions de couverture — `front/cypress.config.ts` | — | — | Aucune configuration de couverture / exclusion trouvée dans ce fichier (pas de section coverage, pas d'`nyc`/`istanbul` config inline). |
| Exclusions de couverture — `back/pom.xml` (jacoco-check) | back/pom.xml | 201-206 | Bloc `<excludes>` de l'exécution `jacoco-check` : `com/openclassrooms/starterjwt/dto/**`, `com/openclassrooms/starterjwt/mapper/**`, `com/openclassrooms/starterjwt/payload/request/**`, `com/openclassrooms/starterjwt/payload/response/**`, `com/openclassrooms/starterjwt/SpringBootSecurityJwtApplication.class` (5 exclusions), avec seuil `<minimum>0.9</minimum>` (LINE/COVEREDRATIO, élément PACKAGE) toujours actif à côté. |

## Historique git — messages de commit

Recherche `git log --all --oneline -i --grep=<mot-clé>` exécutée séparément pour
chacun des 5 mots-clés (`wip`, `temp`, `later`, `todo`, `revert`), sur tous les refs
(`--all`, y compris `refs/stash`).

| Hash | Date | Message |
|---|---|---|
| 1ac8374 | 2026-07-24 12:59:14 +0200 | `WIP on main: 2afd8e3 docs: ajoute la méthode d'audit en 5 phases (METHODE_AUDIT.md)` — **N'est pas un commit d'historique de branche** : c'est l'entrée `refs/stash` correspondant au stash courant (`git stash list` → `stash@{0}`), remontée uniquement parce que `--all` inclut les stash refs. Ne représente pas un compromis passé. |
| 1db8cb7 | 2026-06-26 15:24:48 +0200 | `refactor(app): migrate *ngIf/else + ng-template to @if/@else control flow` — faux positif sur le mot-clé `temp` : c'est une sous-chaîne de `ng-**temp**late` dans le message, pas le mot « temp » lui-même. Aucun compromis. |
| — | — | Mot-clé `later` : aucune occurrence trouvée. |
| — | — | Mot-clé `todo` : aucune occurrence trouvée. |
| — | — | Mot-clé `revert` : aucune occurrence trouvée. |

## Historique git — diffs de configuration

Commandes exécutées avec `--all` (le HEAD courant du dépôt est sur `main`, qui
n'inclut pas encore les commits de la branche `feature/ex2-back-unit-tests` où se
trouve la modification la plus récente du seuil Jacoco ; `--all` a donc été
nécessaire pour être exhaustif conformément à la consigne) :
- `git log --all -p -- back/pom.xml`
- `git log --all -p -- front/jest.config.js front/jest.config.ts` (seul `jest.config.js` existe dans le repo)
- `git log --all -p -- front/cypress.config.ts`

| Hash | Date | Fichier | Ce qui a changé (résumé 1 ligne) | Extrait de diff |
|---|---|---|---|---|
| aff4d62 | 2026-07-21 14:51:27 +0200 | back/pom.xml | Première exclusion Jacoco : ajout de 4 `<exclude>` (dto/mapper/payload) dans le check à 90 % | `+ <excludes>`<br>`+ <exclude>com/openclassrooms/starterjwt/dto/**</exclude>`<br>`+ <exclude>com/openclassrooms/starterjwt/mapper/**</exclude>`<br>`+ <exclude>com/openclassrooms/starterjwt/payload/request/**</exclude>`<br>`+ <exclude>com/openclassrooms/starterjwt/payload/response/**</exclude>`<br>`+ </excludes>` |
| d384f93 | 2026-07-24 11:58:30 +0200 | back/pom.xml | Deuxième exclusion Jacoco : ajout d'un 5e `<exclude>` ciblant la classe de bootstrap Spring Boot | `+ <exclude>com/openclassrooms/starterjwt/SpringBootSecurityJwtApplication.class</exclude>` |

**Précisions read-only sur ce qui n'a PAS été trouvé** (conformément à la consigne
d'exhaustivité, indiqué explicitement plutôt qu'omis) :
- Aucun changement de la valeur `<minimum>` dans le bloc `jacoco-check` sur toute
  l'histoire du repo : elle vaut `0.9` depuis le premier commit (`8f2a3c7`) et n'a
  jamais été modifiée (seul le numéro de version du plugin `jacoco-maven-plugin` a
  changé, `0.8.5` → `0.8.12`, commit `834ae70`, pour une raison de compatibilité
  bytecode Java 21 explicitée dans le message de commit — pas un assouplissement du
  contrôle).
- Aucune désactivation de plugin ou d'`<execution>` Jacoco trouvée sur toute
  l'histoire (`prepare-agent`, `report` et `jacoco-check` sont présents et actifs
  depuis le premier commit).
- `front/jest.config.js` : aucune modification depuis le premier commit (`8f2a3c7`,
  2025-10-14). Le seuil `coverageThreshold.global.statements` vaut `80` depuis
  l'origine, jamais changé. Aucun `collectCoverageFrom` avec pattern d'exclusion
  (`!...`) n'a jamais été ajouté — la clé `collectCoverageFrom` n'existe pas dans ce
  fichier, à aucun moment de l'historique.
- `front/cypress.config.ts` : aucune modification depuis le premier commit
  (`8f2a3c7`, 2025-10-14). Aucun seuil ni exclusion de coverage e2e n'a jamais été
  ajouté à ce fichier.

---

## Résumé (5-6 lignes)

Aucun marqueur de dette « classique » trouvé dans le code (TODO/FIXME/XXX/HACK,
`@Disabled`/`@Ignore`, tests désactivés `.skip`/`xit`/`xdescribe`,
`eslint-disable`, `@SuppressWarnings`, catch vides, `any` résiduel) : 0 occurrence
sur les 9 catégories recherchées. Le seul compromis structurel identifié est
l'exclusion de 5 périmètres du contrôle de couverture Jacoco backend (90 % ligne,
niveau PACKAGE) : `dto/**`, `mapper/**`, `payload/request/**`, `payload/response/**`
(commit `aff4d62`, 2026-07-21) et la classe de bootstrap `SpringBootSecurityJwtApplication`
(commit `d384f93`, 2026-07-24) — dans les deux cas justifié en message de commit
(POJOs générés sans logique métier, classe `main()` sans branche testable) et le
seuil de 90 % lui-même n'a jamais été abaissé sur toute l'histoire du repo. Côté
frontend, `jest.config.js` (seuil 80 % statements) et `cypress.config.ts` sont
inchangés depuis le premier commit — aucune exclusion de coverage n'y a jamais été
introduite. Les 5 recherches de mots-clés dans les messages de commit (`wip`,
`temp`, `later`, `todo`, `revert`) n'ont produit que 2 faux positifs sans rapport
(un stash `refs/stash`, une sous-chaîne dans « ng-template »).
