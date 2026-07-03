import { HttpClientModule } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { provideRouter, Router } from '@angular/router';
import { expect } from '@jest/globals';
import { of } from 'rxjs';
import { User } from 'src/app/core/models/user.interface';
import { SessionService } from 'src/app/core/service/session.service';
import { UserService } from 'src/app/core/service/user.service';

import { MeComponent } from './me.component';

describe('MeComponent', () => {
  let component: MeComponent;
  let fixture: ComponentFixture<MeComponent>;
  let userService: UserService;
  let router: Router;

  const mockSessionService = {
    sessionInformation: {
      admin: true,
      id: 1
    },
    logOut: jest.fn()
  }

  const mockMatSnackBar = {
    open: jest.fn()
  }

  const mockUser: User = {
    id: 1,
    email: 'user@test.com',
    lastName: 'Doe',
    firstName: 'John',
    admin: false,
    password: 'password',
    createdAt: new Date('2023-01-01'),
    updatedAt: new Date('2023-01-02')
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        MatSnackBarModule,
        HttpClientModule,
        MatCardModule,
        MatFormFieldModule,
        MatIconModule,
        MatInputModule,MeComponent

      ],
      providers: [
        provideRouter([]),
        { provide: SessionService, useValue: mockSessionService }
      ],
    })
      .overrideProvider(MatSnackBar, { useValue: mockMatSnackBar })
      .compileComponents();

    fixture = TestBed.createComponent(MeComponent);
    component = fixture.componentInstance;
    userService = TestBed.inject(UserService);
    router = TestBed.inject(Router);
    jest.spyOn(userService, 'getById').mockReturnValue(of(mockUser));
  });

  afterEach(() => {
    mockSessionService.logOut.mockClear();
    mockMatSnackBar.open.mockClear();
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should fetch and display the user information in the DOM', () => {
    fixture.detectChanges();

    expect(userService.getById).toHaveBeenCalledWith('1');
    expect(component.user).toEqual(mockUser);

    const text: string = fixture.nativeElement.textContent;
    expect(text).toContain('John');
    expect(text).toContain('DOE');
    expect(text).toContain(mockUser.email);
  });

  it('should call window.history.back on back()', () => {
    fixture.detectChanges();
    const backSpy = jest.spyOn(window.history, 'back').mockImplementation(() => {});

    component.back();

    expect(backSpy).toHaveBeenCalled();
    backSpy.mockRestore();
  });

  it('should delete the account, notify the user and navigate on delete', () => {
    fixture.detectChanges();
    jest.spyOn(userService, 'delete').mockReturnValue(of(undefined));
    const navigateSpy = jest.spyOn(router, 'navigate');

    component.delete();

    expect(userService.delete).toHaveBeenCalledWith('1');
    expect(mockMatSnackBar.open).toHaveBeenCalledWith('Your account has been deleted !', 'Close', { duration: 3000 });
    expect(mockSessionService.logOut).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/']);
  });
});
