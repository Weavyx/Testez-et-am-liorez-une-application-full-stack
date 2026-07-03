import { HttpClientModule } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { expect } from '@jest/globals';

import { UserService } from './user.service';

/**
 * UserService — appels HTTP de lecture/suppression d'un compte utilisateur
 *
 * Cas du testing plan couverts :
 *   - Instanciation du service (les appels HTTP sont exercés indirectement
 *     via les tests de MeComponent)
 *
 * Répartition des tests (méthodologie stricte du projet) :
 *   - INTÉGRATION = le test lit lui-même le DOM réellement rendu et/ou
 *     vérifie une requête HTTP réelle via HttpTestingController.
 *   - UNITAIRE = tout le reste, même si TestBed sert de simple plomberie.
 *
 * Service pur, sans composant/DOM : tous les tests sont unitaires par
 * construction ici, pas de regroupement rendu/logique supplémentaire.
 */
describe('UserService', () => {
  let service: UserService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports:[
        HttpClientModule
      ]
    });
    service = TestBed.inject(UserService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
