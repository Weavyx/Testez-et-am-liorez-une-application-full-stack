# Audit — occurrences restantes du type `any` (front/src)

Diagnostic READ-ONLY, réalisé le 2026-07-31, en réponse au point relevé par le mentor sur l'Ex1
(« Supprimer tous les `any` »). Aucune modification de fichier n'a été effectuée.

**Périmètre analysé** : `front/src/**/*.ts` (42 fichiers), hors `node_modules`, `dist`, `coverage`.
**Méthode** : recherche `\bany\b` (mot entier) sur tout le périmètre, puis relecture manuelle de
chaque résultat pour écarter les faux positifs (commentaires, mots contenant "any" comme "company",
noms de variables sans rapport avec le typage).

## Code source (hors tests)

| Fichier | Ligne | Contexte (extrait de la ligne) | Type de any (retour fonction / paramètre / variable / cast) |
|---|---|---|---|

**Aucune occurrence trouvée.** Le seul résultat brut du grep dans le code source hors tests se
situe dans `front/src/polyfills.ts` (lignes 34, 35, 36, 41 — `(window as any).__Zone_...`), mais il
s'agit de **code commenté** appartenant au bloc de documentation généré par défaut par Angular CLI
(`/** ... */`, lignes 21-43), et non de code exécuté. Ces occurrences ne constituent donc pas des
`any` réels dans le code source applicatif.

## Tests (.spec.ts)

| Fichier | Ligne | Contexte (extrait de la ligne) |
|---|---|---|
| `front/src/app/pages/sessions/components/list/list.component.spec.ts` | 137 | `.find((btn: any) => btn.textContent?.includes('Detail'));` |

Une seule occurrence trouvée dans les tests : un paramètre de callback typé `any` dans un test
Jest (`Array.prototype.find`), hors périmètre strict de la consigne d'Ex1 mais listée par prudence.

## Résumé chiffré

- **Code source applicatif (hors tests) : 0 occurrence réelle** (4 lignes matchées par le grep,
  toutes dans un commentaire de documentation de `polyfills.ts`, non comptabilisées).
- **Tests (.spec.ts) : 1 occurrence** (`list.component.spec.ts:137`).

## Vérification anti-régression

`git status --porcelain` avant écriture de ce fichier :
```
M back/pom.xml
```
Après ajout de ce fichier, seul `AUDIT_ANY_RESTANTS.md` apparaît comme nouveau fichier ; aucun
fichier existant n'a été modifié par cet audit.
