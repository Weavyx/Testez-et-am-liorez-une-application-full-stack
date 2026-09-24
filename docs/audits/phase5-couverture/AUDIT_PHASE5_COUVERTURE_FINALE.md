# Audit Phase 5 — Couverture finale, ratio d'intégration, synthèse

Exécution fraîche, réalisée le **2026-07-26** (horodatages des commandes ci-dessous). Périmètre : back (Jacoco), front (Jest), e2e (Cypress/nyc). Aucun fichier de code ni de test n'a été modifié — seules les commandes de build/test existantes ont été exécutées.

---

## 1. Back — Jacoco

### Exécution

```
mvn clean verify
```

- Lancée le **2026-07-26 à 11:03:06**, terminée à **11:03:45** (37,267 s).
- `[INFO] BUILD SUCCESS`
- `[INFO] --- jacoco:0.8.12:check (jacoco-check) @ yoga-app ---` → `[INFO] All coverage checks have been met.`
- **Tests run: 126, Failures: 0, Errors: 0, Skipped: 0**

### Seuil réellement appliqué par le projet (rappel de configuration)

`back/pom.xml` (règle `jacoco-check`) : règle **PACKAGE / LINE ≥ 90 %**, uniquement sur le compteur de lignes, avec exclusion explicite de `dto/**`, `mapper/**`, `payload/request/**`, `payload/response/**` et de la classe de bootstrap `SpringBootSecurityJwtApplication`. Il n'y a **aucune règle Jacoco sur les instructions ou les branches** dans ce projet — c'est le seuil qui fait foi pour le `BUILD SUCCESS` ci-dessus.

Le prompt d'audit demande un contrôle **≥80 % par indicateur (instructions/branches/lignes) et par package**. Ce seuil est différent du seuil réellement configuré (90 % lignes uniquement). Les deux tableaux ci-dessous appliquent chacun leur propre seuil, pour ne rien masquer.

### 1.1 Tableau — seuil réel du projet (LINE ≥ 90 % par package, hors dto/mapper/payload/bootstrap)

| Package | Line % | Seuil | Statut |
|---|---|---|---|
| security.services | 100,0 % | 90 % | ✅ |
| services | 100,0 % | 90 % | ✅ |
| security | 100,0 % | 90 % | ✅ |
| security.jwt | 100,0 % | 90 % | ✅ |
| models | 93,0 % | 90 % | ✅ |
| exception | 100,0 % | 90 % | ✅ |
| controllers | 100,0 % | 90 % | ✅ |
| configuration | 100,0 % | 90 % | ✅ |
| **Global (périmètre `jacoco-check`)** | **99,0 % (298/301 lignes)** | 90 % | ✅ |

Tous les packages dans le périmètre du `jacoco-check` sont conformes ; c'est ce qui explique le `BUILD SUCCESS`.

### 1.2 Tableau — seuil demandé par ce prompt d'audit (≥80 % par indicateur ET par package, instructions/branches/lignes), même périmètre (hors dto/mapper/payload/bootstrap)

| Package | Instructions % | Statut | Branches % | Statut | Lignes % | Statut |
|---|---|---|---|---|---|---|
| security.services | 92,9 % | ✅ | 100,0 % | ✅ | 100,0 % | ✅ |
| services | 100,0 % | ✅ | 100,0 % | ✅ | 100,0 % | ✅ |
| security | 100,0 % | ✅ | n/a (0 branche) | — | 100,0 % | ✅ |
| security.jwt | 100,0 % | ✅ | **75,0 %** | ❌ | 100,0 % | ✅ |
| models | **49,8 %** | ❌ | **21,6 %** | ❌ | 93,0 % | ✅ |
| exception | 100,0 % | ✅ | **75,0 %** | ❌ | 100,0 % | ✅ |
| controllers | 100,0 % | ✅ | n/a (0 branche) | — | 100,0 % | ✅ |
| configuration | 100,0 % | ✅ | n/a (0 branche) | — | 100,0 % | ✅ |
| **Global** | **78,2 %** | ❌ | **51,6 %** | ❌ | **99,0 %** | ✅ |

**Écarts précis au seuil de 80 % (pour arbitrage, non corrigés) :**
- Instructions globales : 78,2 % — écart **1,8 point** sous 80 %.
- Instructions, package `models` : 49,8 % — écart **30,2 points** sous 80 %. Porté principalement par `User`, `Session`, `Teacher` et leurs builders Lombok (`equals`/`hashCode`/`toString`/builder generés, non exercés en détail par les tests métier).
- Branches globales : 51,6 % — écart **28,4 points** sous 80 %.
- Branches, package `models` : 21,6 % — écart **58,4 points** sous 80 %.
- Branches, package `security.jwt` : 75,0 % — écart **5 points** sous 80 %.
- Branches, package `exception` : 75,0 % — écart **5 points** sous 80 %.

Si le rapport (couverture globale, tous packages du bundle, y compris `dto/mapper/payload` hors périmètre testé par consigne) est pris en compte sans aucune exclusion : instructions 52,9 % (2140/4049), branches 18,3 % (80/436), lignes 86,0 % (410/477) — ces chiffres n'ont pas de valeur de seuil ici (les DTO/mappers/payloads sont hors périmètre testé par consigne de l'énoncé) mais sont donnés pour traçabilité complète.

### 1.3 Ratio de tests d'intégration (back)

Base : `AUDIT_PHASE3_CLASSEMENT_TESTS.md` — 125 tests classés, 61 intégration réels (48,8 %).
Ajustement Phase 4 : `P4-02` (`DETTE_ET_SUIVI.md`) a ajouté un test d'intégration supplémentaire dans `SessionControllerIT` : `delete_returns200AndClearsParticipations_whenSessionHasParticipants`. Confirmé par le nombre total de tests observé à l'exécution (**126**, contre 125 en Phase 3).

| | Valeur |
|---|---|
| Total tests back (exécution fraîche) | 126 |
| Tests d'intégration (base Phase 3 + P4-02) | 62 |
| **Ratio d'intégration back** | **62 / 126 = 49,2 %** |
| Seuil | ≥ 30 % |
| Statut | ✅ |

---

## 2. Front — Jest (unitaire + intégration)

### Exécution

```
npm test -- --coverage
```

- Lancée et terminée le **2026-07-26 vers 11:07:02 – 11:07:14** (Time: 11,863 s).
- `Test Suites: 13 passed, 13 total`
- `Tests: 65 passed, 65 total`

### Couverture globale (« All files »)

| Indicateur | Couverture | Seuil | Statut |
|---|---|---|---|
| Statements | 100 % | 80 % | ✅ |
| Branches | 100 % | 80 % | ✅ |
| Functions | 100 % | 80 % | ✅ |
| Lines | 100 % | 80 % | ✅ |

Tous les répertoires listés dans le rapport (`app`, `app/components/me`, `app/core/service`, `app/pages/login`, `app/pages/not-found`, `app/pages/register`, `app/pages/sessions/components/{detail,form,list}`, `app/shared`) affichent 100 % sur les quatre indicateurs — aucune ligne non couverte.

### Ratio de tests d'intégration (front)

Base : `AUDIT_PHASE3_CLASSEMENT_TESTS.md` — 65 tests classés, 34 intégration réels (52,3 %). Aucun test front n'a été ajouté en Phase 4 (seul `P4-02`, côté back, a ajouté un test — confirmé dans `DETTE_ET_SUIVI.md`). Le nombre total de tests exécuté (**65**) confirme l'absence de changement côté front depuis la Phase 3.

| | Valeur |
|---|---|
| Total tests front (exécution fraîche) | 65 |
| Tests d'intégration (base Phase 3, inchangée) | 34 |
| **Ratio d'intégration front** | **34 / 65 = 52,3 %** |
| Seuil | ≥ 30 % |
| Statut | ✅ |

---

## 3. E2E — Cypress/nyc

### Exécution

```
npm run e2e:coverage-run   # = npm run e2e:ci && npm run e2e:coverage
```

Backend démarré manuellement au préalable (`mvn spring-boot:run`, Docker Desktop actif, conteneur MySQL déjà présent) pour que les scénarios « real backend » des specs Cypress puissent aboutir.

**Remarque anti-régression** : la première tentative (11:07:48) a échoué à démarrer le serveur de dev (`Port 4200 is already in use` — un processus Angular résiduel d'une session précédente occupait le port) et la commande `nyc report` a néanmoins produit un résultat en se basant sur un rapport de coverage périmé du 21/07. Ce résultat périmé a été écarté. Le port 4200 a été libéré (processus résiduel arrêté), `.nyc_output/out.json` et `coverage/*` ont été purgés, puis la suite a été **relancée intégralement** à 11:08:39. Les chiffres ci-dessous proviennent de cette seconde exécution, confirmée fraîche (fichiers `.nyc_output/out.json` et `coverage/coverage-summary.json` horodatés 2026-07-26 11:10).

- `✔ All specs passed! 00:54 — 37 tests, 37 passing, 0 failing`
- 9 fichiers de specs exécutés : `account.cy.ts`, `login.cy.ts`, `logout.cy.ts`, `register.cy.ts`, `sessions-create.cy.ts`, `sessions-delete.cy.ts`, `sessions-detail.cy.ts`, `sessions-list.cy.ts`, `sessions-update.cy.ts`.

### Couverture (`npx nyc report`, instrumentation `serve-coverage`)

| Indicateur | Couverture | Seuil | Statut |
|---|---|---|---|
| Statements | 95,13 % (176/185) | 80 % | ✅ |
| Branches | 92,3 % (48/52) | 80 % | ✅ |
| Functions | 92,2 % (71/77) | 80 % | ✅ |
| Lines | 94,64 % (159/168) | 80 % | ✅ |

### Écrans / parcours couverts

9 specs, 37 tests, couvrant : connexion (login), inscription (register), déconnexion (logout), compte utilisateur (account/me), liste des sessions, détail de session (participate/unParticipate), création de session, modification de session, suppression de session — chaque spec combinant des scénarios mockés (`cy.intercept`) et au moins un scénario « real backend » de bout en bout.

---

## 4. Synthèse globale

| Partie | Statements/Instructions | Branches | Functions/Méthodes | Lines | Ratio intégration (≥30 %) | Statut global |
|---|---|---|---|---|---|---|
| Back — seuil réel du projet (LINE 90 %/package, hors dto/mapper/payload) | — | — | — | 99,0 % (✅, tous packages ≥ 90 %) | 49,2 % ✅ | ✅ conforme au gate réel (`BUILD SUCCESS`) |
| Back — seuil ≥80 % de ce prompt (instr./branches/lignes, même périmètre) | 78,2 % ❌ | 51,6 % ❌ | — | 99,0 % ✅ | 49,2 % ✅ | ❌ non conforme sur instructions et branches |
| Front — Jest | 100 % ✅ | 100 % ✅ | 100 % ✅ | 100 % ✅ | 52,3 % ✅ | ✅ conforme |
| E2E — Cypress/nyc | 95,13 % ✅ | 92,3 % ✅ | 92,2 % ✅ | 94,64 % ✅ | n/a | ✅ conforme |

### Conclusion

- **Front, e2e et le gate Jacoco réellement configuré côté back sont conformes** : `mvn clean verify` se termine en `BUILD SUCCESS` (126/126 tests verts, seuil de 90 % de lignes par package tenu), `npm test -- --coverage` est à 100 % sur les quatre indicateurs (65/65 tests verts), et l'exécution e2e fraîche est à 37/37 tests verts avec une couverture ≥92 % sur tous les indicateurs. Les deux ratios d'intégration (back 49,2 %, front 52,3 %) restent largement au-dessus du seuil de 30 %, en légère hausse côté back par rapport à la Phase 3 (48,8 % → 49,2 %) grâce au test `P4-02`, et inchangés côté front.
- **Point de non-conformité, à arbitrer en conversationnel** : si l'on applique littéralement le seuil ≥80 % par indicateur *et par package* demandé dans ce prompt au périmètre back testé par consigne (hors DTO/mappers/payloads), deux indicateurs ne l'atteignent pas :
  - **Instructions** : 78,2 % global (écart 1,8 point), tiré vers le bas par le package `models` (49,8 %, écart 30,2 points).
  - **Branches** : 51,6 % global (écart 28,4 points), avec `models` à 21,6 % (écart 58,4 points), et `security.jwt`/`exception` à 75 % chacun (écart 5 points).
  Ce point ne remet pas en cause le `BUILD SUCCESS` ni le seuil noté explicitement documenté dans `CLAUDE.md`/`back/pom.xml` (LINE ≥90 % par package, sans règle sur instructions/branches) — c'est un écart entre deux définitions de seuil coexistant dans les documents du projet, à trancher en conversationnel (aucune correction appliquée ici).

## Vérification anti-régression

```
git status --porcelain
```

Résultat obtenu (avant ajout de ce fichier) : **vide** — aucun fichier de code, test ou configuration modifié pendant cette phase. `target/` (back) et `coverage/` (front) sont bien listés dans `.gitignore` (`back/.gitignore` : `target/` ; `front/.gitignore` : `/coverage`) et n'apparaissent pas comme untracked. Seul `AUDIT_PHASE5_COUVERTURE_FINALE.md` est un fichier nouveau à ce stade.
