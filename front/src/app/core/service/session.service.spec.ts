import { TestBed } from '@angular/core/testing';
import { expect } from '@jest/globals';
import { SessionInformation } from '../models/sessionInformation.interface';

import { SessionService } from './session.service';

/**
 * SessionService — source de vérité unique de l'état d'authentification en mémoire
 *
 * Cas du testing plan couverts :
 *   - Session : état initial non connecté
 *   - Session : connexion (logIn) met à jour isLogged/sessionInformation
 *   - Session : déconnexion (logOut) réinitialise l'état
 *   - Session : notification de l'état via l'observable $isLogged()
 *
 * Répartition des tests (méthodologie stricte du projet) :
 *   - INTÉGRATION = le test lit lui-même le DOM réellement rendu et/ou
 *     vérifie une requête HTTP réelle via HttpTestingController.
 *   - UNITAIRE = tout le reste, même si TestBed sert de simple plomberie.
 *
 * Service pur, sans composant/DOM : tous les tests sont unitaires par
 * construction ici, pas de regroupement rendu/logique supplémentaire.
 */
describe('SessionService', () => {
  let service: SessionService;

  const mockUser: SessionInformation = {
    token: 'fake-jwt',
    type: 'Bearer',
    id: 1,
    username: 'john@test.com',
    firstName: 'John',
    lastName: 'Doe',
    admin: false
  };

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SessionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should initialize as not logged in', () => {
    expect(service.isLogged).toBe(false);
    expect(service.sessionInformation).toBeUndefined();
  });

  describe('$isLogged()', () => {
    it('should emit false initially', () => {
      let emitted: boolean | undefined;
      service.$isLogged().subscribe(v => (emitted = v));
      expect(emitted).toBe(false);
    });

    it('should emit true after logIn', () => {
      service.logIn(mockUser);
      let emitted: boolean | undefined;
      service.$isLogged().subscribe(v => (emitted = v));
      expect(emitted).toBe(true);
    });

    it('should emit false after logOut', () => {
      service.logIn(mockUser);
      service.logOut();
      let emitted: boolean | undefined;
      service.$isLogged().subscribe(v => (emitted = v));
      expect(emitted).toBe(false);
    });
  });

  describe('logIn()', () => {
    it('should store the user in sessionInformation', () => {
      service.logIn(mockUser);
      expect(service.sessionInformation).toEqual(mockUser);
    });

    it('should set isLogged to true', () => {
      service.logIn(mockUser);
      expect(service.isLogged).toBe(true);
    });
  });

  describe('logOut()', () => {
    it('should clear sessionInformation', () => {
      service.logIn(mockUser);
      service.logOut();
      expect(service.sessionInformation).toBeUndefined();
    });

    it('should set isLogged to false', () => {
      service.logIn(mockUser);
      service.logOut();
      expect(service.isLogged).toBe(false);
    });
  });
});
