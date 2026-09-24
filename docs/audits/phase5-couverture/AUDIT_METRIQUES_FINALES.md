# Audit métriques finales — branche `chore/verification-finale-livrable-p4`

> Collecte read-only, aucune modification de fichier de code ni de test. Chiffres issus
> d'exécutions réelles des commandes, pas d'estimation ni de report des anciens chiffres du
> README. Date de collecte : 2026-08-07.

---

## 1. Back — `mvn clean verify`

### Commande et résultat

```
cd back && mvn clean verify
```

```
[INFO] Results:
[INFO]
[INFO] Tests run: 180, Failures: 0, Errors: 0, Skipped: 0
...
[INFO] --- jacoco:0.8.12:check (jacoco-check) @ yoga-app ---
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

- **180 tests**, **0 échec**, **0 erreur**, **0 skip**.
- Gate JaCoCo (**90% lignes par package**) : `All coverage checks have been met.` → **PASS**.
- `BUILD SUCCESS`.

### Couverture JaCoCo — globale

Source : `back/target/site/jacoco/jacoco.csv` (agrégation manuelle des colonnes
`INSTRUCTION_MISSED/COVERED`, `BRANCH_MISSED/COVERED`, `LINE_MISSED/COVERED` sur les 24 classes
du rapport).

| Indicateur | Manqué | Couvert | Total | % |
|---|---|---|---|---|
| Instructions | 46 | 2020 | 2066 | **97,77 %** |
| Branches | 10 | 116 | 126 | **92,06 %** |
| Lignes | 0 | 305 | 305 | **100,00 %** |

### Couverture JaCoCo — par package

| Package | Instructions | Branches | Lignes |
|---|---|---|---|
| `security.jwt` | 100,00 % (173/173) | 100,00 % (8/8) | 100,00 % (43/43) |
| `models` | 96,26 % (824/856) | 86,49 % (64/74) | 100,00 % (43/43) |
| `security.services` | 92,89 % (183/197) | 100,00 % (8/8) | 100,00 % (36/36) |
| `exception` | 100,00 % (70/70) | 100,00 % (4/4) | 100,00 % (17/17) |
| `controllers` | 100,00 % (243/243) | 100,00 % (0/0 — pas de branche) | 100,00 % (54/54) |
| `services` | 100,00 % (388/388) | 100,00 % (32/32) | 100,00 % (83/83) |
| `configuration` | 100,00 % (15/15) | 100,00 % (0/0) | 100,00 % (4/4) |
| `security` (WebSecurityConfig) | 100,00 % (124/124) | 100,00 % (0/0) | 100,00 % (25/25) |

Tous les packages sont ≥ 90 % en lignes (seuil du gate), la plupart à 100 %. Le seul point sous
100 % en lignes n'existe pas — la ligne globale à 100 % confirme qu'aucune ligne exécutable
n'est manquée nulle part dans le bundle analysé par JaCoCo.

---

## 2. Back — recalcul du ratio d'intégration

### Vérification de la classification (méthodologie `METHODE_AUDIT.md` §Phase 3 / Passe 2)

Critère : **intégration** = contexte Spring démarré **ET** ≥ 2 couches réelles traversées
(controller → service → repository/BDD réels) ; **unitaire** = tous les collaborateurs mockés.
Convention de classement par nom de fichier confirmée sans écart dans
`AUDIT_PHASE3_CLASSEMENT_TESTS.md` (à une exception près, déjà résolue — voir ci-dessous) :
`*IT.java` → intégration déclaré, `*Test.java` → unitaire déclaré.

**Tests migrés vers `@WithMockUser`** : recherche exhaustive de `@MockBean`, `@MockitoBean`,
`@WebMvcTest`, `@DataJpaTest` sur `back/src/test/java/**` → aucune occurrence (déjà constaté
dans `AUDIT_PHASE3_CLASSEMENT_TESTS.md §Point de vigilance`). Les fichiers `*ControllerIT.java`
étendent tous `AbstractIntegrationTest` (`@SpringBootTest(RANDOM_PORT)` + Testcontainers MySQL
réel + `@AutoConfigureMockMvc`). `@WithMockUser` ne remplace que le mécanisme d'obtention du
principal Spring Security dans la requête simulée — `AuthTokenFilter` réel, `UserDetailsServiceImpl`
réel et `UserRepository`/MySQL réels restent traversés pour les scénarios qui authentifient
réellement en base au préalable, et le contexte Spring + Testcontainers reste démarré dans tous
les cas. **Confirmé : classement intégration inchangé.**

**Les 2 nouveaux tests JWT invalide/expiré** — vérifiés dans le code source :

```
back/src/test/java/com/openclassrooms/starterjwt/controllers/SessionControllerIT.java:219
    void findAll_returns401_whenTokenIsMalformed() throws Exception {
back/src/test/java/com/openclassrooms/starterjwt/controllers/SessionControllerIT.java:231
    void findAll_returns401_whenTokenIsExpired() throws Exception {
```

Fichier `*IT.java`, classe étendant `AbstractIntegrationTest` (contexte Spring + Testcontainers
MySQL réels), requête MockMvc traversant `AuthTokenFilter` réel avant rejet. → **Classés
intégration.**

`ApplicationContextTest.java`, seul écart relevé dans l'audit Phase 3 (nom `*Test` mais
`@SpringBootTest` + Testcontainers réels), a depuis été renommé `ApplicationContextIT.java`
(cohérent avec l'historique de commits de la branche) : l'écart nom/classement réel n'existe
plus, la convention par nom de fichier est désormais fiable à 100 % pour dénombrer le ratio.

### Décompte actuel (post-branche)

Source : `back/target/surefire-reports/*.txt` (`Tests run: N -- in <classe>`), recoupé avec un
comptage `@Test` par fichier sur `back/src/test/java/**`.

**Tests classés intégration (`*IT.java`)**

| Fichier | Tests |
|---|---|
| ApplicationContextIT | 1 |
| AuthControllerIT | 10 |
| TeacherControllerIT | 6 |
| UserControllerIT | 11 |
| SessionControllerIT | 37 |
| **Total intégration** | **65** |

**Tests classés unitaires (`*Test.java`)**

| Fichier | Tests |
|---|---|
| UserServiceTest | 20 |
| SessionServiceTest | 19 |
| TeacherServiceTest | 4 |
| SessionTest | 10 |
| TeacherTest | 10 |
| UserTest | 23 |
| GlobalExceptionHandlerTest | 3 |
| UserDetailsServiceImplTest | 2 |
| UserDetailsImplTest | 12 |
| JwtUtilsTest | 9 |
| AuthTokenFilterTest | 3 |
| **Total unitaire** | **115** |

Vérification : 65 + 115 = **180**, égal au total Surefire (`Tests run: 180`).

### Ratio recalculé

```
Ratio intégration = 65 / 180 × 100 = 36,11 %
```

**Ratio d'intégration back : 36,11 %** — au-dessus du seuil de 30 % retenu comme critère noté
dans `METHODE_AUDIT.md`.

---

## 3. Front — couverture Jest et E2E

### 3.1 Jest — `npm run test:coverage`

```
Test Suites: 13 passed, 13 total
Tests:       65 passed, 65 total
Snapshots:   0 total
```

Couverture (`All files` — table de synthèse imprimée par `jest --coverage`) :

| Indicateur | % |
|---|---|
| Statements | **100 %** |
| Branch | **100 %** |
| Functions | **100 %** |
| Lines | **100 %** |

100 % sur les quatre indicateurs, sur tous les répertoires listés (`app`, `app/components/me`,
`app/core/service`, `app/pages/login`, `app/pages/not-found`, `app/pages/register`,
`app/pages/sessions/components/{detail,form,list}`, `app/shared`).

*Note : un warning jsdom `[warning] Error: Could not parse CSS stylesheet` (parsing CSS des
overlays Angular Material dans `form.component.spec.ts`) apparaît dans le log — c'est un warning
de rendu jsdom, sans échec de test associé (`Tests: 65 passed, 65 total`).*

### 3.2 Cypress E2E — `npm run e2e:coverage-run`

**Prérequis d'infrastructure** : Docker actif (conteneur `back_mysql` déjà levé), backend déjà
démarré et répondant sur `:8080` (vérifié via `curl` avant lancement). Le port 4200 était occupé
par un `ng serve` non instrumenté (aucun marqueur `cov_` dans le bundle) — arrêté après
confirmation utilisateur pour permettre à `e2e:ci` (cible Angular `serve-coverage`, build
instrumenté Istanbul) de démarrer son propre serveur sur ce port. `.nyc_output` préexistant
(données antérieures à cette conversation) supprimé avant réexécution pour garantir un rapport
frais.

```
Run Finished
account.cy.ts        4/4
login.cy.ts          4/4
logout.cy.ts         3/3
register.cy.ts       4/4
sessions-create.cy.ts       4/4
sessions-delete.cy.ts       3/3
sessions-detail.cy.ts       6/6
sessions-list.cy.ts         5/5
sessions-update.cy.ts       4/4

All specs passed!   00:53   37   37   -   -   -
```

**37 tests Cypress**, **37 passing**, **0 failing**.

Couverture nyc (`npx nyc report --reporter=text-summary`, exécuté juste après le run) :

```
=============================== Coverage summary ===============================
Statements   : 95.13% ( 176/185 )
Branches     : 92.3% ( 48/52 )
Functions    : 92.2% ( 71/77 )
Lines        : 94.64% ( 159/168 )
================================================================================
```

| Indicateur | % | Détail |
|---|---|---|
| Statements | **95,13 %** | 176/185 |
| Branches | **92,3 %** | 48/52 |
| Functions | **92,2 %** | 71/77 |
| Lines | **94,64 %** | 159/168 |

Rapport HTML détaillé : `front/coverage/lcov-report/index.html`.

---

## Synthèse

| Volet | Résultat |
|---|---|
| Back — build/tests | 180 tests, 0 échec, `BUILD SUCCESS`, gate JaCoCo (90 % lignes/package) : PASS |
| Back — couverture globale | Instructions 97,77 % · Branches 92,06 % · **Lignes 100 %** |
| Back — ratio intégration | **65/180 = 36,11 %** (seuil ≥ 30 % respecté) |
| Front — Jest | 65 tests, 0 échec, **100 %** sur les 4 indicateurs |
| Front — Cypress E2E | 37 tests, 0 échec, couverture nyc : Statements 95,13 % · Branches 92,3 % · Functions 92,2 % · Lines 94,64 % |

Aucune commande n'a échoué. Ces chiffres sont prêts à être injectés dans le README lors d'un
prompt séparé.
