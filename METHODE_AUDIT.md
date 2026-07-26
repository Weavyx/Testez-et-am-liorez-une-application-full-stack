# Méthode d'audit — qualité métier, dette technique, qualité des tests

> Fichier de référence pour les phases d'audit post-Ex2. Stable, versionné, à citer dans les prompts Claude Code (« applique la grille définie dans METHODE_AUDIT.md »). Complète PLAN_DE_TRAVAIL.md et CONTEXTE_PROJET.md.

## Pourquoi cette méthode existe

Ex1 et Ex2 ont été réalisés à un rythme soutenu (prompts Claude Code enchaînés, un service/controller après l'autre). Cela a permis d'avancer vite, mais expose à trois risques non vérifiés :

1. **Bugs métier non détectés** : les corrections apportées l'ont été de façon incidente (ce que Claude Code a remarqué en écrivant un test), pas via une revue volontaire et exhaustive. Le starter OpenClassrooms contient intentionnellement de mauvais patterns, difficiles à repérer sans expertise Angular/Spring confirmée.
2. **Dette oubliée** : des points repérés au fil des sessions ont été reportés « à plus tard » sans trace centralisée.
3. **Qualité des tests jamais auditée a posteriori** : nommage, classement unitaire/intégration, pertinence des cas couverts.

Principe directeur : **un test vert qui verrouille un bug est plus coûteux qu'un bug nu** — il transforme une anomalie en spécification implicite. Corriger le métier après coup fait payer deux fois (code + test), avec un doute sur lequel des deux avait raison. **Le métier s'audite avant la qualité des tests.**

## Ordre des phases

| Phase | Contenu | Dépend du métier ? | Statut |
|---|---|---|---|
| 0 | Récupération + consolidation de la dette | Non | À faire en premier — recueil rapide, alimente les phases suivantes |
| 1 | Audit métier, par zone de risque décroissant | — | Le gros morceau |
| 2 | Arbitrage : corriger / documenter / abandonner | — | Décision, pas exécution |
| 3 | Audit tests — forme (nommage + classement U/I) | Non | Parallélisable dès la Phase 1 |
| 4 | Audit tests — fond (cas couverts, cas manquants) | Oui | Après l'audit métier |
| 5 | Re-run couverture + ratio intégration + rapports | — | Clôture |

**Note sur ce qui est réellement noté** : Ex1 est mergé et sera évalué tel quel. Les bugs métier trouvés ne sont pas tous dans le périmètre noté. Ce qui l'est : couverture ≥80% par indicateur, ratio ≥30% d'intégration, absence de tests sur les DTO, cohérence de l'historique de commits. Le reste relève de la qualité perçue en soutenance. **La Phase 2 existe pour ne pas corriger des points hors périmètre.**

## Phase 1 — Audit métier

### Principe : revue dirigée par hypothèses, pas lecture linéaire

Une question formulée à l'avance transforme une revue de code (qui demande de l'expertise) en vérification (qui demande de la rigueur). C'est faisable sans être expert Angular/Spring.

### Cibler : inventaire par surface, pas par fichier

Lister les **contrats** (chaque endpoint back, chaque action front), pas les fichiers. Pour chacun, appliquer la même grille de 7 questions :

1. Qui a le droit d'appeler ça — et le code le vérifie-t-il côté serveur (pas seulement côté front) ?
2. Que se passe-t-il si l'identifiant n'existe pas ?
3. Que se passe-t-il si l'entrée est malformée (id non numérique, champ vide, email invalide) ?
4. Que se passe-t-il si l'action est déjà faite (participer deux fois, supprimer un déjà supprimé) ?
5. Que se passe-t-il si l'action inverse n'a jamais eu lieu (annuler une participation inexistante) ?
6. L'utilisateur est-il propriétaire de la ressource qu'il modifie/supprime ?
7. L'erreur remontée est-elle interprétable côté front, ou tombe-t-elle en 500 ?

### Hiérarchiser le risque : marqueurs mécaniques, grep-ables

Une unité est à risque si elle contient au moins un de ces marqueurs :

- une condition (`if`, ternaire, `@if`) ;
- une écriture en base (`save`, `delete`) ;
- un contrôle d'autorisation ou une lecture du principal JWT ;
- une conversion/parsing (`Long.parseLong` sur un `@PathVariable` est un classique OC) ;
- une opération sur collection (`contains`, `stream().anyMatch`, `removeIf`) ;
- un `Optional.get()`, `.orElse(null)`, ou une assertion non-null (`!`) côté TS.

**Zéro marqueur = zéro audit.** Ce qui exclut mécaniquement les unités triviales (ex. un service à une seule méthode de lecture sans logique).

### Départager bug et design discutable

| Catégorie | Définition | Preuve exigée | Décision par défaut |
|---|---|---|---|
| **A — Bug fonctionnel** | Résultat faux ou exception non gérée dans un cas atteignable par un utilisateur | Un scénario reproductible en 3 étapes | Corriger + test |
| **B — Sécurité / autorisation** | Un acteur fait ce qu'il ne devrait pas | Idem | Corriger + test |
| **C — Fragilité** | Marche aujourd'hui, casse au premier changement | Argument, pas scénario | Documenter, corriger si trivial |
| **D — Design discutable** | Non idiomatique mais comportement conforme | — | Documenter, ne pas corriger |

**Test de départage** : *peux-tu écrire un test qui échoue aujourd'hui et qui devrait passer ?* Si oui → A ou B. Si tu ne peux qu'argumenter sans écrire de test rouge → C ou D. Critère opérationnel, pas jugement de goût — protège contre la sur-ingénierie.

### Apport réel de la recherche web

Limité, à utiliser ponctuellement : vérifier un comportement dépendant de version (Spring Security 6.x sur Boot 3.5, spécificités Angular 19 standalone), confirmer qu'un pattern est réellement déconseillé plutôt que juste inhabituel. **Elle ne trouvera pas les pièges volontairement glissés par OpenClassrooms.**

## Phase 3 — Audit des tests : forme

### Passe 1 — Inventaire mécanique (aucun jugement)

Tableau de tous les tests existants : fichier, describe/classe, nom, câblage détecté (`@SpringBootTest` / `@ExtendWith(MockitoExtension)` / `@WebMvcTest` / MockMvc / TestBed / HttpTestingController / mocks purs), nombre d'assertions.

### Passe 2 — Classement unitaire vs intégration

**Critère opérationnel :**

- **Back — intégration** : le test démarre un contexte Spring et traverse au moins deux couches réelles (controller → service → repository/BDD réels).
- **Back — unitaire** : tous les collaborateurs sont mockés.
- **Front — intégration** : assertion sur le DOM après coordination composant↔service↔HTTP, ou vérification via `HttpTestingController`.
- **Front — unitaire** : appel de méthode + assertion sur une propriété de classe.

**Piège classique à vérifier** : `@WebMvcTest` + `@MockBean(Service)` ressemble à un test d'intégration (contexte Spring, MockMvc, JSON) mais n'intègre rien — c'est un test unitaire du controller avec sérialisation HTTP. À reclasser si trouvé.

### Note méthodologique — HttpTestingController et la notion d'« intégration »

Deux lectures coexistent dans la doctrine Angular sur le statut d'un test utilisant
`HttpTestingController` :

1. **Lecture stricte (majoritaire)** : `HttpTestingController` remplace la couche
   transport, aucun appel réseau réel n'a lieu → le test reste *unitaire*. Le vrai
   test d'intégration, dans cette lecture, est celui qui fait un appel réseau réel
   vers un serveur de test dédié.
2. **Lecture par profondeur de coordination** (retenue dans ce document) : ce qui
   définit l'intégration n'est pas la présence du réseau réel, mais la coordination
   réelle de plusieurs collaborateurs (composant, service, construction de la
   requête HTTP) sans mock intermédiaire — seul le transport final est intercepté.
   Cette lecture s'aligne sur le modèle *Testing Trophy* (privilégier les tests
   d'intégration à la coordination réelle, réserver le mock aux cas où aucune
   autre option n'existe).

**Ce projet retient la lecture 2**, choix assumé et documenté ici plutôt qu'implicite :
un test via `HttpTestingController` est classé *intégration* dès lors qu'il exerce
un composant ou un service réel jusqu'à la construction de la requête, même sans
assertion sur le DOM rendu. Un test qui mocke directement le service (sans passer
par `HttpTestingController`) reste unitaire, de même qu'un test qui n'exerce qu'une
méthode isolée avec assertion sur une propriété de classe.

Sortie attendue : trois colonnes — classement déclaré (nom/dossier) · classement réel · écart. Seuls les écarts remontent à arbitrage.

**⚠️ Deux pièges à vérifier avant tout renommage :**

1. Renommer en `*IT.java` sort les tests de Surefire (passent sous Failsafe) → `mvn test` ne les exécute plus, couverture Jacoco impactée. Vérifier la config du projet avant de renommer quoi que ce soit (rappel : ce projet inclut déjà `*IT.java` explicitement dans les `<includes>` Surefire — voir `pom.xml`).
2. Le reclassement change le ratio d'intégration déclaré. Recalculer le ratio réel avant de décider d'un renommage, pour savoir si des tests d'intégration supplémentaires sont nécessaires.

## Phase 4 — Audit des tests : fond

### Passe 3 — Pertinence du contenu

Construire une matrice **comportement attendu × test**, où les comportements viennent du testing plan + des questions de la grille Phase 1. Trois sorties possibles :

- comportement non couvert,
- test sans comportement associé,
- test redondant.

### Critère anti-sur-couverture

Un test doit pouvoir échouer pour une raison utile. Un test qui ne peut échouer que si le framework est cassé (getters, constructeurs, mappers MapStruct, `verify()` sur un mock qu'on vient de stubber) n'apporte rien.

**Exception notée** : s'il existe des tests sur les DTO ou les mappers, ce n'est pas de la sur-couverture — c'est une **non-conformité à l'énoncé** (consigne explicite : ne pas tester les DTO). À traiter en priorité dans cette passe, en vérifiant que leur suppression ne fait pas passer un package sous 80%.

### Grille de cas de bord réutilisable

entrée nulle/vide · id inexistant · collection vide · doublon · état déjà atteint · non-admin · non-propriétaire · format invalide · date passée.

## Phase 0 — Retrouver la dette oubliée

Par ordre de rendement décroissant :

1. **Le repo** (Claude Code, exhaustif) : `TODO`, `FIXME`, `XXX`, `HACK`, `@Disabled`, `@Ignore`, `.skip`, `xit`, `eslint-disable`, `@SuppressWarnings`, catch vides, `any` résiduels, exclusions ajoutées dans Jacoco/Jest.
2. **L'historique git** (souvent le meilleur gisement) : `git log --grep` sur `wip`/`temp`/`later`/`todo`/`revert`, et surtout `git log -p -- pom.xml jest.config.js cypress.config.ts`. Les diffs de configuration trahissent les compromis : un seuil abaissé, une exclusion ajoutée, un plugin désactivé.
3. **Les conversations Claude.ai du Projet** : recherche par mots-clés dans le périmètre du Projet — utile, pas exhaustif. Requêtes ciblées : « plus tard », « à corriger », « on verra », « reporté », plus les noms de fichiers sensibles.
4. **Les PR GitHub** commentées.

### Consolidation : `DETTE_ET_SUIVI.md`

Fichier versionné unique, colonnes :

| ID | Source | Zone | Description | Catégorie (A/B/C/D) | Impact note (oui/non) | Décision | Statut |
|---|---|---|---|---|---|---|---|

**La colonne « impact note » est la digue anti-sur-ingénierie** : tout ce qui est D + impact note = non est documenté et jamais corrigé.

**Condition avant dépôt final** : aucune ligne « non-conformité à un critère noté » ne doit rester au statut ouvert le jour du dépôt.

## Répartition des rôles : Claude.ai (conversationnel) vs Claude Code

**Règle générale** : si la réponse dépend du contenu exact d'un fichier → Claude Code. Si elle dépend d'un arbitrage → conversationnel. Le conversationnel qui raisonne sur du code qu'il n'a pas lu invente, et ce n'est pas vérifiable.

**Règles complémentaires impératives :**

1. **Jamais audit + correction dans le même prompt Claude Code.** L'audit read-only produit un livrable qui s'arbitre en conversationnel. Fusionner les deux retire la décision.
2. **Imposer le format de sortie dans chaque prompt d'audit** (colonnes exactes, fichier de destination). Sans ça, les résultats de plusieurs sessions ne sont pas agrégeables.

| Étape | Conversationnel | Claude Code |
|---|---|---|
| P0 dette — repo + git | — | ✅ prompt read-only dédié |
| P0 dette — conversations passées | ✅ (recherche + tri) | — |
| P0 consolidation `DETTE_ET_SUIVI.md` | ✅ (structure, arbitrage) | ✅ (écriture du fichier) |
| P1 grille des 7 questions par endpoint | ✅ | — |
| P1 inventaire endpoints/écrans + marqueurs de risque | — | ✅ prompt read-only |
| P1 audit zone par zone | — | ✅ un prompt par zone, jamais un méga-audit |
| P2 classification A/B/C/D + décision | ✅ exclusivement | — |
| P3 inventaire des tests | — | ✅ |
| P3 classement U/I (application du critère) | — | ✅ (critère fourni dans le prompt) |
| P3 arbitrage des écarts + risque ratio 30% | ✅ | — |
| P4 matrice comportements × tests | ✅ (construction) | ✅ (extraction) |
| P4 cas de bord manquants | ✅ | — |
| P5 exécution couverture, ratio, rapports | — | ✅ |
| Corrections effectives | — | ✅ (prompts séparés, un sujet = un commit) |

**Repère pratique** : dès qu'une question commence par *« est-ce que le code fait… »*, c'est un prompt Claude Code séparé, jamais une réponse conversationnelle directe.
