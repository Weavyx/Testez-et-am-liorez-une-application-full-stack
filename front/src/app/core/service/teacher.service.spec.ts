import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { expect } from '@jest/globals';

import { TeacherService } from './teacher.service';

/**
 * TeacherService — appels HTTP de lecture des enseignants (all / detail)
 *
 * Cas du testing plan couverts :
 *   - Instanciation du service (les appels HTTP sont exercés indirectement
 *     via les tests de FormComponent/DetailComponent)
 *
 * Répartition des tests (méthodologie stricte du projet) :
 *   - INTÉGRATION = le test lit lui-même le DOM réellement rendu et/ou
 *     vérifie une requête HTTP réelle via HttpTestingController.
 *   - UNITAIRE = tout le reste, même si TestBed sert de simple plomberie.
 *
 * Service pur, sans composant/DOM : tous les tests sont unitaires par
 * construction ici, pas de regroupement rendu/logique supplémentaire.
 */
describe('TeacherService', () => {
  let service: TeacherService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers:[
        provideHttpClient(withInterceptorsFromDi())
      ]
    });
    service = TestBed.inject(TeacherService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
