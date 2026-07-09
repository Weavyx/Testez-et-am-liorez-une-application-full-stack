import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { expect } from '@jest/globals';

import { SessionApiService } from './session-api.service';

/**
 * SessionApiService — appels HTTP CRUD + participate/unParticipate sur les sessions
 *
 * Cas du testing plan couverts :
 *   - Instanciation du service (les appels HTTP CRUD sont exercés
 *     indirectement via les tests de ListComponent/DetailComponent/FormComponent)
 *
 * Répartition des tests (méthodologie stricte du projet) :
 *   - INTÉGRATION = le test lit lui-même le DOM réellement rendu et/ou
 *     vérifie une requête HTTP réelle via HttpTestingController.
 *   - UNITAIRE = tout le reste, même si TestBed sert de simple plomberie.
 *
 * Service pur, sans composant/DOM : tous les tests sont unitaires par
 * construction ici, pas de regroupement rendu/logique supplémentaire.
 */
describe('SessionsService', () => {
  let service: SessionApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers:[
        provideHttpClient(withInterceptorsFromDi())
      ]
    });
    service = TestBed.inject(SessionApiService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
