import { TestBed } from '@angular/core/testing';
import { expect } from '@jest/globals';
import { provideRouter, Router } from '@angular/router';

import { UnauthGuard } from './unauth.guard';
import { SessionService } from '../core/service/session.service';
import { SessionInformation } from '../core/models/sessionInformation.interface';

/**
 * UnauthGuard — protège /login et /register contre un utilisateur déjà connecté
 *
 * Cas du testing plan couverts :
 *   - Utilisateur non connecté : accès autorisé (canActivate() === true)
 *   - Utilisateur déjà connecté : accès bloqué, redirection vers une route existante
 *
 * UNITAIRE : aucun DOM rendu, guard exécuté directement via TestBed.inject.
 */
describe('UnauthGuard', () => {
  let guard: UnauthGuard;
  let sessionService: SessionService;
  let router: Router;

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
    TestBed.configureTestingModule({
      providers: [SessionService, provideRouter([])]
    });
    guard = TestBed.inject(UnauthGuard);
    sessionService = TestBed.inject(SessionService);
    router = TestBed.inject(Router);
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  it('should allow activation when the user is not logged in', () => {
    expect(guard.canActivate()).toBe(true);
  });

  it('should block activation and redirect to an existing route when the user is already logged in', () => {
    sessionService.logIn(mockUser);
    const navigateSpy = jest.spyOn(router, 'navigate');

    const result = guard.canActivate();

    expect(result).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith(['sessions']);
  });
});
