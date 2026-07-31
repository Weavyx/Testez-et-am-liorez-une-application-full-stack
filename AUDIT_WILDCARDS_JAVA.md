# Audit — wildcards génériques `<?>` (backend Java)

Diagnostic READ-ONLY réalisé le 2026-07-31, en complément de `AUDIT_ANY_RESTANTS.md` (qui porte
sur le type `any` TypeScript côté front). Aucune modification de fichier n'a été effectuée.

**Périmètre analysé** : `back/src/**/*.java`.

## Occurrences `ResponseEntity<?>` (code source, `main/`)

| Fichier | Ligne | Contexte (extrait de la ligne) |
|---|---|---|
| `AuthController.java` | 38 | `public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {` |
| `AuthController.java` | 55 | `public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {` |
| `SessionController.java` | 37 | `public ResponseEntity<?> findById(@PathVariable("id") String id) {` |
| `SessionController.java` | 42 | `public ResponseEntity<?> findAll() {` |
| `SessionController.java` | 49 | `public ResponseEntity<?> create(@Valid @RequestBody SessionDto sessionDto) {` |
| `SessionController.java` | 59 | `public ResponseEntity<?> update(@PathVariable("id") String id, @Valid @RequestBody SessionDto sessionDto) {` |
| `SessionController.java` | 66 | `public ResponseEntity<?> save(@PathVariable("id") String id) {` |
| `SessionController.java` | 72 | `public ResponseEntity<?> participate(@PathVariable("id") String id, @PathVariable("userId") String userId) {` |
| `SessionController.java` | 79 | `public ResponseEntity<?> noLongerParticipate(@PathVariable("id") String id, @PathVariable("userId") String userId) {` |
| `UserController.java` | 27 | `public ResponseEntity<?> findById(@PathVariable("id") String id) {` |
| `UserController.java` | 33 | `public ResponseEntity<?> save(@PathVariable("id") String id) {` |
| `TeacherController.java` | 28 | `public ResponseEntity<?> findById(@PathVariable("id") String id) {` |
| `TeacherController.java` | 33 | `public ResponseEntity<?> findAll() {` |
| `GlobalExceptionHandler.java` | 13 | `public ResponseEntity<?> handleNumberFormatException(NumberFormatException e) {` |
| `GlobalExceptionHandler.java` | 18 | `public ResponseEntity<?> handleBadRequestException(BadRequestException e) {` |
| `GlobalExceptionHandler.java` | 26 | `public ResponseEntity<?> handleNotFoundException(NotFoundException e) {` |
| `GlobalExceptionHandler.java` | 31 | `public ResponseEntity<?> handleForbiddenException(ForbiddenException e) {` |

Total : **17 occurrences** de `ResponseEntity<?>` en code source (main). (Note : le décompte initial
communiqué en conversation était de 15 ; en relisant le grep, `GlobalExceptionHandler.java` compte
4 occurrences et non 2, portant le total exact à 17.)

## `any` TypeScript vs wildcard générique Java `<?>` — pourquoi ce n'est pas le même problème

Le `any` TypeScript désactive la vérification de type à la compilation : le compilateur n'offre
plus aucune garantie sur la forme de la valeur, ce qui est la définition même d'une perte de
typage. Le wildcard Java `<?>` dans `ResponseEntity<?>` est l'inverse : c'est un mécanisme du
système de types de Java qui dit « ce endpoint peut renvoyer des corps de réponse de types
différents selon le cas (payload de succès, message d'erreur, etc.), et le type exact n'a pas
besoin d'être connu à l'appel » — la vérification de type reste pleinement active à la
compilation, seul le paramètre de type générique est laissé non contraint. C'est un idiome Spring
standard pour les contrôleurs REST, pas un contournement du typage.

## Conclusion

**Aucune correction nécessaire.** Ce constat ne relève pas de la consigne d'Ex1 (« supprimer tous
les `any` »), qui porte spécifiquement sur le typage TypeScript côté `front/`. Les wildcards
génériques Java `<?>` du backend sont un usage idiomatique et volontaire, sans rapport avec le
problème de typage visé par le mentor.
