# Audits

Ces fichiers documentent la démarche d'audit menée après l'Exercice 2 (ainsi que l'état initial du dépôt et une demande adressée au mentor, hors périmètre de cette démarche) ; pour l'état final du projet, voir [DETTE_ET_SUIVI.md](../../DETTE_ET_SUIVI.md) et [README.md](../../README.md) à la racine.

## Structure

- **etat-initial/** — photographie du dépôt avant toute intervention.
- **phase0-dette/** — inventaire de la dette technique et des résidus de debug initiaux.
- **phase1-metier/** — audit des zones métier (authentification, utilisateurs, autorisation) et du bug de contrainte unique sur les enseignants.
- **phase3-tests-forme/** — inventaire et classement des tests existants, nommage et isolation des tests d'intégration.
- **phase4-tests-fond/** — analyse du fond des tests : comportements couverts, points restants, qualité et jugement des assertions.
- **phase5-couverture/** — mesures de couverture finales et historique de la configuration JaCoCo.
- **ex1-conformite/** — audits de conformité aux exigences de l'Exercice 1 (typage, wildcards, couches, gestion des erreurs, etc.).
- **demande-mentor/** — audit rédigé en support d'une demande adressée au mentor.

## Notes de succession

1. [AUDIT_WILDCARDS_JAVA.md](ex1-conformite/AUDIT_WILDCARDS_JAVA.md) (31/07, wildcards back en général) est le prédécesseur de [AUDIT_RESPONSEENTITY_WILDCARD.md](ex1-conformite/AUDIT_RESPONSEENTITY_WILDCARD.md) (07/08, spécifiquement `ResponseEntity<?>`, 18 occurrences) — se référer au second pour l'état final.
2. [AUDIT_PHASE5_COUVERTURE_FINALE.md](phase5-couverture/AUDIT_PHASE5_COUVERTURE_FINALE.md) (26/07, mesure intermédiaire, 126 tests back) est le prédécesseur de [AUDIT_METRIQUES_FINALES.md](phase5-couverture/AUDIT_METRIQUES_FINALES.md) (07/08, mesure finale, 180 tests back, référencée dans le README à la racine) — se référer au second pour l'état final.

## Note sur l'écart de comptage

[AUDIT_PHASE4_COMPORTEMENTS_TESTES.md](phase4-tests-fond/AUDIT_PHASE4_COMPORTEMENTS_TESTES.md) (26/07) mentionne 190 tests recensés en Phase 3, contre 180 dans [AUDIT_METRIQUES_FINALES.md](phase5-couverture/AUDIT_METRIQUES_FINALES.md) (07/08). Cet écart est normal : il résulte d'un regroupement/d'une simplification de tests entre les deux dates, et ne constitue pas une contradiction à investiguer.
