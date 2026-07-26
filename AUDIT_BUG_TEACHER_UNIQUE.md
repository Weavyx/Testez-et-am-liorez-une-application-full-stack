# Audit — Bug contrainte UNIQUE sur sessions.teacher_id

Contexte : vérification manuelle Postman (hors Claude Code) a confirmé que la
création d'une deuxième session pour un enseignant déjà associé à une session
existante échoue avec une `DataIntegrityViolationException` (contrainte MySQL
`UKj01m3te676lnn3e956dg7s2mr` sur `sessions.teacher_id`, confirmée par
`SHOW CREATE TABLE sessions`). Ce document est un diagnostic READ-ONLY :
aucune correction n'a été appliquée.

## Diagnostic

### Relation Session → Teacher

Fichier : `back/src/main/java/com/openclassrooms/starterjwt/models/Session.java`, lignes 63-65 :

```java
@OneToOne
@JoinColumn(name = "teacher_id", referencedColumnName = "id")
private Teacher teacher;
```

Il s'agit d'une relation `@OneToOne` unidirectionnelle portant la clé étrangère
(`@JoinColumn`) côté `Session`, sans attribut `unique` explicite sur
`@JoinColumn`.

### Relation inverse côté Teacher

Fichier : `back/src/main/java/com/openclassrooms/starterjwt/models/Teacher.java`

Aucune relation vers `Session` n'est mappée dans cette classe (champs présents :
`id`, `lastName`, `firstName`, `createdAt`, `updatedAt` uniquement). Il n'y a
donc pas de relation inverse `@OneToOne(mappedBy = ...)` côté `Teacher` — la
relation est strictement unidirectionnelle, portée par `Session`.

### Configuration ddl-auto

Fichier : `back/src/main/resources/application.yml`, lignes 11-13 :

```yaml
  jpa:
    hibernate:
      ddl-auto: update
```

Le schéma est donc généré par Hibernate lui-même (`ddl-auto=update`), à partir
des annotations JPA des entités — pas depuis un script SQL externe.

### Absence de script SQL ou de schéma explicite

- `back/src/main/resources/sql/insert_teacher.sql` et
  `back/src/main/resources/sql/insert_user.sql` : contiennent uniquement des
  instructions `INSERT`, aucun DDL (`CREATE TABLE`, `ALTER TABLE`, `UNIQUE`, etc.).
- Aucun fichier `schema.sql`, `data.sql`, `migration`, ou script Flyway/Liquibase
  n'existe dans `back/src/main/resources` (recherche exhaustive du dossier).
- `back/compose.yaml` : le service MySQL ne monte aucun volume d'initialisation
  (`docker-entrypoint-initdb.d` ou équivalent) — le conteneur démarre avec une
  base vide, entièrement peuplée par le DDL généré par Hibernate.

Aucun script SQL indépendant ne définit donc la contrainte : elle n'est
imposée nulle part explicitement dans le code applicatif.

## Conclusion

La contrainte UNIQUE sur `sessions.teacher_id` provient du comportement par
défaut de Hibernate pour une relation `@OneToOne` portée par `@JoinColumn` :
en l'absence de `unique = false` explicite sur l'annotation `@JoinColumn` du
champ `teacher` dans `Session.java` (ligne 64), Hibernate génère
automatiquement une contrainte UNIQUE sur la colonne de jointure, reproduite
en base par le mode `ddl-auto=update` défini dans `application.yml` (ligne 13).
Aucun script SQL externe n'intervient dans cette contrainte — le mapping JPA
est donc la seule et unique source à corriger en phase de correction (fichier
`Session.java`, ligne 63-65 : ajouter `unique = false` sur `@JoinColumn`, ou
remplacer la relation `@OneToOne` par une relation `@ManyToOne` si la
sémantique métier « un enseignant peut animer plusieurs sessions » doit être
reflétée fidèlement).
