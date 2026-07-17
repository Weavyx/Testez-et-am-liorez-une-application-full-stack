import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { expect } from '@jest/globals';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../core/service/auth.service';

import { RegisterComponent } from './register.component';

/**
 * RegisterComponent — page "Register"
 *
 * Cas du testing plan couverts :
 *   - Register : inscription réussie et redirection vers /login
 *   - Register : échec d'inscription, message d'erreur affiché
 *   - Register : champ obligatoire manquant → bouton submit désactivé
 *
 * Répartition des tests (méthodologie stricte du projet) :
 *   - INTÉGRATION = le test lit lui-même le DOM réellement rendu.
 *   - UNITAIRE = tout le reste, y compris les tests qui vérifient le contrat
 *     HTTP réel (URL/verbe/payload) via HttpTestingController — ce dernier
 *     mocke le backend, aucun réseau ni serveur réel n'est impliqué — même
 *     si TestBed/fixture servent de plomberie (instanciation) sans
 *     assertion sur ce qu'ils produisent.
 */
describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let authService: AuthService;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptorsFromDi())],
      imports: [
        BrowserAnimationsModule,
        ReactiveFormsModule,
        MatCardModule,
        MatFormFieldModule,
        MatIconModule,
        MatInputModule,RegisterComponent
      ]
    })
      .compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  describe('rendu (intégration DOM)', () => {
    it('should set onError to true when registration fails', () => {
      jest.spyOn(authService, 'register').mockReturnValue(throwError(() => new Error('Email already used')));

      component.form.setValue({
        email: 'user@test.com',
        firstName: 'John',
        lastName: 'Doe',
        password: 'password'
      });
      component.submit();
      fixture.detectChanges();

      expect(component.onError).toBe(true);
      const errorElement: HTMLElement = fixture.nativeElement.querySelector('.error');
      expect(errorElement).toBeTruthy();
    });

    it('should disable the submit button when a required field is missing', () => {
      component.form.setValue({
        email: '',
        firstName: 'John',
        lastName: 'Doe',
        password: 'password'
      });
      fixture.detectChanges();

      const submitButton: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
      expect(component.form.invalid).toBe(true);
      expect(submitButton.disabled).toBe(true);
    });
  });

  describe('logique isolée (unitaire)', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should register and navigate to /login on successful submit', () => {
      jest.spyOn(authService, 'register').mockReturnValue(of(undefined));
      const navigateSpy = jest.spyOn(router, 'navigate');

      component.form.setValue({
        email: 'user@test.com',
        firstName: 'John',
        lastName: 'Doe',
        password: 'password'
      });
      component.submit();

      expect(authService.register).toHaveBeenCalledWith({
        email: 'user@test.com',
        firstName: 'John',
        lastName: 'Doe',
        password: 'password'
      });
      expect(navigateSpy).toHaveBeenCalledWith(['/login']);
      expect(component.onError).toBe(false);
    });
  });
});
