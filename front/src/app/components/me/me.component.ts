import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { User } from '../../core/models/user.interface';
import { SessionService } from '../../core/service/session.service';
import { UserService } from '../../core/service/user.service';
import { MaterialModule } from "../../shared/material.module";
import { CommonModule } from "@angular/common";

@Component({
  selector: 'app-me',
  imports: [CommonModule, MaterialModule],
  templateUrl: './me.component.html',
  styleUrls: ['./me.component.scss']
})
export class MeComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  private router = inject(Router);
  private sessionService = inject(SessionService);
  private matSnackBar = inject(MatSnackBar);
  private userService = inject(UserService);
  public user: User | undefined;


  ngOnInit(): void {
    this.userService
      .getById(this.sessionService.sessionInformation!.id.toString())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((user: User) => this.user = user);
  }

  public back(): void {
    window.history.back();
  }

  public delete(): void {
    this.userService
      .delete(this.sessionService.sessionInformation!.id.toString())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((_) => {
        this.matSnackBar.open("Your account has been deleted !", 'Close', { duration: 3000 });
        this.sessionService.logOut();
        this.router.navigate(['/']);
      })
  }

}
