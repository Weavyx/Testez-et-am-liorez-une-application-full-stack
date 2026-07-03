import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
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
 *
 * Répartition des tests (méthodologie stricte du projet) :
 *   - INTÉGRATION = le test lit lui-même le DOM réellement rendu et/ou
 *     vérifie une requête HTTP réelle via HttpTestingController.
 *   - UNITAIRE = tout le reste, même si TestBed/fixture servent de
 *     plomberie (instanciation) sans assertion sur ce qu'ils produisent.
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
      imports: [HttpClientTestingModule, MatCardModule, MatIconModule, ListComponent],
      providers: [
        provideRouter([]),
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
