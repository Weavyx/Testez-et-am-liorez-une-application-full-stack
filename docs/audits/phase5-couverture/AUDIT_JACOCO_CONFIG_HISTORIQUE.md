# Audit — Origine de la configuration Jacoco (LINE-only) dans back/pom.xml

Diagnostic **read-only**, réalisé le 2026-07-26. Aucun fichier modifié. Objectif : déterminer si la règle `jacoco-check` de `back/pom.xml`, qui ne contrôle que le compteur `LINE` (aucune règle sur `INSTRUCTION` ni `BRANCH`), est une configuration héritée du starter OpenClassrooms ou un choix/oubli survenu pendant ce projet.

---

## Configuration actuelle

Extrait exact de `back/pom.xml` (exécution `jacoco-check` du plugin `jacoco-maven-plugin`, lignes 163–213 au moment de l'audit) :

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <!-- attached to Maven test phase -->
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>

        <execution>
            <id>jacoco-check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.9</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
                <!-- Packages sans logique métier propre (POJOs generes par Lombok/MapStruct) : exclus du perimetre de test de l'exercice -->
                <!-- SpringBootSecurityJwtApplication : classe de bootstrap Spring Boot (main() = SpringApplication.run(...)), sans branche ni comportement testable, meme raisonnement -->
                <excludes>
                    <exclude>com/openclassrooms/starterjwt/dto/**</exclude>
                    <exclude>com/openclassrooms/starterjwt/mapper/**</exclude>
                    <exclude>com/openclassrooms/starterjwt/payload/request/**</exclude>
                    <exclude>com/openclassrooms/starterjwt/payload/response/**</exclude>
                    <exclude>com/openclassrooms/starterjwt/SpringBootSecurityJwtApplication.class</exclude>
                </excludes>
            </configuration>
        </execution>

    </executions>
</plugin>
```

Un seul `<rule>`, un seul `<limit>` : `element=PACKAGE`, `counter=LINE`, `value=COVEREDRATIO`, `minimum=0.9`. Aucun élément `<limit>` avec `counter=INSTRUCTION` ou `counter=BRANCH` n'existe où que ce soit dans le fichier actuel (vérifié par recherche exhaustive de `<counter>` dans `back/pom.xml` : une seule occurrence, celle listée ci-dessus).

---

## Historique

```
git log --oneline -- back/pom.xml
```

```
d384f93 build(back): exclut la classe de bootstrap Spring Boot du check Jacoco
57b3363 test(back): ajoute les tests d'intégration de TeacherController
b87930c test(back): pose le socle des tests d'intégration Spring Boot + Testcontainers MySQL
834ae70 chore(back): relève jacoco-maven-plugin à 0.8.12
aff4d62 build(back): exclut dto/mapper/payload du check Jacoco
8f2a3c7 first commit
```

| Commit | Date | Message | Changement côté Jacoco |
|---|---|---|---|
| `8f2a3c7` | 2025-10-14 | first commit | **Introduit** la section `jacoco-check` telle quelle : `jacoco-maven-plugin` version `0.8.5`, règle unique `PACKAGE / LINE / COVEREDRATIO ≥ 0.9`, **aucune règle `INSTRUCTION` ou `BRANCH`**, aucun `<excludes>`. |
| `aff4d62` | 2026-07-21 14:51:27 | build(back): exclut dto/mapper/payload du check Jacoco | Ajoute uniquement le bloc `<excludes>` (dto/mapper/payload/request/response). **Ne touche pas** à `<rules>`/`<limits>`/`<counter>`. |
| `834ae70` | 2026-07-21 14:51:30 | chore(back): relève jacoco-maven-plugin à 0.8.12 | Bump de version `0.8.5` → `0.8.12` (compatibilité bytecode Java 21). **Ne touche pas** à `<rules>`/`<limits>`/`<counter>`. |
| `b87930c` | 2026-07-21 15:29:56 | test(back): pose le socle des tests d'intégration Spring Boot + Testcontainers MySQL | Diff sur `back/pom.xml` : bump `testcontainers.version` uniquement. **Aucun changement** dans la section jacoco. |
| `57b3363` | 2026-07-21 15:45:32 | test(back): ajoute les tests d'intégration de TeacherController | Ajoute le plugin `maven-surefire-plugin` (inclusion des `*IT.java`). **Aucun changement** dans la section jacoco (le diff n'insère du texte qu'avant le bloc `jacoco-maven-plugin`). |
| `d384f93` | 2026-07-24 11:58:30 | build(back): exclut la classe de bootstrap Spring Boot du check Jacoco | Ajoute une ligne à `<excludes>` (`SpringBootSecurityJwtApplication.class`) + un commentaire. **Ne touche pas** à `<rules>`/`<limits>`/`<counter>`. |

**Aucun commit, depuis le premier commit du repo, n'a jamais ajouté, modifié ou supprimé un élément `<rule>`, `<limit>` ou `<counter>`.** Les cinq commits qui touchent `back/pom.xml` après le premier ne portent que sur : le numéro de version du plugin, la liste `<excludes>` (packages hors périmètre de test), la version de Testcontainers, et l'ajout du plugin Surefire — jamais sur le contenu de la règle de couverture elle-même.

La règle `PACKAGE / LINE ≥ 0.9`, seule et unique règle du fichier, est présente **mot pour mot** (même `element`, même `counter`, même `value`, même `minimum`) dès `8f2a3c7` (« first commit », 2025-10-14), avant tout commit de fonctionnalité ou de test de ce projet.

---

## Conclusion

**La restriction au seul compteur `LINE` (absence de règle `INSTRUCTION`/`BRANCH`) est une configuration héritée telle quelle du starter OpenClassrooms d'origine (commit `8f2a3c7`, « first commit »), jamais modifiée depuis** — les cinq commits ultérieurs touchant `back/pom.xml` (`aff4d62`, `834ae70`, `b87930c`, `57b3363`, `d384f93`) ont tous porté sur d'autres aspects (version du plugin, exclusions de packages, dépendance Testcontainers, plugin Surefire) sans jamais toucher à la définition de la règle de couverture elle-même. Ce n'est donc ni un choix volontaire ni un oubli survenu pendant ce projet : c'est la configuration du starter, restée intacte.

---

## Vérification anti-régression

```
git status --porcelain
```

Résultat (avant ajout de ce fichier) : vide. Après écriture, seul `AUDIT_JACOCO_CONFIG_HISTORIQUE.md` apparaît comme fichier non suivi.
