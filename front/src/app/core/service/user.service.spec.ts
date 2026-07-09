import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { expect } from '@jest/globals';

import { UserService } from './user.service';
import { User } from '../models/user.interface';

/**
 * UserService — appels HTTP de lecture/suppression d'un compte utilisateur
 *
 * Cas du testing plan couverts :
 *   - Récupération des informations utilisateur (getById)
 *   - Suppression du compte utilisateur (delete)
 *
 * Répartition des tests (méthodologie stricte du projet) :
 *   - INTÉGRATION = le test lit lui-même le DOM réellement rendu et/ou
 *     vérifie une requête HTTP réelle via HttpTestingController.
 *   - UNITAIRE = tout le reste, même si TestBed sert de simple plomberie.
 *
 * Service pur, sans DOM : les tests ci-dessous vérifient le contrat HTTP réel
 * (URL, verbe, payload) via HttpTestingController → classés INTÉGRATION.
 */
describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  const mockUser: User = {
    id: 1,
    email: 'test@studio-yoga.com',
    lastName: 'Doe',
    firstName: 'John',
    admin: false,
    password: 'encoded-password',
    createdAt: new Date('2024-01-01'),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getById', () => {
    // Intégration : vérifie la requête HTTP réelle (verbe + URL) et la réponse transmise
    it('should send a GET request to api/user/:id and return the user', () => {
      service.getById('1').subscribe((user) => {
        expect(user).toEqual(mockUser);
      });

      const req = httpMock.expectOne('api/user/1');
      expect(req.request.method).toBe('GET');
      req.flush(mockUser);
    });
  });

  describe('delete', () => {
    // Intégration : vérifie la requête HTTP réelle (verbe + URL) sans payload de retour
    it('should send a DELETE request to api/user/:id', () => {
      service.delete('1').subscribe((response) => {
        expect(response).toBeUndefined();
      });

      const req = httpMock.expectOne('api/user/1');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
