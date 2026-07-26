import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { expect } from '@jest/globals';
import { Session } from '../../../../core/models/session.interface';
import { Teacher } from '../../../../core/models/teacher.interface';
import { SessionService } from '../../../../core/service/session.service';

import { DetailComponent } from './detail.component';

const mockSession: Session = {
  id: 1,
  name: 'Yoga session',
  description: 'A relaxing session',
  date: new Date('2026-01-01'),
  teacher_id: 1,
  users: [1],
  createdAt: new Date('2025-01-01'),
  updatedAt: new Date('2025-06-01'),
};

const mockTeacher: Teacher = {
  id: 1,
  lastName: 'DELAHAYE',
  firstName: 'Margot',
  createdAt: new Date('2025-01-01'),
  updatedAt: new Date('2025-06-01'),
};

function configureTestBed(sessionInformation: { admin: boolean; id: number }) {
  return TestBed.configureTestingModule({
    imports: [
      MatSnackBarModule,
      ReactiveFormsModule,
      DetailComponent
    ],
    providers: [
      provideRouter([]),
      provideHttpClient(withInterceptorsFromDi()),
      provideHttpClientTesting(),
      { provide: SessionService, useValue: { sessionInformation } },
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }) } } }
    ],
  }).compileComponents();
}

/**
 * DetailComponent — page "Session detail"
 *
 * Cas du testing plan couverts :
 *   - Session detail : affichage des informations (nom, description, professeur, date)
 *   - Session detail : actions admin (bouton Delete, suppression)
 *   - Session detail : actions non-admin (Participate / UnParticipate)
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
 *
 * La structure fonctionnelle existante (as an admin user / as a non-admin
 * user who has(n't) joined) est conservée ; le classement
 * unitaire/intégration est appliqué à l'intérieur de chacune de ces
 * sections.
 */
describe('DetailComponent', () => {
  let component: DetailComponent;
  let fixture: ComponentFixture<DetailComponent>;
  let service: SessionService;
  let httpMock: HttpTestingController;
  let router: Router;

  describe('as an admin user', () => {
    beforeEach(async () => {
      await configureTestBed({ admin: true, id: 1 });
      service = TestBed.inject(SessionService);
      httpMock = TestBed.inject(HttpTestingController);
      router = TestBed.inject(Router);
      jest.spyOn(router, 'navigate').mockResolvedValue(true);

      fixture = TestBed.createComponent(DetailComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      httpMock.expectOne({ url: 'api/session/1', method: 'GET' }).flush(mockSession);
      httpMock.expectOne({ url: 'api/teacher/1', method: 'GET' }).flush(mockTeacher);
      fixture.detectChanges();
    });

    afterEach(() => {
      httpMock.verify();
    });

    describe('rendu (intégration DOM)', () => {
      it('should display the session name, description, teacher and date', () => {
        const compiled = fixture.nativeElement as HTMLElement;
        expect(compiled.querySelector('h1')?.textContent).toContain('Yoga Session');
        expect(compiled.querySelector('.description')?.textContent).toContain('A relaxing session');
        const subtitle = compiled.querySelector('mat-card-subtitle');
        expect(subtitle?.textContent).toContain('Margot');
        expect(subtitle?.textContent).toContain('DELAHAYE');
      });

      it('should show the Delete button for an admin', () => {
        const compiled = fixture.nativeElement as HTMLElement;
        const deleteButton = Array.from(compiled.querySelectorAll('button'))
          .find(btn => btn.textContent?.includes('Delete'));
        expect(deleteButton).toBeDefined();
      });

      it('should not show the Participate/UnParticipate buttons for an admin', () => {
        const compiled = fixture.nativeElement as HTMLElement;
        const participateButton = Array.from(compiled.querySelectorAll('button'))
          .find(btn => btn.textContent?.includes('Participate'));
        expect(participateButton).toBeUndefined();
      });
    });

    describe('logique isolée (unitaire)', () => {
      it('should create', () => {
        expect(component).toBeTruthy();
      });

      it('should navigate back in browser history when back() is called', () => {
        const historySpy = jest.spyOn(window.history, 'back').mockImplementation(() => {});
        component.back();
        expect(historySpy).toHaveBeenCalled();
        historySpy.mockRestore();
      });

      // Unitaire : ne lit pas le DOM, vérifie seulement le contrat HTTP
      // mocké (verbe + URL) et les appels à la snackbar/router.
      it('should delete the session, notify the user and navigate to /sessions', () => {
        const snackBarSpy = jest.spyOn(MatSnackBar.prototype, 'open')
          .mockReturnValue({} as ReturnType<MatSnackBar['open']>);

        component.delete();

        const deleteReq = httpMock.expectOne({ url: 'api/session/1', method: 'DELETE' });
        deleteReq.flush(null);

        expect(snackBarSpy).toHaveBeenCalledWith('Session deleted !', 'Close', { duration: 3000 });
        expect(router.navigate).toHaveBeenCalledWith(['sessions']);
      });
    });
  });

  describe('as a non-admin user who has not joined the session', () => {
    const nonParticipatingSession: Session = { ...mockSession, users: [2] };

    beforeEach(async () => {
      await configureTestBed({ admin: false, id: 1 });
      service = TestBed.inject(SessionService);
      httpMock = TestBed.inject(HttpTestingController);
      router = TestBed.inject(Router);
      jest.spyOn(router, 'navigate').mockResolvedValue(true);

      fixture = TestBed.createComponent(DetailComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      httpMock.expectOne({ url: 'api/session/1', method: 'GET' }).flush(nonParticipatingSession);
      httpMock.expectOne({ url: 'api/teacher/1', method: 'GET' }).flush(mockTeacher);
      fixture.detectChanges();
    });

    afterEach(() => {
      httpMock.verify();
    });

    describe('rendu (intégration DOM)', () => {
      it('should not show the Delete button for a non-admin', () => {
        const compiled = fixture.nativeElement as HTMLElement;
        const deleteButton = Array.from(compiled.querySelectorAll('button'))
          .find(btn => btn.textContent?.includes('Delete'));
        expect(deleteButton).toBeUndefined();
      });

      it('should show the Participate button when the user has not joined', () => {
        expect(component.isParticipate).toBe(false);
        const compiled = fixture.nativeElement as HTMLElement;
        const participateButton = Array.from(compiled.querySelectorAll('button'))
          .find(btn => btn.textContent?.includes('Participate'));
        expect(participateButton).toBeDefined();
      });

      it('should toggle the DOM to "Do not participate" and update the attendee count when the Participate button is clicked', () => {
        const compiled = fixture.nativeElement as HTMLElement;
        const participateButton = Array.from(compiled.querySelectorAll('button'))
          .find(btn => btn.textContent?.includes('Participate')) as HTMLButtonElement;
        expect(participateButton).toBeDefined();

        participateButton.click();

        const participateReq = httpMock.expectOne({ url: 'api/session/1/participate/1', method: 'POST' });
        participateReq.flush(null);

        const updatedSession: Session = { ...mockSession, users: [1, 2] };
        httpMock.expectOne({ url: 'api/session/1', method: 'GET' }).flush(updatedSession);
        httpMock.expectOne({ url: 'api/teacher/1', method: 'GET' }).flush(mockTeacher);
        fixture.detectChanges();

        const refreshed = fixture.nativeElement as HTMLElement;
        const buttons = Array.from(refreshed.querySelectorAll('button'));
        expect(buttons.find(btn => btn.textContent?.includes('Do not participate'))).toBeDefined();
        expect(buttons.find(btn => btn.textContent?.includes('Participate'))).toBeUndefined();
        expect(refreshed.textContent).toContain('2 attendees');
      });
    });

    describe('logique isolée (unitaire)', () => {
      // Unitaire : ne lit pas le DOM, vérifie seulement le contrat HTTP
      // mocké (verbe + URL) et l'état interne du composant.
      it('should call the participate API with the session and user id, then reload the session', () => {
        component.participate();

        const participateReq = httpMock.expectOne({ url: 'api/session/1/participate/1', method: 'POST' });
        participateReq.flush(null);

        const updatedSession: Session = { ...mockSession, users: [1, 2] };
        httpMock.expectOne({ url: 'api/session/1', method: 'GET' }).flush(updatedSession);
        httpMock.expectOne({ url: 'api/teacher/1', method: 'GET' }).flush(mockTeacher);

        fixture.detectChanges();
        expect(component.isParticipate).toBe(true);
      });
    });
  });

  describe('as a non-admin user who has already joined the session', () => {
    const participatingSession: Session = { ...mockSession, users: [1] };

    beforeEach(async () => {
      await configureTestBed({ admin: false, id: 1 });
      service = TestBed.inject(SessionService);
      httpMock = TestBed.inject(HttpTestingController);
      router = TestBed.inject(Router);
      jest.spyOn(router, 'navigate').mockResolvedValue(true);

      fixture = TestBed.createComponent(DetailComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();

      httpMock.expectOne({ url: 'api/session/1', method: 'GET' }).flush(participatingSession);
      httpMock.expectOne({ url: 'api/teacher/1', method: 'GET' }).flush(mockTeacher);
      fixture.detectChanges();
    });

    afterEach(() => {
      httpMock.verify();
    });

    describe('rendu (intégration DOM)', () => {
      it('should show the UnParticipate button when the user has already joined', () => {
        expect(component.isParticipate).toBe(true);
        const compiled = fixture.nativeElement as HTMLElement;
        const unParticipateButton = Array.from(compiled.querySelectorAll('button'))
          .find(btn => btn.textContent?.includes('Do not participate'));
        expect(unParticipateButton).toBeDefined();
      });

      it('should toggle the DOM to "Participate" and update the attendee count when the Do not participate button is clicked', () => {
        const compiled = fixture.nativeElement as HTMLElement;
        const unParticipateButton = Array.from(compiled.querySelectorAll('button'))
          .find(btn => btn.textContent?.includes('Do not participate')) as HTMLButtonElement;
        expect(unParticipateButton).toBeDefined();

        unParticipateButton.click();

        const unParticipateReq = httpMock.expectOne({ url: 'api/session/1/participate/1', method: 'DELETE' });
        unParticipateReq.flush(null);

        const updatedSession: Session = { ...mockSession, users: [] };
        httpMock.expectOne({ url: 'api/session/1', method: 'GET' }).flush(updatedSession);
        httpMock.expectOne({ url: 'api/teacher/1', method: 'GET' }).flush(mockTeacher);
        fixture.detectChanges();

        const refreshed = fixture.nativeElement as HTMLElement;
        const buttons = Array.from(refreshed.querySelectorAll('button'));
        expect(buttons.find(btn => btn.textContent?.includes('Participate'))).toBeDefined();
        expect(buttons.find(btn => btn.textContent?.includes('Do not participate'))).toBeUndefined();
        expect(refreshed.textContent).toContain('0 attendees');
      });
    });

    describe('logique isolée (unitaire)', () => {
      // Unitaire : ne lit pas le DOM, vérifie seulement le contrat HTTP
      // mocké (verbe + URL) et l'état interne du composant.
      it('should call the unParticipate API with the session and user id, then reload the session', () => {
        component.unParticipate();

        const unParticipateReq = httpMock.expectOne({ url: 'api/session/1/participate/1', method: 'DELETE' });
        unParticipateReq.flush(null);

        const updatedSession: Session = { ...mockSession, users: [] };
        httpMock.expectOne({ url: 'api/session/1', method: 'GET' }).flush(updatedSession);
        httpMock.expectOne({ url: 'api/teacher/1', method: 'GET' }).flush(mockTeacher);

        fixture.detectChanges();
        expect(component.isParticipate).toBe(false);
      });
    });
  });
});
