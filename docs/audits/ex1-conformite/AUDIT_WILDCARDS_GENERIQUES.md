# Audit — usages de génériques wildcard (`<?>` et bornés) hors `ResponseEntity<?>`

## Résumé

Deux recherches complémentaires ont été menées sur `back/src/main/java` :

1. `grep -rn "<?>"` (wildcard nu strict) : **18 occurrences**, toutes des `ResponseEntity<?>` déjà
   répertoriées et documentées dans `AUDIT_RESPONSEENTITY_WILDCARD.md`. **0 occurrence restante**
   après exclusion de ces 18.
2. Recherche élargie (regex couvrant aussi les wildcards bornés `<? extends X>` / `<? super X>`,
   au cas où `grep "<?>" ` aurait raté une forme bornée) : **1 occurrence supplémentaire** trouvée,
   qui n'est pas un wildcard nu mais un wildcard borné : `Collection<? extends GrantedAuthority>`
   dans `security/services/UserDetailsImpl.java:34`.

| Catégorie | Nombre |
|---|---|
| Total `<?>` bruts trouvés | 18 |
| Exclus car déjà couverts par `AUDIT_RESPONSEENTITY_WILDCARD.md` (`ResponseEntity<?>`) | 18 |
| Wildcards nus restants à documenter | **0** |
| Wildcards bornés trouvés en complément (`<? extends X>` / `<? super X>`) | 1 |

**Résultat négatif confirmé** : il n'existe aucun wildcard nu `<?>` dans `back/src/main/java` en
dehors des 18 `ResponseEntity<?>` déjà audités. Le seul autre usage de générique wildcard dans tout
le code de production est un wildcard borné, intentionnel par construction (voir tableau).

Aucune fausse détection à écarter (pas de faux positifs Javadoc, commentaire ou chaîne de
caractères rencontrés sur ce périmètre — toutes les occurrences de `<?>` étaient des déclarations
Java réelles).

Aucun fichier de production ni de test n'a été modifié dans le cadre de cet audit.

## Tableau détaillé (occurrence hors ResponseEntity)

| Fichier | Ligne | Contexte (signature) | Type réel utilisé à l'usage | Wildcard justifié ou générique par défaut/oubli ? | Type cible proposé |
|---|---|---|---|---|---|
| `security/services/UserDetailsImpl.java` | 34 | `public Collection<? extends GrantedAuthority> getAuthorities()` | Toujours `List.of(new SimpleGrantedAuthority(...))` — une seule implémentation concrète (`SimpleGrantedAuthority`) selon que `admin` est vrai ou faux (`ROLE_ADMIN` / `ROLE_USER`). | **Justifié** : cette méthode surcharge `UserDetails.getAuthorities()` de Spring Security, dont la signature d'interface impose exactement `Collection<? extends GrantedAuthority>`. Le wildcard n'est pas un choix du projet mais une contrainte du contrat d'interface externe — il ne peut pas être resserré sans rompre l'implémentation de `UserDetails`. | Aucun changement possible/pertinent : le type est dicté par l'interface `UserDetails` (hors périmètre du code applicatif). |
