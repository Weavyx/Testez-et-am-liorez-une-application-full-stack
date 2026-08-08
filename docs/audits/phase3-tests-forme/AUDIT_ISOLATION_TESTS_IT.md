# Audit — Isolation réelle des tests d'intégration (`*IT.java`)

Périmètre : `back/src/test/java/.../AbstractIntegrationTest.java`, les 4 classes
`*ControllerIT.java` (Auth, Session, Teacher, User), `ApplicationContextIT.java`,
et `back/src/test/resources/application-test.yml`. Audit en lecture seule,
aucun fichier modifié, aucun test exécuté.

## 1. Stratégie d'isolation réellement en place

**Le conteneur MySQL est bien un singleton partagé pour toute la JVM de test.**
`AbstractIntegrationTest` démarre `mysqlContainer` dans un bloc `static {}` (pas
via `@Container`/`@Testcontainers`), avec un commentaire explicite dans le
fichier justifiant ce choix (éviter qu'un `ApplicationContext` Spring mis en
cache pointe vers un port mort après le redémarrage d'un conteneur par
classe). Donc, comme anticipé : **l'isolation ne peut pas venir du conteneur**,
qui persiste (schéma et données) entre toutes les classes de test de la JVM.

**Il n'existe aucun mécanisme de nettoyage explicite.** Recherche exhaustive
sur l'ensemble de l'arbre de test : aucun `@Sql`, aucun `@BeforeEach`/
`@AfterEach`, aucun `deleteAll()` dans `AbstractIntegrationTest` ni dans les 4
fichiers `*ControllerIT.java`. Le seul `@BeforeEach`/`@AfterEach` du repo se
trouve dans des tests **unitaires** (`SessionServiceTest`, `UserServiceTest`,
`AuthTokenFilterTest`, `JwtUtilsTest`), hors périmètre de cet audit.

**L'isolation vient exclusivement de `@Transactional` posé au niveau classe**
sur chacune des 4 classes `*ControllerIT` :

```java
@AutoConfigureMockMvc
@Transactional
class AuthControllerIT extends AbstractIntegrationTest { ... }
```

Identique pour `SessionControllerIT`, `TeacherControllerIT`, `UserControllerIT`.
`@Transactional` au niveau classe s'applique à **chaque méthode de test
individuellement** (c'est le comportement standard de
`TransactionalTestExecutionListener` de Spring Test : une transaction est
ouverte avant chaque `@Test` et rollback par défaut à la fin de celle-ci, pas
une seule transaction pour toute la classe). `AbstractIntegrationTest`
lui-même n'est **pas** annoté `@Transactional` — l'annotation n'existe que sur
les sous-classes concrètes.

Point de vigilance vérifié : `AbstractIntegrationTest` expose des méthodes
protégées (`persistStandardUser()`, `generateTokenForUser()`, etc.) qui
insèrent des données via des repositories injectés dans la classe abstraite.
Ces insertions s'exécutent dans le contexte transactionnel de la méthode de
test appelante (même thread), donc elles sont couvertes par le rollback au
même titre que les insertions faites directement dans les classes filles —
pas de fuite possible par ce biais.

Autre point vérifié : `@AutoConfigureMockMvc` est utilisé ici avec
`webEnvironment = RANDOM_PORT` (hérité d'`AbstractIntegrationTest`). Le bean
`MockMvc` construit dans cette configuration reste un dispatcher Spring MVC
in-process (pas un vrai client HTTP passant par le port réseau ouvert) : les
requêtes `mockMvc.perform(...)` s'exécutent donc dans le même thread que le
test, donc dans la même transaction. C'est cohérent avec le commentaire trouvé
dans `SessionControllerIT` sur `delete_returns200AndClearsParticipations_whenSessionHasParticipants`,
qui explique pourquoir un `entityManager.flush()` est nécessaire avant les
assertions (sans quoi les écritures resteraient différées jusqu'au rollback,
dans la même transaction) — c'est une confirmation empirique indirecte, déjà
présente dans le code, que le mécanisme fonctionne comme attendu.

**Conclusion étape 1** : stratégie = transactionnelle, avec rollback
automatique par méthode de test, appliquée uniformément aux 4 classes de
contrôleurs. Aucun nettoyage manuel n'existe ni n'est nécessaire tant que ce
mécanisme est correctement en place partout.

## 2. Conformité par fichier

| Fichier | Méthode(s) | Respecte la stratégie d'isolation ? | Risque identifié |
|---|---|---|---|
| `AbstractIntegrationTest.java` | (classe abstraite, pas de `@Test`) | N/A — pas annotée `@Transactional` elle-même, mais ses méthodes d'aide héritent de la transaction de la classe fille qui les appelle | Aucun : comportement attendu et vérifié (voir §1) |
| `ApplicationContextIT.java` | `contextLoads()` | Pas de `@Transactional`, mais ne touche aucune donnée (test de démarrage de contexte uniquement) | Aucun |
| `AuthControllerIT.java` | Toutes (10 méthodes) | Oui — `@Transactional` au niveau classe, toutes les données créées (`persistUser`, emails via `uniqueEmail(UUID)`) le sont dans la méthode qui les utilise | Aucun confirmé. Emails générés par `UUID.randomUUID()` : collision techniquement possible mais non liée à l'ordre d'exécution (probabilité négligeable, hors périmètre "ordre de tests") |
| `SessionControllerIT.java` | Toutes (~30 méthodes) | Oui — `@Transactional` au niveau classe, toutes les entités (`Teacher`, `Session`, `User`) créées via les helpers `persistTeacher()/persistSession()/persistParticipant()` dans la méthode elle-même | Aucun confirmé. `findAll_returns200AndAllSessions` fait `hasSize(2)` — dépend explicitement (et en connaissance de cause, cf. commentaire ligne 198) du rollback pour que la table soit vide en entrée ; c'est cohérent avec la stratégie identifiée, pas une dérogation |
| `TeacherControllerIT.java` | Toutes (7 méthodes) | Oui — `@Transactional` au niveau classe, `Teacher` créé localement dans chaque méthode qui en a besoin | Aucun confirmé. Même remarque `hasSize(2)` sur `findAll_returns200AndAllTeachers`, également documentée en commentaire (ligne 101-102) comme dépendant du rollback |
| `UserControllerIT.java` | Toutes (12 méthodes) | Oui — `@Transactional` au niveau classe, `User`/`Session`/`Teacher` créés localement via `persistStandardUser()`/`persistSessionWithParticipant()` | Aucun confirmé |

Aucune méthode, dans les 4 fichiers `*ControllerIT.java`, ne déroge à
l'annotation `@Transactional` de sa classe (elle est posée une fois au niveau
classe, sans `@Rollback(false)` ni `@Commit` nulle part dans l'arbre de test).

## 3. Recherche de tests potentiellement order-dependent

- **ID auto-incrémenté supposé fixe** : aucun test ne suppose une valeur
  fixe d'ID (`teacher_id = 1`, etc.). Toutes les assertions utilisent l'ID
  réellement renvoyé par la persistance (`teacher.getId()`, `session.getId()`,
  `user.getId()`) ou des IDs volontairement inexistants pour tester les 404
  (`999999L`). **Aucun risque identifié sur ce point.**

- **Comptage de lignes sans avoir créé toutes les lignes comptées** :
  identifié deux occurrences — `SessionControllerIT#findAll_returns200AndAllSessions`
  et `TeacherControllerIT#findAll_returns200AndAllTeachers` — qui font
  `hasSize(2)` sur l'intégralité de la table. Les deux dépendent bien du fait
  que la table soit vide en entrée de méthode. **Risque théorique confirmé
  comme dépendance à la stratégie d'isolation (pas à l'ordre d'exécution)** :
  si `@Transactional` ne faisait pas correctement son rollback (mauvaise
  configuration, `@Commit` accidentel, appel HTTP sortant du thread de test),
  ces deux tests seraient les premiers à révéler la fuite, par un
  `hasSize(2)` qui échouerait avec un nombre supérieur dès qu'un autre test de
  la même classe s'exécuterait avant. Les auteurs en avaient conscience : les
  deux tests portent un commentaire explicite le signalant. Ce n'est pas un
  test qui suppose qu'un test précédent a tourné (le cas décrit dans la
  consigne) — c'est l'inverse : un test qui suppose qu'aucun test précédent
  n'a laissé de résidu, ce que `@Transactional` garantit tant qu'il fonctionne
  correctement.

- **Réutilisation d'une donnée censée avoir été créée par un test précédent** :
  recherche négative. Chaque méthode crée systématiquement ses propres
  entités via les helpers (`persistTeacher()`, `persistSession()`,
  `persistParticipant()`, `persistStandardUser()`, etc.). Aucune méthode ne
  suppose l'existence d'une donnée laissée par une autre méthode de la même
  classe ou d'une autre classe. **Aucun risque identifié.**

## Conclusion

La suite `*IT.java` est **order-independent aujourd'hui, mais par conception
explicite (rollback transactionnel systématique), pas par chance** :

- Le mécanisme d'isolation est identifié, uniforme sur les 4 classes, et
  documenté en commentaire à plusieurs endroits du code lui-même
  (`TeacherControllerIT` l'explicite en tête de fichier, `SessionControllerIT`
  et `UserControllerIT` en justifient des usages ponctuels comme le
  `flush()`/`clear()` avant assertion).
- Aucune classe ne déroge à `@Transactional`, aucun test ne s'appuie sur un ID
  fixe ou sur une donnée supposément créée ailleurs.
- Le seul point de fragilité réel n'est pas dans le code des tests mais dans
  la **dépendance implicite au bon fonctionnement de `@Transactional` avec
  MockMvc + `webEnvironment = RANDOM_PORT`** : cette combinaison fonctionne
  correctement uniquement parce que `MockMvc` reste in-process (même thread,
  même transaction) malgré le port réseau ouvert. C'est un point d'attention
  pour toute évolution future de la configuration de test (ex. migration vers
  un vrai client HTTP/`TestRestTemplate` pour certains scénarios) : un tel
  changement casserait silencieusement l'hypothèse de rollback sans qu'aucune
  ligne des fichiers `*IT.java` n'ait besoin de changer pour révéler le
  problème — et les deux `hasSize(2)` identifiés ci-dessus seraient les
  premiers indicateurs (probablement instables) d'une telle régression.
