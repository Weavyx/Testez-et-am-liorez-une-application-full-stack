# Audit — Absence de tests directs sur les packages DTO / Mapper / Payload

**Date** : 2026-08-07
**Branche** : `chore/verification-finale-livrable-p4`
**Périmètre** : `back/src/test/java/` (intégralité)
**Type** : Audit en lecture seule — aucun fichier modifié.

## Objectif

Vérifier une garantie distincte de l'exclusion JaCoCo (déjà auditée précédemment) : que
**la consigne « ne pas tester les packages de type DTO » soit respectée dans les faits**,
c'est-à-dire qu'aucun fichier de test back ne cible directement une classe des packages
`dto`, `mapper` ou `payload` (requête/réponse), que ce soit par emplacement de fichier
(chemin miroir) ou par contenu (instanciation/appel direct d'une classe DTO ou mapper
pour en vérifier le comportement interne).

## Packages de production vérifiés

Identifiés via :
```
find back/src/main/java -type d \( -iname "dto" -o -iname "mapper" -o -iname "payload" \)
```

- `back/src/main/java/com/openclassrooms/starterjwt/dto`
- `back/src/main/java/com/openclassrooms/starterjwt/mapper`
- `back/src/main/java/com/openclassrooms/starterjwt/payload` (incluant les sous-packages `request`/`response`)

## Fichiers de test scannés (exhaustivité)

**16 fichiers de test** au total dans `back/src/test/java/` :

```
AbstractIntegrationTest.java
ApplicationContextIT.java
controllers/AuthControllerIT.java
controllers/SessionControllerIT.java
controllers/TeacherControllerIT.java
controllers/UserControllerIT.java
exception/GlobalExceptionHandlerTest.java
models/SessionTest.java
models/TeacherTest.java
models/UserTest.java
security/jwt/AuthTokenFilterTest.java
security/jwt/JwtUtilsTest.java
security/services/UserDetailsImplTest.java
security/services/UserDetailsServiceImplTest.java
services/SessionServiceTest.java
services/TeacherServiceTest.java
services/UserServiceTest.java
```

Vérifications effectuées :
1. **Chemin miroir** — recherche de tout fichier dans un sous-chemin de test `.../dto/`,
   `.../mapper/`, ou `.../payload/**` : aucun résultat. Aucun sous-dossier `dto`, `mapper`
   ou `payload` n'existe sous `back/src/test/java`.
2. **Déclaration de package** — `grep "^package"` sur les 16 fichiers, filtré sur
   `dto|mapper|payload` (insensible à la casse) : **0 résultat**. Aucun fichier de test
   n'est déclaré dans un de ces packages.
3. **Contenu** — recherche des motifs `Mapper`, `Dto`, `payload\.` dans l'ensemble des
   fichiers de test. 3 fichiers contiennent une occurrence :
   - `services/UserServiceTest.java:213` — un **commentaire** expliquant pourquoi
     `SessionMapper` appelle `UserService` (aucun appel réel, aucune classe mapper
     instanciée ou testée).
   - `controllers/UserControllerIT.java:84,102` — le nom de méthode de test
     (`findById_returns200AndUserDto_whenUserReadsOwnAccount`) et un commentaire
     mentionnent `UserDto`, mais l'assertion porte sur le **JSON retourné par
     l'endpoint** (`GET /api/user/{id}`) via `MockMvc`/`jsonPath` — c'est un test du
     comportement du controller, pas une instanciation ou un appel direct de la classe
     `UserDto`. Conforme à la distinction énoncée dans la consigne (DTO comme résultat
     observable d'un appel controller = légitime).
   - `controllers/AuthControllerIT.java:3,54,176,178` — `ObjectMapper` est la classe
     Jackson (`com.fasterxml.jackson.databind.ObjectMapper`), sans rapport avec le
     package `mapper` du projet ; le mot « payload » à la ligne 178 apparaît dans un
     commentaire décrivant la structure d'un JWT (`header.payload.signature`), sans
     rapport avec le package `payload` du projet.

Aucune autre occurrence des mots-clés `Dto`, `Mapper`, `payload` (packages projet) n'a
été trouvée dans les 16 fichiers de test.

## Conclusion

**0 fichier ou classe de test ne cible directement une classe des packages `dto`,
`mapper` ou `payload`.** Les seules mentions de DTO dans les tests apparaissent dans des
tests de controller (`*ControllerIT.java`) qui vérifient le JSON produit par un endpoint
réel via `MockMvc` — un test légitime du comportement de l'API, pas du DTO en tant que
tel.

La consigne « ne pas tester les packages de type DTO côté back » est respectée dans les
faits, et pas seulement au niveau de l'exclusion JaCoCo de la métrique de couverture.

**Aucune violation trouvée — aucune action corrective nécessaire.**
