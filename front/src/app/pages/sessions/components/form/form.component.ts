import { Component, OnInit, inject, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { SessionService } from '../../../../core/service/session.service';
import { TeacherService } from '../../../../core/service/teacher.service';
import { Session } from '../../../../core/models/session.interface';
import { SessionApiService } from '../../../../core/service/session-api.service';
import { MaterialModule } from "../../../../shared/material.module";
import { CommonModule } from "@angular/common";

@Component({
  selector: 'app-form',
  imports: [CommonModule, MaterialModule],
  templateUrl: './form.component.html',
  styleUrls: ['./form.component.scss']
})
export class FormComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);
  private matSnackBar = inject(MatSnackBar);
  private sessionApiService = inject(SessionApiService);
  private sessionService = inject(SessionService);
  private teacherService = inject(TeacherService);
  private router = inject(Router);

  public onUpdate: boolean = false;
  public sessionForm: FormGroup | undefined;
  public teachers$ = this.teacherService.all();
  private id: string | undefined;

  ngOnInit(): void {
    if (!this.sessionService.sessionInformation!.admin) {
      this.router.navigate(['/sessions']);
    }
    const url = this.router.url;
    if (url.includes('update')) {
      this.onUpdate = true;
      this.id = this.route.snapshot.paramMap.get('id')!;
      this.sessionApiService
        .detail(this.id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((session: Session) => this.initForm(session));
    } else {
      this.initForm();
    }
  }

  public submit(): void {
    const session = this.sessionForm?.value as Session;

    if (!this.onUpdate) {
      this.sessionApiService
        .create(session)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((_: Session) => this.exitPage('Session created !'));
    } else {
      this.sessionApiService
        .update(this.id!, session)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((_: Session) => this.exitPage('Session updated !'));
    }
  }

  private initForm(session?: Session): void {
    this.sessionForm = this.fb.group({
      name: [
        session ? session.name : '',
        [Validators.required]
      ],
      date: [
        session ? new Date(session.date).toISOString().split('T')[0] : '',
        [Validators.required]
      ],
      teacher_id: [
        session ? session.teacher_id : '',
        [Validators.required]
      ],
      description: [
        session ? session.description : '',
        [
          Validators.required,
          Validators.max(2000)
        ]
      ],
    });
  }

  private exitPage(message: string): void {
    this.matSnackBar.open(message, 'Close', { duration: 3000 });
    this.router.navigate(['sessions']);
  }
}
