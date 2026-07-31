# Yoga App

Application full-stack de gestion de sessions de yoga, développée dans le cadre d'un
projet OpenClassrooms de test et d'amélioration continue d'une application existante.
Le dépôt est composé de deux sous-projets :

- `back/` — API REST Spring Boot
- `front/` — application web Angular

## Stack technique

| Composant | Technologie | Version |
|---|---|---|
| Backend | Java | 21 |
| Backend | Spring Boot | 3.5.5 |
| Backend | Maven (parent) | via `spring-boot-starter-parent` 3.5.5 |
| Backend | Base de données | MySQL (image `mysql:latest`, via Docker Compose) |
| Backend | Sécurité | Spring Security + JWT (`io.jsonwebtoken` 0.12.6) |
| Backend | Tests | JUnit 5 (Spring Boot Starter Test), Testcontainers 1.21.4 (MySQL), JaCoCo 0.8.12 |
| Frontend | Angular | 19.2 |
| Frontend | Angular Material / CDK | 19.2 |
| Frontend | Tests unitaires/intégration | Jest 29 |
| Frontend | Tests E2E | Cypress 15, couverture via `nyc` 17 |

## Prérequis

- JDK 21
- Docker Desktop (démarré avant de lancer le back)
- Docker Compose (fourni avec Docker Desktop)
- Maven 3.9.3 ou plus récent
- Node.js et npm (aucune version minimale n'est imposée par le projet — un JS moderne
  compatible Angular 19 / Node 18+ convient)

## Installation et lancement

### Backend

```bash
cd back
mvn spring-boot:run
```

Cette commande initialise automatiquement, via `spring-boot-docker-compose` et le
fichier `back/compose.yaml`, un conteneur Docker MySQL (`back_mysql`), puis démarre
l'API sur `http://localhost:8080`. Les identifiants de connexion à la base sont lus
depuis `back/.env` (`DB_USER`, `DB_PASSWORD`, `DB_HOST`, `DB_PORT`, `DB_NAME`,
`TOKEN_SECRET`).

Aucun compte n'est créé automatiquement au démarrage : le schéma `users` est vide tant
qu'aucun utilisateur ne s'est inscrit ou que le compte admin de seed n'a pas été inséré
manuellement (voir [Comptes de test](#comptes-de-test)).

### Frontend

```bash
cd front
npm install
npm run start
```

L'application est servie sur `http://localhost:4200` et proxifie les appels `/api`
vers `http://localhost:8080` (voir `front/proxy.conf.json`).

### URLs

- Front : http://localhost:4200
- Back : http://localhost:8080

## Comptes de test

Un compte administrateur peut être créé en exécutant le script SQL fourni
(`back/src/main/resources/sql/insert_user.sql`) sur la base de données du conteneur
`back_mysql` :

```sql
INSERT INTO users(first_name, last_name, admin, email, password)
VALUES ('Admin', 'Admin', true, 'yoga@studio.com', '$2a$10$.Hsa/ZjUVaHqi0tp9xieMeewrnZxrZ5pQRzddUXE/WjDu2ZThe6Iq');
```

Pour l'exécuter : ouvrir Docker Desktop, accéder au conteneur `back_mysql`, onglet
`Exec`, puis :

```bash
mysql -u user_test -p        # mot de passe : test_password
use test;
# coller la requête INSERT ci-dessus
select * from users;         # vérification
```

Identifiants obtenus une fois le script exécuté :

- Email : `yoga@studio.com`
- Mot de passe : `test!1234`

## Exécution des tests

### Backend

```bash
cd back
mvn clean verify
```

`mvn test` seul exécute les tests mais ne suffit pas : le gate de couverture
(`jacoco-check`) est lié à l'exécution `check` du plugin JaCoCo, elle-même déclenchée
par la phase `verify` (et non `test`). Utiliser `mvn clean verify` pour que le build
échoue effectivement si le seuil de couverture n'est pas atteint.

Docker Desktop doit être actif : les tests d'intégration utilisent Testcontainers pour
démarrer un conteneur MySQL dédié.

Lancer une seule classe de test :

```bash
mvn test -Dtest=NomDeLaClasse
```

### Frontend — tests unitaires et d'intégration (Jest)

```bash
cd front
npm run test              # exécution complète
npm run test:watch        # mode watch
npm run test:coverage     # avec rapport de couverture
npm run test:unit         # sous-ensemble hors tests marqués "intégration"
npm run test:integration  # sous-ensemble des tests marqués "intégration"
```

### Frontend — tests E2E (Cypress)

Le backend doit être démarré au préalable (`mvn spring-boot:run`, Docker Desktop
actif), certains scénarios Cypress appelant la vraie API.

```bash
cd front
npm run cypress:open        # mode interactif
npm run cypress:run         # mode headless
npm run e2e:ci               # exécution headless via le runner Angular/Cypress (avec instrumentation de couverture)
npm run e2e:coverage         # génère le rapport de couverture à partir de l'exécution précédente
npm run e2e:coverage-run     # enchaîne e2e:ci puis e2e:coverage
```

## Rapports de couverture générés

| Périmètre | Chemin |
|---|---|
| Backend (JaCoCo) | `back/target/site/jacoco/index.html` |
| Frontend unitaire (Jest) | `front/coverage/jest/lcov-report/index.html` |
| Frontend E2E (Cypress/nyc) | `front/coverage/lcov-report/index.html` |

## Couverture atteinte

### Backend — JaCoCo (`mvn clean verify`, exécution du 2026-07-31)

Le gate `jacoco-check` du `pom.xml` vérifie, par package, LINE ≥ 90 %, INSTRUCTION ≥ 80 %
et BRANCH ≥ 80 %.

| Package | Instructions % | Branches % | Lignes % | Statut |
|---|---|---|---|---|
| security.services | 92,9 % | 100,0 % | 100,0 % | ✅ |
| services | 100,0 % | 100,0 % | 100,0 % | ✅ |
| security | 100,0 % | n/a (0 branche) | 100,0 % | ✅ |
| security.jwt | 100,0 % | 100,0 % | 100,0 % | ✅ |
| models | 96,3 % | 86,5 % | 100,0 % | ✅ |
| exception | 100,0 % | 100,0 % | 100,0 % | ✅ |
| controllers | 100,0 % | n/a (0 branche) | 100,0 % | ✅ |
| configuration | 100,0 % | n/a (0 branche) | 100,0 % | ✅ |
| **Global (périmètre `jacoco-check`)** | **97,7 % (1994/2040)** | **92,1 % (116/126)** | **100,0 % (297/297)** | ✅ |

170 tests exécutés, 0 échec. `[INFO] All coverage checks have been met.` / `BUILD SUCCESS`.
Périmètre hors `dto/`, `mapper/`, `payload/request/`, `payload/response/` et la classe de
bootstrap (exclusions explicites du build).

Chiffres front et e2e ci-dessous issus de la dernière mesure documentée dans
`AUDIT_PHASE5_COUVERTURE_FINALE.md` (exécution du 2026-07-26).

### Frontend — Jest

| Indicateur | Couverture | Seuil | Statut |
|---|---|---|---|
| Statements | 100 % | 80 % | ✅ |
| Branches | 100 % | 80 % | ✅ |
| Functions | 100 % | 80 % | ✅ |
| Lines | 100 % | 80 % | ✅ |

65 tests exécutés, 0 échec.

### E2E — Cypress/nyc

| Indicateur | Couverture | Seuil | Statut |
|---|---|---|---|
| Statements | 95,13 % (176/185) | 80 % | ✅ |
| Branches | 92,3 % (48/52) | 80 % | ✅ |
| Functions | 92,2 % (71/77) | 80 % | ✅ |
| Lines | 94,64 % (159/168) | 80 % | ✅ |

37 tests exécutés (9 specs), 0 échec.

## Structure du projet

```
back/
├── src/main/java/com/openclassrooms/starterjwt/
│   ├── controllers/
│   ├── services/
│   ├── repository/
│   ├── models/
│   ├── dto/
│   ├── mapper/
│   ├── payload/
│   ├── security/
│   └── exception/
├── src/main/resources/
├── src/test/java/
├── postman/
└── pom.xml

front/
├── src/app/
│   ├── core/
│   ├── pages/
│   ├── shared/
│   └── guards/interceptors
├── cypress/
├── coverage/
└── package.json
```

## Documentation complémentaire

La démarche qualité menée sur ce projet va au-delà du minimum demandé : `METHODE_AUDIT.md`
décrit la méthode d'audit suivie, `DETTE_ET_SUIVI.md` trace la dette technique identifiée
et son traitement, et les fichiers `AUDIT_*.md` à la racine documentent chaque phase
d'analyse (inventaire, classement des tests, couverture, etc.).

## Ressources

### Collection Postman

Importer la collection Postman :

> `back/postman/yoga.postman_collection.json`

Documentation Postman pour l'import :
https://learning.postman.com/docs/getting-started/importing-and-exporting-data/#importing-data-into-postman
