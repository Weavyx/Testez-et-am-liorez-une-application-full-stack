import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { expect, jest } from '@jest/globals';
import { SessionInformation } from './core/models/sessionInformation.interface';
import { SessionService } from './core/service/session.service';

import { AppComponent } from './app.component';

/**
 * AppComponent — coquille racine de l'application (toolbar + router-outlet)
 *
 * Cas du testing plan couverts :
 *   - Bootstrap : l'application se crée sans erreur
 *   - Logout : clic sur le lien "Logout" de la toolbar → déconnexion et
 *     redirection vers '/'
 *   - Session : $isLogged() reflète l'état de connexion (false puis true)
 *
 * Répartition des tests (méthodologie stricte du projet) :
 *   - INTÉGRATION = le test lit lui-même le DOM réellement rendu.
 *   - UNITAIRE = tout le reste, y compris les tests qui vérifient le contrat
 *     HTTP réel (URL/verbe/payload) via HttpTestingController — ce dernier
 *     mocke le backend, aucun réseau ni serveur réel n'est impliqué — même
 *     si TestBed/fixture servent de plomberie (instanciation) sans
 *     assertion sur ce qu'ils produisent.
 */
describe('AppComponent', () => {
  const mockSessionInfo: SessionInformation = {
    token: 'token',
    type: 'Bearer',
    id: 1,
    username: 'user@test.com',
    firstName: 'John',
    lastName: 'Doe',
    admin: false
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        MatToolbarModule,AppComponent
      ],
      providers: [
        provideHttpClient(withInterceptorsFromDi())
      ],
    }).compileComponents();
  });

  describe('logique isolée (unitaire)', () => {
    it('should create the app', () => {
      const fixture = TestBed.createComponent(AppComponent);
      const app = fixture.componentInstance;
      expect(app).toBeTruthy();
    });

    it('should reflect the session state through $isLogged() (false, then true after logIn)', () => {
      const fixture = TestBed.createComponent(AppComponent);
      const app = fixture.componentInstance;
      const sessionService = TestBed.inject(SessionService);

      let emitted: boolean | undefined;
      app.$isLogged().subscribe(v => (emitted = v));
      expect(emitted).toBe(false);

      sessionService.logIn(mockSessionInfo);
      app.$isLogged().subscribe(v => (emitted = v));
      expect(emitted).toBe(true);
    });
  });

  describe('rendu (intégration DOM)', () => {
    it('should display Login/Register links (not Sessions/Account/Logout) when not logged in', () => {
      const fixture = TestBed.createComponent(AppComponent);
      fixture.detectChanges();

      const nativeElement = fixture.nativeElement as HTMLElement;
      const links = Array.from(nativeElement.querySelectorAll('.link')).map((el) => el.textContent?.trim());

      expect(links).toContain('Login');
      expect(links).toContain('Register');
      expect(links).not.toContain('Sessions');
      expect(links).not.toContain('Account');
      expect(links).not.toContain('Logout');
    });

    it('should log out and navigate to "/" when the Logout link is clicked', () => {
      const sessionService = TestBed.inject(SessionService);
      const router = TestBed.inject(Router);
      sessionService.logIn(mockSessionInfo);

      const fixture = TestBed.createComponent(AppComponent);
      fixture.detectChanges();

      const nativeElement = fixture.nativeElement as HTMLElement;
      const logoutLink = Array.from(nativeElement.querySelectorAll('span.link'))
        .find((el) => el.textContent?.includes('Logout')) as HTMLElement;
      expect(logoutLink).toBeTruthy();

      const logOutSpy = jest.spyOn(sessionService, 'logOut');
      const navigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);

      logoutLink.click();

      expect(logOutSpy).toHaveBeenCalled();
      expect(navigateSpy).toHaveBeenCalledWith(['']);
    });
  });
});
