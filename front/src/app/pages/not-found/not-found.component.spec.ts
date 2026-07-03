import { ComponentFixture, TestBed } from '@angular/core/testing';
import { expect } from '@jest/globals';

import { NotFoundComponent } from './not-found.component';

/**
 * NotFoundComponent — page 404 affichée sur route inconnue
 *
 * Cas du testing plan couverts :
 *   - Navigation : route inconnue → le composant 404 se crée sans erreur
 *
 * Répartition des tests (méthodologie stricte du projet) :
 *   - INTÉGRATION = le test lit lui-même le DOM réellement rendu et/ou
 *     vérifie une requête HTTP réelle via HttpTestingController.
 *   - UNITAIRE = tout le reste, même si TestBed/fixture servent de
 *     plomberie (instanciation) sans assertion sur ce qu'ils produisent.
 */
describe('NotFoundComponent', () => {
  let component: NotFoundComponent;
  let fixture: ComponentFixture<NotFoundComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotFoundComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NotFoundComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('logique isolée (unitaire)', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });
  });
});
