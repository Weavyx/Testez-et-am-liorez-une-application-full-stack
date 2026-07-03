import { HttpClientModule } from '@angular/common/http';
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
import { SessionInformation } from 'src/app/core/models/sessionInformation.interface';
import { AuthService } from 'src/app/core/service/auth.service';
import { SessionService } from 'src/app/core/service/session.service';

import { LoginComponent } from './login.component';

/**
 * LoginComponent — page "Login"
 *
 * Cas du testing plan couverts :
 *   - Login : connexion réussie, mise à jour de la session et redirection
 *   - Login : échec de connexion, message d'erreur affiché
 *   - Login : champ obligatoire manquant → bouton submit désactivé
 *
 * Répartition des tests (méthodologie stricte du projet) :
 *   - INTÉGRATION = le test lit lui-même le DOM réellement rendu et/ou
 *     vérifie une requête HTTP réelle via HttpTestingController.
 *   - UNITAIRE = tout le reste, même si TestBed/fixture servent de
 *     plomberie (instanciation) sans assertion sur ce qu'ils produisent.
 */
describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: AuthService;
  let sessionService: SessionService;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [SessionService, provideRouter([])],
      imports: [
        BrowserAnimationsModule,
        HttpClientModule,
        MatCardModule,
        MatIconModule,
        MatFormFieldModule,
        MatInputModule,
        ReactiveFormsModule,LoginComponent]
    })
      .compileComponents();
    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService);
    sessionService = TestBed.inject(SessionService);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  describe('rendu (intégration DOM)', () => {
    it('should set onError to true and display an error message when login fails', () => {
      jest.spyOn(authService, 'login').mockReturnValue(throwError(() => new Error('Invalid credentials')));

      component.form.setValue({ email: 'wrong@test.com', password: 'wrongpass' });
      component.submit();
      fixture.detectChanges();

      expect(component.onError).toBe(true);
      const errorElement: HTMLElement = fixture.nativeElement.querySelector('.error');
      expect(errorElement).toBeTruthy();
      expect(errorElement.textContent).toContain('An error occurred');
    });

    it('should disable the submit button when a required field is missing', () => {
      component.form.setValue({ email: '', password: 'password' });
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

    it('should log in and navigate to /sessions on successful submit', () => {
      const sessionInfo: SessionInformation = {
        token: 'token',
        type: 'Bearer',
        id: 1,
        username: 'user@test.com',
        firstName: 'John',
        lastName: 'Doe',
        admin: false
      };
      jest.spyOn(authService, 'login').mockReturnValue(of(sessionInfo));
      const logInSpy = jest.spyOn(sessionService, 'logIn');
      const navigateSpy = jest.spyOn(router, 'navigate');

      component.form.setValue({ email: 'user@test.com', password: 'password' });
      component.submit();

      expect(authService.login).toHaveBeenCalledWith({ email: 'user@test.com', password: 'password' });
      expect(logInSpy).toHaveBeenCalledWith(sessionInfo);
      expect(navigateSpy).toHaveBeenCalledWith(['/sessions']);
      expect(component.onError).toBe(false);
    });
  });
});
