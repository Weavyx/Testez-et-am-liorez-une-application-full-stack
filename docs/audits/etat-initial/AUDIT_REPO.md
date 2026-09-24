# AUDIT_REPO.md — Rapport architectural et qualité de code

> Généré le 2026-06-26 — lecture seule, aucune modification du code source.
> Branche analysée : `main` (commit de tête : `3411c67`)

---

## 1. Architecture back (`back/src/main/java/`)

### 1.1 Packages et rôles

| Package | Rôle |
|---|---|
| `controllers` | Couche REST : reçoit les requêtes HTTP, délègue aux services, retourne des DTO |
| `services` | Logique métier : `SessionService`, `TeacherService`, `UserService` |
| `repository` | Interfaces Spring Data JPA (accès base de données) |
| `models` | Entités JPA (Lombok `@Data`, `@Builder`) |
| `dto` | Objets de transfert de données (shapes API publiques) |
| `mapper` | Convertisseurs MapStruct entité ↔ DTO |
| `payload/request` | Corps des requêtes d'authentification (`LoginRequest`, `SignupRequest`) |
| `payload/response` | Corps des réponses d'authentification (`JwtResponse`, `MessageResponse`) |
| `security` | Configuration Spring Security + JWT (`WebSecurityConfig`) |
| `security/jwt` | Filtre JWT (`AuthTokenFilter`), génération/validation (`JwtUtils`), point d'entrée (`AuthEntryPointJwt`) |
| `security/services` | `UserDetailsImpl` (implémentation `UserDetails`), `UserDetailsServiceImpl` (chargement par email) |
| `exception` | Exceptions métier et `GlobalExceptionHandler` |
| `configuration` | `AppConfig` : chargement du fichier `.env` |

---

### 1.2 Controllers — endpoints complets

#### `AuthController` — `@RequestMapping("/api/auth")`

| Méthode HTTP | Path | Corps entrant | Retour |
|---|---|---|---|
| `POST` | `/api/auth/login` | `LoginRequest` (`email`, `password`) | `JwtResponse` (200) |
| `POST` | `/api/auth/register` | `SignupRequest` (`email`, `firstName`, `lastName`, `password`) | `MessageResponse` (200) |

#### `SessionController` — `@RequestMapping("/api/session")`

| Méthode HTTP | Path | Corps entrant | Retour |
|---|---|---|---|
| `GET` | `/api/session/{id}` | — | `SessionDto` |
| `GET` | `/api/session` | — | `List<SessionDto>` |
| `POST` | `/api/session` | `SessionDto` | `SessionDto` |
| `PUT` | `/api/session/{id}` | `SessionDto` | `SessionDto` |
| `DELETE` | `/api/session/{id}` | — | 200 vide |
| `POST` | `/api/session/{id}/participate/{userId}` | — | 200 vide |
| `DELETE` | `/api/session/{id}/participate/{userId}` | — | 200 vide |

> **Note** : la méthode gérant `DELETE /api/session/{id}` s'appelle `save()` — nom trompeur, dette de nommage.

#### `TeacherController` — `@RequestMapping("/api/teacher")`

| Méthode HTTP | Path | Retour |
|---|---|---|
| `GET` | `/api/teacher/{id}` | `TeacherDto` |
| `GET` | `/api/teacher` | `List<TeacherDto>` |

#### `UserController` — `@RequestMapping("/api/user")`

| Méthode HTTP | Path | Retour |
|---|---|---|
| `GET` | `/api/user/{id}` | `UserDto` |
| `DELETE` | `/api/user/{id}` | 200 vide |

> **Note** : même problème de nommage — la méthode delete s'appelle `save()`.

---

### 1.3 Services — méthodes publiques

#### `SessionService`

| Signature | Description |
|---|---|
| `Session create(Session session)` | Persiste une nouvelle session |
| `void delete(Long id)` | Supprime par ID, lève `NotFoundException` si absent |
| `List<Session> findAll()` | Retourne toutes les sessions |
| `Session getById(Long id)` | Retourne une session ou lève `NotFoundException` |
| `Session update(Long id, Session session)` | Force l'ID puis sauvegarde |
| `void participate(Long id, Long userId)` | Ajoute un participant ; `NotFoundException` si session/user absent, `BadRequestException` si déjà inscrit |
| `void noLongerParticipate(Long id, Long userId)` | Retire un participant ; `NotFoundException` si session absente, `BadRequestException` si non inscrit |

#### `TeacherService`

| Signature | Description |
|---|---|
| `List<Teacher> findAll()` | Retourne tous les enseignants |
| `Teacher findById(Long id)` | Retourne un enseignant ou lève `NotFoundException` |

#### `UserService`

| Signature | Description |
|---|---|
| `void delete(Long id)` | Suppression directe sans vérification (non appelée de l'extérieur) |
| `void deleteById(Long id, String currentUsername)` | Vérifie que l'utilisateur supprime son propre compte ; lève `UnauthorizedException` sinon |
| `User findById(Long id)` | Retourne un utilisateur ou lève `NotFoundException` |
| `Optional<User> findByEmail(String email)` | Recherche par email |
| `boolean existsByEmail(String email)` | Vérification d'unicité d'email |
| `boolean isAdmin(String email)` | Vérifie le flag admin |
| `User save(User user)` | Sauvegarde brute (non exposée en REST) |
| `void register(String email, String lastName, String firstName, String rawPassword)` | Inscription : vérifie l'unicité, encode le mot de passe, crée l'utilisateur |

---

### 1.4 Repositories et entités JPA

| Repository | Entité | Méthodes personnalisées |
|---|---|---|
| `SessionRepository extends JpaRepository<Session, Long>` | `Session` | Aucune (CRUD standard uniquement) |
| `TeacherRepository extends JpaRepository<Teacher, Long>` | `Teacher` | Aucune |
| `UserRepository extends JpaRepository<User, Long>` | `User` | `Optional<User> findByEmail(String)`, `Boolean existsByEmail(String)` |

**Entités JPA :**

- **`Session`** (`sessions`) : `id`, `name` (50c), `date` (`Date`), `description` (2500c), relation `@OneToOne` vers `Teacher`, relation `@ManyToMany` (EAGER) vers `User` via table `PARTICIPATE`, `createdAt`, `updatedAt`
- **`Teacher`** (`teachers`) : `id`, `lastName` (20c), `firstName` (20c), `createdAt`, `updatedAt`
- **`User`** (`users`) : `id`, `email` (unique, 50c), `lastName` (20c), `firstName` (20c), `password` (120c), `admin` (boolean), `createdAt`, `updatedAt`

Toutes les entités utilisent Lombok (`@Data`, `@Builder`, `@Accessors(chain=true)`) et JPA Auditing (`AuditingEntityListener`) pour les timestamps.

---

### 1.5 Couche DTO et mappers MapStruct

**DTOs :**

| DTO | Champs notables |
|---|---|
| `SessionDto` | `id`, `name`, `date`, `teacher_id` (Long — clé étrangère aplatie), `description`, `users` (List\<Long\> — IDs seulement), `createdAt`, `updatedAt` |
| `TeacherDto` | Reflet direct de l'entité `Teacher` |
| `UserDto` | Reflet direct de l'entité `User` |

**Mappers :**

| Mapper | Type | Particularité |
|---|---|---|
| `EntityMapper<D,E>` | Interface générique | Déclare `toEntity`, `toDto`, `toEntity(List)`, `toDto(List)` |
| `TeacherMapper` | Interface étendant `EntityMapper` | Mapping direct champ-à-champ, aucune logique custom |
| `UserMapper` | Interface étendant `EntityMapper` | Mapping direct champ-à-champ |
| `SessionMapper` | **Classe abstraite** étendant `EntityMapper` | Injecte `TeacherService` et `UserService` via `@Autowired` ; résout les IDs en entités dans `toEntity()` avec des expressions Java inline dans `@Mapping` |

**Couplage remarquable dans `SessionMapper` :**
- `toEntity(SessionDto)` appelle `teacherService.findById(sessionDto.getTeacher_id())` et `userService.findById(userId)` pour reconstruire les relations.
- Les expressions Java injectées dans `@Mappings` sont des lambdas de stream — lisibles mais inhabituelles dans MapStruct.

---

### 1.6 Sécurité JWT — classes et flux

**Classes impliquées :**

| Classe | Rôle |
|---|---|
| `WebSecurityConfig` | Configure la `SecurityFilterChain` : stateless, CSRF désactivé, CORS désactivé, règles d'autorisation, `DaoAuthenticationProvider` |
| `AuthTokenFilter` | `OncePerRequestFilter` : extrait le JWT de l'en-tête `Authorization: Bearer …`, valide, charge le `UserDetails`, positionne l'authentification dans le `SecurityContext` |
| `JwtUtils` | Génère (`HS512` + secret configurable), parse et valide les tokens JWT avec la lib `jjwt` |
| `AuthEntryPointJwt` | Classe présente mais **entièrement commentée** — n'implémente pas `AuthenticationEntryPoint` ; le gestionnaire d'erreur 401 est une lambda inline dans `WebSecurityConfig` |
| `UserDetailsImpl` | Implémentation de `UserDetails` : champs `id`, `username` (email), `firstName`, `lastName`, `admin`, `password` ; `getAuthorities()` retourne toujours un `HashSet` vide |
| `UserDetailsServiceImpl` | Charge l'utilisateur par email depuis `UserRepository` ; construit un `UserDetailsImpl` — **oublie de renseigner le champ `admin`** |

**Flux d'authentification (login) :**

```
Client  →  POST /api/auth/login  →  AuthController.authenticateUser()
        →  AuthenticationManager.authenticate(UsernamePasswordAuthenticationToken)
        →  DaoAuthenticationProvider  →  UserDetailsServiceImpl.loadUserByUsername(email)
        →  UserRepository.findByEmail(email)  →  UserDetailsImpl
        →  BCryptPasswordEncoder.matches(rawPwd, encodedPwd)
        →  JwtUtils.generateJwtToken(authentication)
        →  ResponseEntity<JwtResponse(jwt, id, email, firstName, lastName, isAdmin)>
```

**Flux d'autorisation (requêtes protégées) :**

```
Client  →  GET /api/...  (header: Authorization: Bearer <jwt>)
        →  AuthTokenFilter.doFilterInternal()
        →  JwtUtils.validateJwtToken(jwt)  →  JwtUtils.getUserNameFromJwtToken(jwt)
        →  UserDetailsServiceImpl.loadUserByUsername(email)
        →  SecurityContextHolder.setAuthentication(...)
        →  Controller
```

Routes publiques : `/api/auth/**` — tout le reste exige un JWT valide.

---

## 2. Architecture front (`front/src/app/`)

### 2.1 Arborescence

```
src/app/
├── app.component.ts          # Composant racine (shell avec toolbar + router-outlet)
├── app.config.ts             # Bootstrap standalone : provideRouter + provideHttpClient + intercepteur JWT
├── app.routes.ts             # Définition des routes
│
├── core/
│   ├── models/               # Interfaces TypeScript
│   │   ├── loginRequest.interface.ts
│   │   ├── registerRequest.interface.ts
│   │   ├── session.interface.ts
│   │   ├── sessionInformation.interface.ts
│   │   ├── teacher.interface.ts
│   │   └── user.interface.ts
│   └── service/
│       ├── auth.service.ts          # POST /api/auth/login|register
│       ├── session.service.ts       # État d'authentification en mémoire
│       ├── session-api.service.ts   # CRUD /api/session
│       ├── teacher.service.ts       # GET /api/teacher
│       └── user.service.ts          # GET|DELETE /api/user
│
├── components/
│   └── me/
│       └── me.component.ts          # Page profil utilisateur
│
├── pages/
│   ├── login/
│   │   └── login.component.ts
│   ├── register/
│   │   └── register.component.ts
│   ├── not-found/
│   │   └── not-found.component.ts
│   └── sessions/
│       └── components/
│           ├── list/
│           │   └── list.component.ts    # Liste toutes les sessions
│           ├── detail/
│           │   └── detail.component.ts  # Détail + participation
│           └── form/
│               └── form.component.ts    # Création + modification
│
├── guards/
│   ├── auth.guard.ts       # Redirige vers /login si non connecté
│   └── unauth.guard.ts     # Redirige si déjà connecté
│
├── interceptors/
│   └── customJwtInterceptorFn.ts   # Ajoute Authorization: Bearer sur chaque requête HTTP
│
└── shared/
    └── material.module.ts   # Regroupe les imports Angular Material
```

---

### 2.2 Services Angular et endpoints consommés

| Service | Méthodes | Endpoints back |
|---|---|---|
| `AuthService` | `login(req)`, `register(req)` | `POST /api/auth/login`, `POST /api/auth/register` |
| `SessionApiService` | `all()`, `detail(id)`, `delete(id)`, `create(s)`, `update(id,s)`, `participate(id,userId)`, `unParticipate(id,userId)` | `GET /api/session`, `GET /api/session/:id`, `DELETE /api/session/:id`, `POST /api/session`, `PUT /api/session/:id`, `POST /api/session/:id/participate/:userId`, `DELETE /api/session/:id/participate/:userId` |
| `TeacherService` | `all()`, `detail(id)` | `GET /api/teacher`, `GET /api/teacher/:id` |
| `UserService` | `getById(id)`, `delete(id)` | `GET /api/user/:id`, `DELETE /api/user/:id` |
| `SessionService` | `logIn(user)`, `logOut()`, `$isLogged()` | **Aucun** — service état pur |

---

### 2.3 Gestion de l'état

**Pas de store (NgRx, Signals…).** L'état d'authentification est géré par `SessionService` avec un **`BehaviorSubject<boolean>`** :

- `isLogged: boolean` — propriété publique synchrone (lue directement par les guards et l'intercepteur)
- `sessionInformation: SessionInformation | undefined` — données de session en mémoire
- `$isLogged(): Observable<boolean>` — observable dérivé du `BehaviorSubject` pour les souscriptions réactives (ex : toolbar)

L'état est purement **en mémoire** — un rechargement de page déconnecte l'utilisateur.

---

### 2.4 Guards et intercepteurs

| Fichier | Type | Comportement |
|---|---|---|
| `auth.guard.ts` | `CanActivate` | Bloque si `sessionService.isLogged === false` → redirige vers `/login` |
| `unauth.guard.ts` | `CanActivate` | Bloque si `sessionService.isLogged === true` → redirige vers `/rentals` (**bug : route inexistante**) |
| `customJwtInterceptorFn.ts` | Functional interceptor | Si `isLogged`, clone la requête en ajoutant `Authorization: Bearer <token>` |

**Routes protégées :**

| Route | Guard |
|---|---|
| `/login`, `/register` | `UnauthGuard` |
| `/sessions/**`, `/me` | `AuthGuard` |

---

## 3. Particularités notables du repo

### 3.1 Écarts par rapport aux conventions Spring Boot

| Observation | Détail |
|---|---|
| **Injection incohérente** | `WebSecurityConfig` et `AuthTokenFilter` utilisent `@Autowired` sur champ (field injection) alors que tous les controllers et services utilisent l'injection par constructeur — style contradictoire dans le même projet |
| **`AuthEntryPointJwt` vide** | La classe existe avec `@Slf4j @Component`, mais tout son corps est commenté. Elle n'implémente plus `AuthenticationEntryPoint`. Elle est référencée nulle part dans la config active — dead code |
| **Méthodes mal nommées** | `SessionController.save()` et `UserController.save()` gèrent des opérations `DELETE` — nommage trompeur, probable copier-coller |
| **`Session.teacher` en `@OneToOne`** | La relation enseignant-session est modélisée en `@OneToOne` alors qu'un enseignant peut animer plusieurs sessions — sémantiquement un `@ManyToOne` serait correct |
| **API jjwt dépréciée** | `JwtUtils` utilise `SignatureAlgorithm.HS512` et `Jwts.parser().setSigningKey()` — APIs deprecated dans jjwt ≥ 0.12.x en faveur de `Jwts.SIG.HS512` et `verifyWith()` |
| **JaCoCo 0.8.5** | La version configurée date de 2020 ; la version actuelle est 0.8.12 |
| **`AppConfig` charge `.env` via `FileSystemResource`** | Pattern non standard : Spring Boot lit normalement les `.env` via `spring-boot-docker-compose` ou des profils. Ce bean charge `.env` comme fichier de propriétés supplémentaire depuis le système de fichiers courant |
| **`SessionMapper` classe abstraite** | Les deux autres mappers sont des interfaces. `SessionMapper` est une classe abstraite car elle a des champs `@Autowired` — couplage entre couches mapper et service |
| **`UserDetailsServiceImpl` oublie `admin`** | Le `UserDetailsImpl` construit dans `loadUserByUsername` ne renseigne pas le champ `admin`, qui sera donc toujours `null` dans le contexte de sécurité (le champ `admin` du `JwtResponse` est lu via `userService.isAdmin()` dans le controller, contournant ce problème) |

### 3.2 Écarts par rapport aux conventions Angular

| Observation | Détail |
|---|---|
| **`UnauthGuard` redirige vers `/rentals`** | Route inexistante dans ce projet — copier-coller d'un autre projet OC (Orion/Rental) |
| **`MeComponent` dans `components/`** | Tous les autres composants de page sont dans `pages/` ; `MeComponent` est dans `components/` — arborescence incohérente |
| **`SessionService` (état) ≠ `SessionApiService` (HTTP)** | Nommage ambigu : deux services "session" avec des rôles très différents |
| **`@angular/flex-layout` 15.0.0-beta.42** | Cette librairie est **officiellement archivée** (dépôt GitHub archivé en 2023) — dépendance en fin de vie |
| **État non persisté** | Aucune persistance JWT (localStorage, sessionStorage) — un F5 déconnecte l'utilisateur |
| **Guards en classes `CanActivate`** | Angular 15+ recommande les functional guards ; les guards ici sont encore des classes `@Injectable` avec `CanActivate` (style legacy) |

### 3.3 Dépendances remarquables

**Backend (`pom.xml`) :**

| Dépendance | Version | Note |
|---|---|---|
| Spring Boot | 3.5.5 | Récent |
| Java | 21 | LTS |
| `io.jsonwebtoken:jjwt` | 0.12.6 | Récent mais API interne utilisée de manière dépréciée |
| MapStruct | 1.6.3 | Récent |
| Lombok | 1.18.32 | Récent |
| Testcontainers | 1.20.0 | Récent |
| JaCoCo | **0.8.5** | Très ancien (2020) |
| `spring-boot-docker-compose` | géré par BOM | Démarre MySQL automatiquement via Docker Compose |

**Frontend (`package.json`) :**

| Dépendance | Version | Note |
|---|---|---|
| Angular | 19.2.x | Récent |
| `@angular/flex-layout` | **15.0.0-beta.42** | Librairie archivée — dette technique |
| `@angular/material` | ^19.2.19 | Récent |
| Cypress | ^15.2.0 | Récent |
| Jest | ^29.7.0 | Récent |
| `@types/jest` | ^30.0.0 | Attention : version majeure supérieure à Jest — peut créer des incompatibilités |

---

## 4. État post-Ex1 (branche `main` actuelle)

### 4.1 `GlobalExceptionHandler`

**Emplacement :** `back/src/main/java/com/openclassrooms/starterjwt/exception/GlobalExceptionHandler.java`

Annotée `@RestControllerAdvice`.

| Exception capturée | Code HTTP retourné | Corps |
|---|---|---|
| `NumberFormatException` | 400 Bad Request | Vide |
| `BadRequestException` | 400 Bad Request | `MessageResponse(message)` si message présent, sinon vide |
| `NotFoundException` | 404 Not Found | Vide |
| `UnauthorizedException` | 401 Unauthorized | Vide |

**Exceptions déclarées :**

- `BadRequestException extends RuntimeException` — `@ResponseStatus(BAD_REQUEST)`, constructeur avec et sans message
- `NotFoundException extends RuntimeException` — `@ResponseStatus(NOT_FOUND)`, no-arg uniquement
- `UnauthorizedException extends RuntimeException` — `@ResponseStatus(UNAUTHORIZED)`, no-arg uniquement

### 4.2 Aucun controller n'injecte de repository

**Confirmé.** Les quatre controllers (`AuthController`, `SessionController`, `TeacherController`, `UserController`) n'importent aucun repository. Tous leurs accès aux données passent exclusivement par des services.

### 4.3 Exemple concret de logique métier déportée en service (Ex1)

**`UserController.delete()`** délègue à `userService.deleteById(id, currentUsername)`. La logique suivante réside dans `UserService.deleteById()` :

```java
// UserService.java
public void deleteById(Long id, String currentUsername) {
    User user = findById(id);                             // lève NotFoundException si absent
    if (!Objects.equals(currentUsername, user.getEmail())) {
        throw new UnauthorizedException();                // 401 si ce n'est pas son propre compte
    }
    this.userRepository.deleteById(id);
}
```

Avant Ex1, cette vérification (existsByEmail, vérification de l'identité, gestion des erreurs) aurait été inline dans le controller. Aujourd'hui :
- Le controller se limite à extraire le principal du `SecurityContextHolder` et à appeler `userService`.
- `UserService.register()` encapsule la vérification d'unicité d'email + encodage BCrypt + sauvegarde.
- `SessionService.participate()` / `noLongerParticipate()` contiennent toute la logique métier de participation (existence, doublons, filtrage).
- `TeacherService.findById()` centralise le `orElseThrow(NotFoundException::new)` (utilisé aussi par `SessionMapper`).

---

## 5. Couverture de test actuelle

### 5.1 Tests backend

**Aucun fichier de test Java n'existe dans `back/src/test/`.** Le répertoire `src/test` est absent.

> La règle JaCoCo (90 % de lignes couvertes par package) ferait donc échouer `mvn test` dès qu'elle serait confrontée à du code non couvert — sauf si l'absence totale de tests provoque un résultat de couverture à 0 % qui est simplement ignoré par JaCoCo (comportement dépendant de la configuration).

### 5.2 Tests frontend (Jest)

| Fichier | Composant/Service testé |
|---|---|
| `src/app/app.component.spec.ts` | `AppComponent` |
| `src/app/components/me/me.component.spec.ts` | `MeComponent` |
| `src/app/pages/login/login.component.spec.ts` | `LoginComponent` |
| `src/app/pages/not-found/not-found.component.spec.ts` | `NotFoundComponent` |
| `src/app/pages/register/register.component.spec.ts` | `RegisterComponent` |
| `src/app/pages/sessions/components/detail/detail.component.spec.ts` | `DetailComponent` |
| `src/app/pages/sessions/components/form/form.component.spec.ts` | `FormComponent` |
| `src/app/pages/sessions/components/list/list.component.spec.ts` | `ListComponent` |
| `src/app/core/service/session-api.service.spec.ts` | `SessionApiService` |
| `src/app/core/service/session.service.spec.ts` | `SessionService` |
| `src/app/core/service/teacher.service.spec.ts` | `TeacherService` |
| `src/app/core/service/user.service.spec.ts` | `UserService` |

**Non couverts par des specs :** `AuthService`, `AuthGuard`, `UnauthGuard`, `customJwtInterceptorFn`.

### 5.3 Tests E2E (Cypress)

| Fichier | Scénario |
|---|---|
| `front/cypress/e2e/login.cy.ts` | Login réussi : remplit le formulaire, vérifie la redirection vers `/sessions` (stubbing `cy.intercept`) |

Un seul fichier E2E, un seul scénario happy-path.

### 5.4 Configuration Jest (`front/jest.config.js`)

```js
module.exports = {
  moduleNameMapper: { '@core/(.*)': '<rootDir>/src/app/core/$1' },
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  bail: false,
  verbose: false,
  collectCoverage: false,          // couverture NON collectée par défaut
  coverageDirectory: './coverage/jest',
  testPathIgnorePatterns: ['<rootDir>/node_modules/'],
  coveragePathIgnorePatterns: ['<rootDir>/node_modules/'],
  coverageThreshold: {
    global: { statements: 80 }    // seuil : 80% de statements
  },
  roots: ['<rootDir>'],
  modulePaths: ['<rootDir>'],
  moduleDirectories: ['node_modules'],
};
```

**Note :** `collectCoverage: false` — le seuil de 80 % n'est vérifié que lorsque la couverture est explicitement collectée (ex : `jest --coverage`). Un simple `npm test` ne déclenche pas le seuil.

### 5.5 Configuration JaCoCo (`back/pom.xml`)

```xml
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.5</version>
  <executions>
    <execution>
      <goals><goal>prepare-agent</goal></goals>
    </execution>
    <execution>
      <id>report</id>
      <phase>test</phase>
      <goals><goal>report</goal></goals>
    </execution>
    <execution>
      <id>jacoco-check</id>
      <goals><goal>check</goal></goals>
      <configuration>
        <rules>
          <rule>
            <element>PACKAGE</element>           <!-- granularité : par package -->
            <limits>
              <limit>
                <counter>LINE</counter>          <!-- métrique : lignes -->
                <value>COVEREDRATIO</value>       <!-- ratio lignes couvertes/total -->
                <minimum>0.9</minimum>            <!-- seuil : 90% -->
              </limit>
            </limits>
          </rule>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

**Règle exacte :** chaque package doit avoir **au moins 90 % de ses lignes couvertes** par les tests. Le build Maven échoue si ce seuil n'est pas atteint dans l'un des packages. Aucune exclusion de package n'est configurée.

---

*Fin du rapport — 2026-06-26*
