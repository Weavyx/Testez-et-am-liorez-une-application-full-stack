# TESTING — aide-mémoire

Aide-mémoire des commandes de test pour une démo au mentor. Front (Angular/Jest/Cypress) et back (Spring Boot/Maven) — le back n'a pas encore de tests (Ex2-Back).

## Front (`front/`)

| Commande | Ce qu'elle fait | Résultat attendu |
|---|---|---|
| `npm test` | Lance toute la suite Jest (unitaire + intégration mélangés) | 65/65 tests verts |
| `npm run test:unit` | Filtre les tests dont le nom ne contient pas "intégration" (composants tagués unitaires + tous les fichiers services) | 41/65 tests |
| `npm run test:integration` | Filtre les tests dont le nom contient "intégration" (rendu DOM) | 24/65 tests |
| `npm run test:coverage` | Jest avec couverture (équivalent `npm test -- --coverage`) | 100% statements/branches/functions/lines, rapport dans `coverage/jest/lcov-report/index.html` |
| `npm run e2e` | Cypress en mode interactif, app servie via `ng e2e` | Sert de démo manuelle, pas de rapport de couverture |
| `npm run e2e:ci` | Cypress headless, app instrumentée Istanbul via `serve-coverage` | 36/36 tests verts |
| `npm run e2e:coverage` | Génère le rapport nyc à partir des données Istanbul collectées par `e2e:ci` | 95%/92%/92%/94% (statements/branches/functions/lines), rapport dans `coverage/lcov-report/index.html` |
| `npm run e2e:coverage-run` | Enchaîne `e2e:ci` puis `e2e:coverage` | Identique à lancer les deux commandes à la suite : 36/36 puis 95%/92%/92%/94% |
| `npm run cypress:open` / `npm run cypress:run` | Cypress direct, sans build instrumenté | Pas de couverture (pas de `devServerTarget` Angular) |

Prérequis pour `e2e*` : backend Spring Boot + MySQL (Docker) démarrés.

## Back (`back/`)

Aucun test n'existe encore (à écrire en Ex2-Back). Commandes Maven à utiliser une fois les premiers tests en place :

| Commande | Ce qu'elle fait | Résultat attendu |
|---|---|---|
| `mvn test` | Exécute tous les tests (unitaire + intégration mélangés, pas de séparation Failsafe pour l'instant), génère le rapport JaCoCo HTML | Rapport dans `target/site/jacoco/index.html` — **n'applique pas** le seuil de couverture |
| `mvn verify` | Identique à `mvn test`, plus l'exécution réelle du gate `jacoco-check` | Échoue si couverture < 90% sur un package (hors DTO, exclu) — commande à utiliser pour démontrer le seuil de couverture |
