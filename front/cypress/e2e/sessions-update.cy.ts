// Modification d'une session — scénarios Cypress.
//
// FormComponent (front/src/app/pages/sessions/components/form/form.component.ts) sert à la fois
// create et update, distingués via this.router.url.includes('update') (form.component.ts:38-48).
// En mode update, initForm() (form.component.ts:67-89) préremplit name/date/teacher_id/description
// depuis la session chargée par GET /api/session/{id} (form.component.ts:41-45) : date reformatée
// en YYYY-MM-DD, teacher_id présélectionné dans le mat-select via le binding standard reactive
// forms (pas de logique custom). On navigue donc toujours en cliquant sur le bouton "Edit" de la
// carte dans la liste (routerLink interne, réservé à l'admin, list.component.html:34-40), jamais
// via cy.visit direct — même contrainte que sessions-detail.cy.ts (SessionService garde l'état de
// connexion en mémoire uniquement, un rechargement de page le perdrait).
//
// Ce composant n'a AUCUNE gestion d'erreur API (submit() n'a pas de callback error sur le
// subscribe, form.component.ts:51-65), même famille que create : aucun scénario "erreur API
// affichée" n'est testé ici. Le cas "champ obligatoire manquant" est couvert par le test
// "session update - submit disabled when required field missing" ci-dessous, avec la même
// stratégie que sessions-create.cy.ts::"submit disabled when required field missing" (spy sur la
// requête PUT + bouton submit disabled, pas de message d'erreur possible). La validation
// (Validators.required) est strictement identique à celle de create (même FormGroup, mêmes
// validators, form.component.ts:67-89) : ce test dédié ne vérifie donc pas une logique différente,
// mais reste nécessaire pour aligner cette spec avec le testing plan officiel, qui exige une
// couverture explicite par écran plutôt qu'un renvoi vers le test équivalent de create.
//
// Le dernier test ("real backend") tape le vrai backend Spring Boot (Docker, http://localhost:8080,
// proxy /api) et NE PASSE QUE SI back/src/main/resources/sql/insert_teacher.sql a été appliqué
// manuellement au préalable (teacher id=1, "Margot DELAHAYE"), même dépendance que
// sessions-create.cy.ts / sessions-list.cy.ts / sessions-detail.cy.ts / sessions-delete.cy.ts.

describe('Session update spec', () => {
  beforeEach(() => {
    cy.visit('/login')
  })

  it('session update - form pre-filled with existing session data (mock)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      fixture: 'login-success.json', // admin: true
    }).as('loginRequest')
    cy.intercept('GET', '/api/teacher', { fixture: 'teachers.json' }).as('teacherRequest')

    cy.fixture('session-detail.json').then((session) => {
      cy.intercept('GET', '/api/session', [session]).as('sessionsRequest')
      cy.intercept('GET', `/api/session/${session.id}`, session).as('sessionDetailRequest')

      cy.get('input[formControlName=email]').type('yoga@studio.com')
      cy.get('input[formControlName=password]').type('test!1234')
      cy.get('button[type=submit]').click()

      cy.wait('@loginRequest')
      cy.wait('@sessionsRequest')

      cy.contains('[data-cy=session-item]', session.name).within(() => {
        cy.contains('button', 'Edit').click()
      })

      cy.wait('@teacherRequest')
      cy.wait('@sessionDetailRequest')

      cy.url().should('include', `/sessions/update/${session.id}`)
      cy.contains('h1', 'Update session').should('be.visible')

      // initForm() (form.component.ts:67-89) mappe chaque champ depuis la session chargée.
      cy.get('input[formControlName=name]').should('have.value', session.name)
      cy.get('input[type=date][formControlName=date]').should(
        'have.value',
        new Date(session.date).toISOString().split('T')[0]
      )
      cy.get('textarea[formControlName=description]').should('have.value', session.description)

      // mat-select se présélectionne via le binding standard reactive forms (form.component.ts:77-80,
      // pas de logique custom) : la valeur affichée doit correspondre au teacher_id=1 de la fixture.
      cy.get('mat-select[formControlName=teacher_id]').should('contain.text', 'Margot DELAHAYE')

      cy.get('button[type=submit]').should('not.be.disabled')
    })
  })

  it('session update - submit disabled when required field missing (validation front only)', () => {
    // Testing plan "affichage d'erreur en l'absence d'un champ obligatoire" : ce composant n'a pas
    // de message d'erreur (ni front ni API, cf. en-tête de fichier) — la seule preuve possible est
    // l'état du DOM (bouton disabled) + l'absence de requête réseau, même logique que
    // sessions-create.cy.ts::"submit disabled when required field missing".
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      fixture: 'login-success.json', // admin: true
    }).as('loginRequest')
    cy.intercept('GET', '/api/teacher', { fixture: 'teachers.json' }).as('teacherRequest')

    cy.fixture('session-detail.json').then((session) => {
      cy.intercept('GET', '/api/session', [session]).as('sessionsRequest')
      cy.intercept('GET', `/api/session/${session.id}`, session).as('sessionDetailRequest')
      // Spy uniquement (pas de stub) : prouve qu'aucune requête n'est jamais émise si le form est invalide.
      cy.intercept('PUT', `/api/session/${session.id}`).as('updateRequest')

      cy.get('input[formControlName=email]').type('yoga@studio.com')
      cy.get('input[formControlName=password]').type('test!1234')
      cy.get('button[type=submit]').click()

      cy.wait('@loginRequest')
      cy.wait('@sessionsRequest')

      cy.contains('[data-cy=session-item]', session.name).within(() => {
        cy.contains('button', 'Edit').click()
      })

      cy.wait('@teacherRequest')
      cy.wait('@sessionDetailRequest')

      // name est prérempli par initForm() (form.component.ts:67-89) : on le vide pour déclencher
      // Validators.required, contrairement à create où le champ part déjà vide.
      cy.get('input[formControlName=name]').clear()

      cy.get('button[type=submit]').should('be.disabled')

      // Un Enter dans un champ du formulaire ne déclenche pas de soumission implicite tant que
      // l'unique bouton submit reste disabled.
      cy.get('textarea[formControlName=description]').type('{enter}')

      cy.get('@updateRequest.all').should('have.length', 0)
      cy.url().should('include', `/sessions/update/${session.id}`)
    })
  })

  it('session update - successful update redirects with updated snackbar (mock)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      fixture: 'login-success.json', // admin: true
    }).as('loginRequest')
    cy.intercept('GET', '/api/teacher', { fixture: 'teachers.json' }).as('teacherRequest')

    cy.fixture('session-detail.json').then((session) => {
      cy.intercept('GET', '/api/session', [session]).as('sessionsRequest')
      cy.intercept('GET', `/api/session/${session.id}`, session).as('sessionDetailRequest')

      cy.get('input[formControlName=email]').type('yoga@studio.com')
      cy.get('input[formControlName=password]').type('test!1234')
      cy.get('button[type=submit]').click()

      cy.wait('@loginRequest')
      cy.wait('@sessionsRequest')

      cy.contains('[data-cy=session-item]', session.name).within(() => {
        cy.contains('button', 'Edit').click()
      })

      cy.wait('@teacherRequest')
      cy.wait('@sessionDetailRequest')

      cy.intercept('PUT', `/api/session/${session.id}`, {
        statusCode: 200,
        fixture: 'session-update-response.json',
      }).as('updateRequest')

      cy.get('input[formControlName=name]').clear().type('Sunrise Yoga Flow - Updated')

      cy.get('button[type=submit]').should('not.be.disabled').click()

      cy.wait('@updateRequest')

      // exitPage() (form.component.ts:91-94) : snackbar différent du create ("Session updated !"
      // vs "Session created !", form.component.ts:58 et :63), même mécanisme synchrone
      // snackbar + navigate.
      cy.contains('Session updated !').should('be.visible')
      cy.url().should('match', /\/sessions$/)
    })
  })

  it('session update (real backend) - full lifecycle', () => {
    // Cleanup de sécurité enregistré via l'alias @createdSessionId, lu par le hook afterEach
    // ci-dessous (même pattern que sessions-create.cy.ts) : même si une assertion échoue après la
    // création réelle, la session sera quand même supprimée. Le test lui-même ne supprime jamais
    // la session (contrairement à sessions-delete.cy.ts), l'id créé via cy.request existe donc
    // toujours au moment de l'afterEach.
    const initialName = `E2E update real session ${Date.now()}`
    const updatedName = `E2E update real session updated ${Date.now()}`

    cy.request('POST', '/api/auth/login', {
      email: 'yoga@studio.com',
      password: 'test!1234',
    }).then((loginResponse) => {
      const authHeaders = { Authorization: `Bearer ${loginResponse.body.token}` }

      cy.request({
        method: 'POST',
        url: '/api/session',
        headers: authHeaders,
        body: {
          name: initialName,
          date: '2026-09-05T00:00:00.000Z',
          // Dépend du teacher id=1 seedé par back/src/main/resources/sql/insert_teacher.sql
          // ("Margot DELAHAYE"), même dépendance que sessions-create.cy.ts.
          teacher_id: 1,
          description: 'Session created by the real-backend update e2e test (before update).',
          users: [],
        },
      }).then((createResponse) => {
        cy.wrap(createResponse.body.id).as('createdSessionId')
        const sessionId = createResponse.body.id as number

        cy.get('input[formControlName=email]').type('yoga@studio.com')
        cy.get('input[formControlName=password]').type('test!1234')
        cy.get('button[type=submit]').click()

        cy.url().should('include', '/sessions')
        cy.contains('[data-cy=session-item]', initialName).within(() => {
          cy.contains('button', 'Edit').click()
        })

        cy.url().should('include', `/sessions/update/${sessionId}`)
        cy.contains('h1', 'Update session').should('be.visible')

        // Préremplissage réel, mêmes assertions que le test mocké ci-dessus.
        cy.get('input[formControlName=name]').should('have.value', initialName)
        cy.get('input[type=date][formControlName=date]').should('have.value', '2026-09-05')
        cy.get('textarea[formControlName=description]').should(
          'have.value',
          'Session created by the real-backend update e2e test (before update).'
        )
        cy.get('mat-select[formControlName=teacher_id]').should('contain.text', 'Margot DELAHAYE')

        cy.get('input[formControlName=name]').clear().type(updatedName)

        cy.get('button[type=submit]').should('not.be.disabled').click()

        cy.url().should('match', /\/sessions$/)
        cy.contains('[data-cy=session-item]', updatedName).should('be.visible')
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
