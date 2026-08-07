// Sessions (liste) — scenarios Cypress.
//
// Le dernier test ("sessions list (real backend)") tape le vrai backend Spring Boot
// (Docker, http://localhost:8080, proxy /api) et NE PASSE QUE SI le script SQL
// back/src/main/resources/sql/insert_teacher.sql a ete applique manuellement au prealable
// sur la base (meme pattern que back/src/main/resources/sql/insert_user.sql pour l'admin).
// Sans teacher en base, POST /api/session echoue en 404 (TeacherService.findById leve
// NotFoundException) et le test echoue avec un message explicite.
//
// Comportement volontaire a garder en tete (voir list.component.html:5, :30-33, :34,
// deja documente cote unitaire) : Create et Edit sont reserves a l'admin, mais Detail
// est TOUJOURS visible, admin ou non — ce n'est pas un bug.

describe('Sessions list spec', () => {
  beforeEach(() => {
    cy.visit('/login')
  })

  it('sessions list - empty state (mock)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      fixture: 'login-success.json',
    }).as('loginRequest')
    cy.intercept('GET', '/api/session', []).as('sessionsRequest')

    cy.get('input[formControlName=email]').type('yoga@studio.com')
    cy.get('input[formControlName=password]').type('test!1234')
    cy.get('button[type=submit]').click()

    cy.wait('@loginRequest')
    cy.wait('@sessionsRequest')
    cy.url().should('include', '/sessions')

    // Pas de crash : la page reste utilisable et aucune carte fantome n'est affichee.
    cy.contains('mat-card-title', 'Rentals available').should('be.visible')
    cy.get('[data-cy=session-item]').should('have.length', 0)
  })

  it('sessions list - displays sessions (mock)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      fixture: 'login-success.json',
    }).as('loginRequest')
    cy.intercept('GET', '/api/session', { fixture: 'sessions-list.json' }).as('sessionsRequest')

    cy.get('input[formControlName=email]').type('yoga@studio.com')
    cy.get('input[formControlName=password]').type('test!1234')
    cy.get('button[type=submit]').click()

    cy.wait('@loginRequest')
    cy.wait('@sessionsRequest')

    cy.fixture('sessions-list.json').then((sessions: Array<{ name: string }>) => {
      cy.get('[data-cy=session-item]').should('have.length', sessions.length)
      sessions.forEach((session) => {
        cy.contains('[data-cy=session-item]', session.name).should('be.visible')
      })
    })
  })

  it('sessions list - Create/Edit buttons visible for admin (mock)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      fixture: 'login-success.json', // admin: true
    }).as('loginRequest')
    cy.intercept('GET', '/api/session', { fixture: 'sessions-list.json' }).as('sessionsRequest')

    cy.get('input[formControlName=email]').type('yoga@studio.com')
    cy.get('input[formControlName=password]').type('test!1234')
    cy.get('button[type=submit]').click()

    cy.wait('@loginRequest')
    cy.wait('@sessionsRequest')

    cy.contains('button', 'Create').should('be.visible')
    cy.get('[data-cy=session-item]').first().contains('button', 'Edit').should('be.visible')
  })

  it('sessions list - Create/Edit buttons hidden for non-admin (mock)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      body: {
        token: 'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQHRlc3QuY29tIn0.mocked-signature-for-e2e-tests',
        type: 'Bearer',
        id: 2,
        username: 'user@test.com',
        firstName: 'Standard',
        lastName: 'User',
        admin: false,
      },
    }).as('loginRequest')
    cy.intercept('GET', '/api/session', { fixture: 'sessions-list.json' }).as('sessionsRequest')

    cy.get('input[formControlName=email]').type('user@test.com')
    cy.get('input[formControlName=password]').type('test1234')
    cy.get('button[type=submit]').click()

    cy.wait('@loginRequest')
    cy.wait('@sessionsRequest')

    // Comportement volontaire (list.component.html:5 et :34) : Create et Edit sont bien
    // masques pour un non-admin, mais Detail (lignes 30-33, aucune condition) reste visible.
    cy.contains('button', 'Create').should('not.exist')
    cy.get('[data-cy=session-item]').each(($item) => {
      cy.wrap($item).contains('button', 'Edit').should('not.exist')
    })
    cy.get('[data-cy=session-item]').first().contains('button', 'Detail').should('be.visible')
  })

  it('sessions list (real backend)', () => {
    // Cleanup systematique enregistre via l'alias @createdSessionId, lu par le hook
    // afterEach ci-dessous (partage de contexte Mocha/Cypress) : meme si une assertion
    // echoue plus bas, la session reelle creee ici sera quand meme supprimee.
    cy.request('POST', '/api/auth/login', {
      email: 'yoga@studio.com',
      password: 'test!1234',
    }).then((loginResponse) => {
      const authHeaders = { Authorization: `Bearer ${loginResponse.body.token}` }

      cy.request({ method: 'GET', url: '/api/teacher', headers: authHeaders }).then((teacherResponse) => {
        expect(
          teacherResponse.body,
          'Aucun teacher en base : executer back/src/main/resources/sql/insert_teacher.sql avant ce test'
        ).to.be.an('array').and.have.length.greaterThan(0)

        const teacherId = teacherResponse.body[0].id
        const sessionName = `E2E real session ${Date.now()}`

        cy.request({
          method: 'POST',
          url: '/api/session',
          headers: authHeaders,
          body: {
            name: sessionName,
            date: new Date().toISOString(),
            teacher_id: teacherId,
            description: 'Session created by the real-backend e2e test.',
            users: [],
          },
        }).then((createResponse) => {
          cy.wrap(createResponse.body.id).as('createdSessionId')

          cy.get('input[formControlName=email]').type('yoga@studio.com')
          cy.get('input[formControlName=password]').type('test!1234')
          cy.get('button[type=submit]').click()

          cy.url().should('include', '/sessions')
          cy.contains('[data-cy=session-item]', sessionName).should('be.visible')
        })
      })
    })
  })

  afterEach(function () {
    // S'execute apres CHAQUE test de la suite (pas seulement le test "real backend") ;
    // ne fait rien si aucune session reelle n'a ete creee dans le test qui vient de tourner.
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
