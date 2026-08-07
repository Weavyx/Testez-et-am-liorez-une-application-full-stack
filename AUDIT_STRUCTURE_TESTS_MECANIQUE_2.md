# Audit structurel mécanique des tests — vérification n°2

Audit en lecture seule, branche `chore/verification-finale-livrable-p4`. Aucun fichier modifié.

## F. Attentes arbitraires (`setTimeout`) dans les tests Jest (front)

**Périmètre scanné** : `front/src/app/**/*.spec.ts` — 13 fichiers.

**Résultat : 0 occurrence.**

Aucun `setTimeout` n'a été trouvé dans les fichiers de test front. Les tests asynchrones du projet reposent déjà sur des mécanismes déterministes (`fakeAsync`/`tick()`, `async`/`await` + `fixture.whenStable()`, ou le comportement synchrone de `HttpTestingController` avec `flush()`), sans attente arbitraire sur l'horloge réelle.

| Fichier | Ligne | Extrait | Avis |
|---|---|---|---|
| — | — | — | Aucune occurrence — rien à signaler |

## G. Dépendance à l'horloge système réelle dans le code testé (back)

**Périmètre scanné** : `back/src/main/java/**/*.java` — 35 fichiers, recherche de `LocalDateTime.now()`, `Instant.now()`, `new Date()`, `System.currentTimeMillis()`.

**Résultat : 2 occurrences, toutes les deux dans `JwtUtils`, aucune injection de `Clock`.**

| Fichier | Ligne | Extrait | Avis |
|---|---|---|---|
| `back/src/main/java/com/openclassrooms/starterjwt/security/jwt/JwtUtils.java` | 32 | `.setIssuedAt(new Date())` | (b) Appel direct à `new Date()`, non substituable en test (pas de `Clock` injecté). |
| `back/src/main/java/com/openclassrooms/starterjwt/security/jwt/JwtUtils.java` | 33 | `.setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))` | (b) Idem — dépend de l'horloge système réelle au moment de l'appel. |

**Classe couverte par un test ?** Oui — `back/src/test/java/com/openclassrooms/starterjwt/security/jwt/JwtUtilsTest.java` exerce `generateJwtToken`, `getUserNameFromJwtToken` et `validateJwtToken`.

**Un test existant en souffre-t-il réellement ?** Non, constaté sain.
- `JwtUtilsTest.should_generateValidDecodableToken_when_generateJwtTokenIsCalled` (ligne 55) n'affirme **pas** de valeur absolue horodatée ; il compare `claims.getExpiration()).isAfter(claims.getIssuedAt())` — une assertion relative, insensible au moment exact d'exécution. Aucune fragilité observée.
- Les autres tests de `JwtUtilsTest` qui manipulent des dates (tokens expirés/valides, lignes 79-113) construisent eux-mêmes leurs tokens de test avec `System.currentTimeMillis() ± offset` directement dans le test — ils ne dépendent pas de l'implémentation interne de `JwtUtils` pour l'horodatage, donc aucune fragilité liée à l'horloge non-mockable de la classe testée.

**Conclusion pour G** : `JwtUtils` utilise `new Date()` de façon directe et non injectable (cas (b), horloge non substituable en test), ce qui est un défaut de testabilité générique (impossible d'écrire un test déterministe sur l'instant exact d'émission/expiration sans un `Clock` injecté). Cependant, **aucun test existant n'est fragile aujourd'hui** : la seule assertion sur les timestamps produits par `JwtUtils` est relative (`isAfter`), pas une comparaison à une valeur figée avec marge de tolérance. Il s'agit donc d'une limitation de testabilité latente à arbitrer (introduire un `Clock` injecté si des tests horodatés plus précis devenaient nécessaires), et non d'un défaut qui casse ou fragilise la suite actuelle.
