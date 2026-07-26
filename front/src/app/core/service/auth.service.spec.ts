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
 * Répartition des tests (méthodologie stricte du projet, cf. METHODE_AUDIT.md
 * — note « HttpTestingController et la notion d'intégration ») :
 *   - INTÉGRATION = le test lit le DOM réellement rendu, OU vérifie une
 *     requête HTTP via HttpTestingController (coordination réelle
 *     service/HTTP jusqu'à la construction de la requête, sans mock
 *     intermédiaire — seul le transport final est intercepté), même sans
 *     assertion sur le DOM rendu.
 *   - UNITAIRE = appel de méthode isolée + assertion sur une propriété de
 *     classe, sans passer par HttpTestingController.
 *
 * Service pur, sans DOM : tous les tests ci-dessous sont donc des tests
 * D'INTÉGRATION au sens de ce critère, car ils vérifient le contrat HTTP
 * (URL, verbe, payload) via HttpTestingController.
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

  describe('login', () => {
    // Unitaire : vérifie le contrat HTTP mocké (verbe + URL + body) et la réponse transmise
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
  });

  describe('register', () => {
    // Unitaire : vérifie le contrat HTTP mocké (verbe + URL + body) sans payload de retour
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
});
