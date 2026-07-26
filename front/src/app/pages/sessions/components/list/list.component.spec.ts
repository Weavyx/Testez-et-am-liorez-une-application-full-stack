import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { provideRouter } from '@angular/router';
import { expect } from '@jest/globals';
import { Session } from 'src/app/core/models/session.interface';
import { SessionService } from 'src/app/core/service/session.service';

import { ListComponent } from './list.component';

/**
 * ListComponent — page "Rentals available" (Sessions list)
 *
 * Cas du testing plan couverts :
 *   - Sessions list : affichage des sessions renvoyées par l'API
 *   - Sessions list : visibilité du bouton "Create" selon le rôle (admin/non-admin)
 *   - Sessions list : visibilité du bouton "Detail" pour un utilisateur non-admin
 *     (comportement volontaire, cf. commentaire sur le test dédié — écart
 *     documenté avec le libellé du testing plan officiel)
 *
 * Répartition des tests (méthodologie stricte du projet, cf. METHODE_AUDIT.md
 * — note « HttpTestingController et la notion d'intégration ») :
 *   - INTÉGRATION = le test lit le DOM réellement rendu, OU vérifie une
 *     requête HTTP via HttpTestingController (coordination réelle
 *     composant/service jusqu'à la construction de la requête, sans mock
 *     intermédiaire — seul le transport final est intercepté), même sans
 *     assertion sur le DOM rendu.
 *   - UNITAIRE = appel de méthode isolée + assertion sur une propriété de
 *     classe, sans passer par HttpTestingController.
 */
describe('ListComponent', () => {
  let fixture: ComponentFixture<ListComponent>;
  let httpMock: HttpTestingController;

  const mockAdminSessionService = {
    sessionInformation: {
      admin: true
    }
  };

  const mockNonAdminSessionService = {
    sessionInformation: {
      admin: false
    }
  };

  const mockSessions: Session[] = [
    {
      id: 1,
      name: 'Yoga débutant',
      description: 'Une session pour découvrir le yoga en douceur.',
      date: new Date('2023-06-01'),
      teacher_id: 1,
      users: []
    }
  ];

  const setup = async (sessionService: unknown): Promise<void> => {
    await TestBed.configureTestingModule({
      imports: [MatCardModule, MatIconModule, ListComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: SessionService, useValue: sessionService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ListComponent);
    httpMock = TestBed.inject(HttpTestingController);
  };

  afterEach(() => {
    httpMock.verify();
  });

  describe('rendu (intégration DOM)', () => {
    it('should fetch and display the sessions returned by the API in the DOM', async () => {
      await setup(mockAdminSessionService);
      fixture.detectChanges();

      const req = httpMock.expectOne('api/session');
      expect(req.request.method).toBe('GET');
      req.flush(mockSessions);

      fixture.detectChanges();

      const text: string = fixture.nativeElement.textContent;
      expect(text).toContain(mockSessions[0].name);
      expect(text).toContain(mockSessions[0].description);
    });

    it('should display the "Create" button when the user is admin', async () => {
      await setup(mockAdminSessionService);
      fixture.detectChanges();

      const req = httpMock.expectOne('api/session');
      req.flush([]);
      fixture.detectChanges();

      const text: string = fixture.nativeElement.textContent;
      expect(text).toContain('Create');
    });

    it('should not display the "Create" button when the user is not admin', async () => {
      await setup(mockNonAdminSessionService);
      fixture.detectChanges();

      const req = httpMock.expectOne('api/session');
      req.flush([]);
      fixture.detectChanges();

      const text: string = fixture.nativeElement.textContent;
      expect(text).not.toContain('Create');
    });

    // Le testing plan officiel énonce (à tort, de façon imprécise) que les
    // boutons "Create" et "Detail" seraient tous deux conditionnés au rôle
    // admin. Ce n'est vrai que pour "Create" : dans list.component.html, le
    // bouton "Detail" est placé hors de tout @if et est donc rendu pour
    // chaque session, quel que soit le rôle. Ce n'est pas un oubli : l'écran
    // Detail porte aussi le flux Participate/Do not participate, que tout
    // utilisateur non-admin doit pouvoir déclencher. Ce test documente ce
    // comportement volontaire (et l'écart avec le libellé du plan), il ne
    // doit pas être "corrigé" pour masquer Detail aux non-admins.
    it('should display the Detail button even for a non-admin user (intentional: Detail also drives the participate/unparticipate flow)', async () => {
      await setup(mockNonAdminSessionService);
      fixture.detectChanges();

      const req = httpMock.expectOne('api/session');
      req.flush(mockSessions);
      fixture.detectChanges();

      const detailButton = Array.from(fixture.nativeElement.querySelectorAll('button'))
        .find((btn: any) => btn.textContent?.includes('Detail'));
      expect(detailButton).toBeDefined();
    });
  });

  describe('logique isolée (unitaire)', () => {
    it('should create', async () => {
      await setup(mockAdminSessionService);
      fixture.detectChanges();

      const req = httpMock.expectOne('api/session');
      req.flush([]);

      expect(fixture.componentInstance).toBeTruthy();
    });
  });
});
