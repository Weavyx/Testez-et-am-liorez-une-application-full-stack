import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { expect } from '@jest/globals';
import { LoginRequest } from '../models/loginRequest.interface';
import { RegisterRequest } from '../models/registerRequest.interface';
import { SessionInformation } from '../models/sessionInformation.interface';

import { AuthService } from './auth.service';

/**
 * AuthService — appels HTTP d'authentification (login / register)
 *
 * Cas du testing plan couverts :
 *   - Login : requête POST vers /api/auth/login avec les identifiants
 *   - Register : requête POST vers /api/auth/register avec les données d'inscription
 *
 * Répartition des tests (méthodologie stricte du projet) :
 *   - INTÉGRATION = le test lit lui-même le DOM réellement rendu et/ou
 *     vérifie une requête HTTP réelle via HttpTestingController.
 *   - UNITAIRE = tout le reste, même si TestBed sert de simple plomberie.
 *
 * Service pur, sans composant/DOM : tous les tests sont unitaires par
 * construction ici, pas de regroupement rendu/logique supplémentaire.
 */
describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should send a POST request to api/auth/login with the credentials', () => {
    const loginRequest: LoginRequest = { email: 'user@test.com', password: 'password' };
    const sessionInfo: SessionInformation = {
      token: 'token',
      type: 'Bearer',
      id: 1,
      username: 'user@test.com',
      firstName: 'John',
      lastName: 'Doe',
      admin: false
    };

    service.login(loginRequest).subscribe((response) => {
      expect(response).toEqual(sessionInfo);
    });

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(loginRequest);
    req.flush(sessionInfo);
  });

  it('should send a POST request to api/auth/register with the registration data', () => {
    const registerRequest: RegisterRequest = {
      email: 'user@test.com',
      firstName: 'John',
      lastName: 'Doe',
      password: 'password'
    };

    service.register(registerRequest).subscribe((response) => {
      expect(response).toBeUndefined();
    });

    const req = httpMock.expectOne('/api/auth/register');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(registerRequest);
    req.flush(null);
  });
});
