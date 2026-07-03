import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { expect } from '@jest/globals';
import { Session } from '../../../../core/models/session.interface';
import { SessionApiService } from '../../../../core/service/session-api.service';
import { SessionService } from 'src/app/core/service/session.service';

import { FormComponent } from './form.component';

describe('FormComponent', () => {
  const mockSession: Session = {
    id: 1,
    name: 'Yoga Morning',
    date: new Date('2025-06-15T00:00:00.000Z'),
    teacher_id: 2,
    description: 'Morning yoga session',
    users: []
  };

  const sharedImports = [
    RouterTestingModule,
    HttpClientTestingModule,
    MatCardModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    MatSnackBarModule,
    MatSelectModule,
    BrowserAnimationsModule,
    FormComponent
  ];

  // ─── Create mode ────────────────────────────────────────────────────────────

  describe('Create mode (admin)', () => {
    let component: FormComponent;
    let fixture: ComponentFixture<FormComponent>;
    let httpMock: HttpTestingController;

    const mockRouter = { url: '/sessions/create', navigate: jest.fn() };
    const mockActivatedRoute = { snapshot: { paramMap: { get: (_: string) => null } } };

    beforeEach(async () => {
      jest.clearAllMocks();

      await TestBed.configureTestingModule({
        imports: sharedImports,
        providers: [
          { provide: SessionService, useValue: { sessionInformation: { admin: true } } },
          { provide: Router, useValue: mockRouter },
          { provide: ActivatedRoute, useValue: mockActivatedRoute },
          SessionApiService
        ]
      }).compileComponents();

      httpMock = TestBed.inject(HttpTestingController);
      fixture = TestBed.createComponent(FormComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      httpMock.expectOne('api/teacher').flush([]);
    });

    afterEach(() => httpMock.verify());

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should be in create mode (onUpdate = false)', () => {
      expect(component.onUpdate).toBe(false);
    });

    it('should initialize an empty form in create mode', () => {
      expect(component.sessionForm?.get('name')?.value).toBe('');
      expect(component.sessionForm?.get('date')?.value).toBe('');
      expect(component.sessionForm?.get('teacher_id')?.value).toBe('');
      expect(component.sessionForm?.get('description')?.value).toBe('');
    });

    it('should disable the submit button when the form is invalid', () => {
      fixture.detectChanges();
      const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
      expect(button.disabled).toBe(true);
    });

    it('should enable the submit button when the form is valid', () => {
      component.sessionForm?.setValue({
        name: 'New Session',
        date: '2025-12-01',
        teacher_id: 1,
        description: 'A test session'
      });
      fixture.detectChanges();
      const button: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
      expect(button.disabled).toBe(false);
    });

    it('should call the create API and navigate to sessions on submit', () => {
      component.sessionForm?.setValue({
        name: 'New Session',
        date: '2025-12-01',
        teacher_id: 1,
        description: 'A test session'
      });

      component.submit();

      const req = httpMock.expectOne('api/session');
      expect(req.request.method).toBe('POST');
      req.flush(mockSession);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['sessions']);
    });
  });

  // ─── Non-admin redirect ──────────────────────────────────────────────────────

  describe('Non-admin user', () => {
    let fixture: ComponentFixture<FormComponent>;
    let httpMock: HttpTestingController;

    const mockRouter = { url: '/sessions/create', navigate: jest.fn() };
    const mockActivatedRoute = { snapshot: { paramMap: { get: (_: string) => null } } };

    beforeEach(async () => {
      jest.clearAllMocks();

      await TestBed.configureTestingModule({
        imports: sharedImports,
        providers: [
          { provide: SessionService, useValue: { sessionInformation: { admin: false } } },
          { provide: Router, useValue: mockRouter },
          { provide: ActivatedRoute, useValue: mockActivatedRoute },
          SessionApiService
        ]
      }).compileComponents();

      httpMock = TestBed.inject(HttpTestingController);
      fixture = TestBed.createComponent(FormComponent);
      fixture.detectChanges();
      httpMock.expectOne('api/teacher').flush([]);
    });

    afterEach(() => httpMock.verify());

    it('should redirect a non-admin user to /sessions on init', () => {
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/sessions']);
    });
  });

  // ─── Edit mode ───────────────────────────────────────────────────────────────

  describe('Edit mode (admin)', () => {
    let component: FormComponent;
    let fixture: ComponentFixture<FormComponent>;
    let httpMock: HttpTestingController;

    const mockRouter = { url: '/sessions/update/1', navigate: jest.fn() };
    const mockActivatedRoute = { snapshot: { paramMap: { get: (_: string) => '1' } } };

    beforeEach(async () => {
      jest.clearAllMocks();

      await TestBed.configureTestingModule({
        imports: sharedImports,
        providers: [
          { provide: SessionService, useValue: { sessionInformation: { admin: true } } },
          { provide: Router, useValue: mockRouter },
          { provide: ActivatedRoute, useValue: mockActivatedRoute },
          SessionApiService
        ]
      }).compileComponents();

      httpMock = TestBed.inject(HttpTestingController);
      fixture = TestBed.createComponent(FormComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
      // In edit mode, sessionForm is undefined on first render (set via subscribe).
      // Flush session detail first so initForm() runs and sessionForm is defined.
      httpMock.expectOne('api/session/1').flush(mockSession);
      // Second detectChanges renders @if(sessionForm) block → teachers$ is subscribed.
      fixture.detectChanges();
      httpMock.expectOne('api/teacher').flush([]);
      fixture.detectChanges();
    });

    afterEach(() => httpMock.verify());

    it('should be in edit mode (onUpdate = true)', () => {
      expect(component.onUpdate).toBe(true);
    });

    it('should pre-fill the form with the existing session data', () => {
      expect(component.sessionForm?.get('name')?.value).toBe('Yoga Morning');
      expect(component.sessionForm?.get('teacher_id')?.value).toBe(2);
      expect(component.sessionForm?.get('description')?.value).toBe('Morning yoga session');
    });

    it('should call the update API and navigate to sessions on submit', () => {
      component.submit();

      const req = httpMock.expectOne('api/session/1');
      expect(req.request.method).toBe('PUT');
      req.flush(mockSession);

      expect(mockRouter.navigate).toHaveBeenCalledWith(['sessions']);
    });
  });
});
