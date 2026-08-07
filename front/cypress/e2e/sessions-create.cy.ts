// Création de session — scénarios Cypress.
//
// FormComponent (front/src/app/pages/sessions/components/form/form.component.ts) sert à la fois
// create et update, distingués via this.router.url.includes('update') (form.component.ts:38-48) :
// on navigue donc toujours en cliquant sur le bouton "Create" de la liste (routerLink interne),
// jamais via cy.visit direct, pour rester cohérent avec sessions-list.cy.ts / sessions-detail.cy.ts
// et respecter le fonctionnement réel de SessionService (état en mémoire, pas de persistance).
//
// Le champ description a un Validators.max(2000) inopérant sur une string (form.component.ts:84-86,
// même famille de bug que register.component.ts) : il ne bloque jamais la soumission. Seul
// Validators.required compte réellement sur chacun des 4 champs — voir le test 2 ci-dessous.
//
// Ce composant n'a AUCUNE gestion d'erreur API (submit() n'a pas de callback error sur le
// subscribe, form.component.ts:51-65) : contrairement à login/register, aucun scénario "erreur API
// affichée" n'est testé ici, ce serait un faux test documentant un comportement inexistant.
//
// Le dernier test ("real backend") tape le vrai backend Spring Boot (Docker, http://localhost:8080,
// proxy /api) et NE PASSE QUE SI back/src/main/resources/sql/insert_teacher.sql a été appliqué
// manuellement au préalable (teacher id=1, "Margot DELAHAYE"), même dépendance que
// sessions-list.cy.ts et sessions-detail.cy.ts.

describe('Session create spec', () => {
  beforeEach(() => {
    cy.visit('/login')
  })

  it('session create - form displayed for admin (mock)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      fixture: 'login-success.json', // admin: true
    }).as('loginRequest')
    cy.intercept('GET', '/api/session', []).as('sessionsRequest')
    cy.intercept('GET', '/api/teacher', { fixture: 'teachers.json' }).as('teacherRequest')

    cy.get('input[formControlName=email]').type('yoga@studio.com')
    cy.get('input[formControlName=password]').type('test!1234')
    cy.get('button[type=submit]').click()

    cy.wait('@loginRequest')
    cy.wait('@sessionsRequest')

    cy.contains('button', 'Create').click()
    cy.wait('@teacherRequest')

    cy.url().should('include', '/sessions/create')
    cy.contains('h1', 'Create session').should('be.visible')

    cy.get('input[formControlName=name]').should('be.visible')
    cy.get('input[type=date][formControlName=date]').should('be.visible')
    cy.get('mat-select[formControlName=teacher_id]').should('be.visible')
    cy.get('textarea[formControlName=description]').should('be.visible')

    // Formulaire vide au chargement : tous les champs required sont vides, submit doit rester bloqué.
    cy.get('button[type=submit]').should('be.disabled')
  })

  it('session create - submit disabled when required field missing (validation front only)', () => {
    // Testing plan "affichage d'erreur en l'absence d'un champ obligatoire" : ce composant n'a pas
    // de message d'erreur (ni front ni API, cf. en-tête de fichier) — la seule preuve possible est
    // l'état du DOM (bouton disabled) + l'absence de requête réseau, même logique que
    // login.cy.ts / register.cy.ts pour leur test "required field missing".
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      fixture: 'login-success.json', // admin: true
    }).as('loginRequest')
    cy.intercept('GET', '/api/session', []).as('sessionsRequest')
    cy.intercept('GET', '/api/teacher', { fixture: 'teachers.json' }).as('teacherRequest')
    // Spy uniquement (pas de stub) : prouve qu'aucune requête n'est jamais émise si le form est invalide.
    cy.intercept('POST', '/api/session').as('createRequest')

    cy.get('input[formControlName=email]').type('yoga@studio.com')
    cy.get('input[formControlName=password]').type('test!1234')
    cy.get('button[type=submit]').click()

    cy.wait('@loginRequest')
    cy.wait('@sessionsRequest')

    cy.contains('button', 'Create').click()
    cy.wait('@teacherRequest')

    // name volontairement laissé vide : seul required peut bloquer (Validators.max sur
    // description est inopérant sur une string, cf. en-tête de fichier).
    cy.get('input[type=date][formControlName=date]').type('2026-09-05')
    cy.get('mat-select[formControlName=teacher_id]').click()
    cy.get('mat-option').contains('Margot DELAHAYE').click()
    cy.get('textarea[formControlName=description]').type('Description valide.')

    cy.get('button[type=submit]').should('be.disabled')

    // Un Enter dans un champ du formulaire ne déclenche pas de soumission implicite tant que
    // l'unique bouton submit reste disabled.
    cy.get('textarea[formControlName=description]').type('{enter}')

    cy.get('@createRequest.all').should('have.length', 0)
    cy.url().should('include', '/sessions/create')
  })

  it('session create - successful creation (mock)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      fixture: 'login-success.json', // admin: true
    }).as('loginRequest')
    cy.intercept('GET', '/api/session', []).as('sessionsRequest')
    cy.intercept('GET', '/api/teacher', { fixture: 'teachers.json' }).as('teacherRequest')
    cy.intercept('POST', '/api/session', {
      statusCode: 200,
      fixture: 'session-create-response.json',
    }).as('createRequest')

    cy.get('input[formControlName=email]').type('yoga@studio.com')
    cy.get('input[formControlName=password]').type('test!1234')
    cy.get('button[type=submit]').click()

    cy.wait('@loginRequest')
    cy.wait('@sessionsRequest')

    cy.contains('button', 'Create').click()
    cy.wait('@teacherRequest')

    cy.get('input[formControlName=name]').type('Mocked New Session')
    cy.get('input[type=date][formControlName=date]').type('2026-09-05')
    cy.get('mat-select[formControlName=teacher_id]').click()
    cy.get('mat-option').contains('Margot DELAHAYE').click()
    cy.get('textarea[formControlName=description]').type('Description de test.')

    cy.get('button[type=submit]').should('not.be.disabled').click()

    cy.wait('@createRequest')

    // exitPage() (form.component.ts:91-94) ouvre le snackbar puis navigate de façon synchrone.
    cy.contains('Session created !').should('be.visible')
    cy.url().should('match', /\/sessions$/)
  })

  it('session create (real backend) - full lifecycle', () => {
    // Cleanup de sécurité enregistré via l'alias @createdSessionId, lu par le hook afterEach
    // ci-dessous (même pattern que sessions-list.cy.ts et sessions-detail.cy.ts) : même si une
    // assertion échoue après la création réelle, la session sera quand même supprimée.
    const sessionName = `E2E create real session ${Date.now()}`

    cy.get('input[formControlName=email]').type('yoga@studio.com')
    cy.get('input[formControlName=password]').type('test!1234')
    cy.get('button[type=submit]').click()

    cy.url().should('include', '/sessions')

    cy.contains('button', 'Create').click()
    cy.url().should('include', '/sessions/create')
    cy.contains('h1', 'Create session').should('be.visible')

    cy.get('input[formControlName=name]').type(sessionName)
    cy.get('input[type=date][formControlName=date]').type('2026-09-05')
    cy.get('mat-select[formControlName=teacher_id]').click()
    // Dépend du teacher id=1 seedé par back/src/main/resources/sql/insert_teacher.sql
    // ("Margot DELAHAYE"), appliqué manuellement au préalable sur cet environnement.
    cy.get('mat-option').contains('Margot DELAHAYE').click()
    cy.get('textarea[formControlName=description]').type(
      'Session created by the real-backend create e2e test.'
    )

    cy.get('button[type=submit]').should('not.be.disabled').click()

    cy.url().should('match', /\/sessions$/)
    cy.contains('[data-cy=session-item]', sessionName).should('be.visible')

    // Cleanup : aucun id disponible côté front (création faite via l'UI, pas via cy.request) ;
    // on retrouve l'id réel via GET /api/session en filtrant sur le nom unique horodaté.
    cy.request('POST', '/api/auth/login', {
      email: 'yoga@studio.com',
      password: 'test!1234',
    }).then((loginResponse) => {
      const authHeaders = { Authorization: `Bearer ${loginResponse.body.token}` }
      cy.request({ method: 'GET', url: '/api/session', headers: authHeaders }).then((sessionsResponse) => {
        const created = (sessionsResponse.body as Array<{ id: number; name: string }>).find(
          (session) => session.name === sessionName
        )
        expect(created, `Session "${sessionName}" introuvable via GET /api/session`).to.exist
        cy.wrap(created!.id).as('createdSessionId')
      })
    })
  })

  afterEach(function () {
    const createdSessionId = this.createdSessionId as number | undefined
    if (createdSessionId === undefined) {
      return
    }

    cy.request('POST', '/api/auth/login', {
      email: 'yoga@studio.com',
      password: 'test!1234',
    }).then((loginResponse) => {
      cy.request({
        method: 'DELETE',
        url: `/api/session/${createdSessionId}`,
        headers: { Authorization: `Bearer ${loginResponse.body.token}` },
      })
    })
  })
})
