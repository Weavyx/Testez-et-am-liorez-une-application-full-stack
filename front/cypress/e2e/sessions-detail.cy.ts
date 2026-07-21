// Détail d'une session — scénarios Cypress.
//
// SessionService garde l'état de connexion en mémoire (BehaviorSubject), pas dans le
// localStorage ni un cookie (voir front/src/app/core/service/session.service.ts) : un
// cy.visit direct vers /sessions/detail/:id après login perdrait donc cet état et
// renverrait sur /login via AuthGuard. Chaque test navigue donc vers le détail en cliquant
// sur le bouton "Detail" de la liste (routerLink interne, pas de rechargement de page),
// exactement comme le ferait un utilisateur reel.
//
// Le dernier test ("real backend") tape le vrai backend Spring Boot (Docker,
// http://localhost:8080, proxy /api) et NE PASSE QUE SI le script SQL
// back/src/main/resources/sql/insert_teacher.sql a ete applique manuellement au prealable
// sur la base (meme dependance que front/cypress/e2e/sessions-list.cy.ts) : il suppose
// l'existence du teacher id=1 ("Margot DELAHAYE") pour verifier l'affichage du formateur.

const nonAdminLogin = {
  token: 'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQHRlc3QuY29tIn0.mocked-signature-for-e2e-tests',
  type: 'Bearer',
  id: 2,
  username: 'user@test.com',
  firstName: 'Standard',
  lastName: 'User',
  admin: false,
}

const teacherResponse = {
  id: 1,
  firstName: 'Margot',
  lastName: 'DELAHAYE',
  createdAt: '2026-01-01T10:00:00',
  updatedAt: '2026-01-01T10:00:00',
}

describe('Session detail spec', () => {
  beforeEach(() => {
    cy.visit('/login')
  })

  it('session detail - displays info (mock)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      body: nonAdminLogin,
    }).as('loginRequest')
    cy.intercept('GET', '/api/teacher/1', teacherResponse).as('teacherRequest')

    cy.fixture('session-detail.json').then((session) => {
      cy.intercept('GET', '/api/session', [session]).as('sessionsRequest')
      cy.intercept('GET', `/api/session/${session.id}`, session).as('sessionDetailRequest')

      cy.get('input[formControlName=email]').type('user@test.com')
      cy.get('input[formControlName=password]').type('test1234')
      cy.get('button[type=submit]').click()

      cy.wait('@loginRequest')
      cy.wait('@sessionsRequest')

      cy.contains('.item', session.name).within(() => {
        cy.contains('button', 'Detail').click()
      })

      cy.wait('@sessionDetailRequest')
      cy.wait('@teacherRequest')

      cy.contains('h1', session.name).should('be.visible')
      cy.get('.description').should('contain.text', session.description)
      cy.contains('September 5, 2026').should('be.visible')
      cy.contains('Margot DELAHAYE').should('be.visible')
    })
  })

  it('session detail - Delete button visible for admin (mock)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      fixture: 'login-success.json', // admin: true, id: 1
    }).as('loginRequest')
    cy.intercept('GET', '/api/teacher/1', teacherResponse).as('teacherRequest')

    cy.fixture('session-detail.json').then((session) => {
      cy.intercept('GET', '/api/session', [session]).as('sessionsRequest')
      cy.intercept('GET', `/api/session/${session.id}`, session).as('sessionDetailRequest')

      cy.get('input[formControlName=email]').type('yoga@studio.com')
      cy.get('input[formControlName=password]').type('test!1234')
      cy.get('button[type=submit]').click()

      cy.wait('@loginRequest')
      cy.wait('@sessionsRequest')

      cy.contains('.item', session.name).within(() => {
        cy.contains('button', 'Detail').click()
      })

      cy.wait('@sessionDetailRequest')
      cy.wait('@teacherRequest')

      cy.contains('button', 'Delete').should('be.visible')
      cy.contains('button', 'Participate').should('not.exist')
      cy.contains('button', 'Do not participate').should('not.exist')
    })
  })

  it('session detail - Participate button visible for non-admin not participating (mock)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      body: nonAdminLogin,
    }).as('loginRequest')
    cy.intercept('GET', '/api/teacher/1', teacherResponse).as('teacherRequest')

    cy.fixture('session-detail.json').then((session) => {
      const notParticipating = { ...session, users: [] }
      cy.intercept('GET', '/api/session', [notParticipating]).as('sessionsRequest')
      cy.intercept('GET', `/api/session/${session.id}`, notParticipating).as('sessionDetailRequest')

      cy.get('input[formControlName=email]').type('user@test.com')
      cy.get('input[formControlName=password]').type('test1234')
      cy.get('button[type=submit]').click()

      cy.wait('@loginRequest')
      cy.wait('@sessionsRequest')

      cy.contains('.item', session.name).within(() => {
        cy.contains('button', 'Detail').click()
      })

      cy.wait('@sessionDetailRequest')
      cy.wait('@teacherRequest')

      cy.contains('button', 'Participate').should('be.visible')
      cy.contains('button', 'Do not participate').should('not.exist')
      cy.contains('button', 'Delete').should('not.exist')
    })
  })

  it('session detail - Do not participate button visible for non-admin already participating (mock)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      body: nonAdminLogin,
    }).as('loginRequest')
    cy.intercept('GET', '/api/teacher/1', teacherResponse).as('teacherRequest')

    cy.fixture('session-detail.json').then((session) => {
      // userId 2 = nonAdminLogin.id, deja dans users[] : le bouton doit basculer sur
      // "Do not participate" (detail.component.ts:80).
      const alreadyParticipating = { ...session, users: [2] }
      cy.intercept('GET', '/api/session', [alreadyParticipating]).as('sessionsRequest')
      cy.intercept('GET', `/api/session/${session.id}`, alreadyParticipating).as('sessionDetailRequest')

      cy.get('input[formControlName=email]').type('user@test.com')
      cy.get('input[formControlName=password]').type('test1234')
      cy.get('button[type=submit]').click()

      cy.wait('@loginRequest')
      cy.wait('@sessionsRequest')

      cy.contains('.item', session.name).within(() => {
        cy.contains('button', 'Detail').click()
      })

      cy.wait('@sessionDetailRequest')
      cy.wait('@teacherRequest')

      cy.contains('button', 'Do not participate').should('be.visible')
      cy.contains('button', 'Participate').should('not.exist')
      cy.contains('button', 'Delete').should('not.exist')
    })
  })

  it('session detail - participate action updates UI (mock)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      body: nonAdminLogin,
    }).as('loginRequest')
    cy.intercept('GET', '/api/teacher/1', teacherResponse).as('teacherRequest')

    cy.fixture('session-detail.json').then((session) => {
      const notParticipating = { ...session, users: [] }
      cy.intercept('GET', '/api/session', [notParticipating]).as('sessionsRequest')
      cy.intercept('GET', `/api/session/${session.id}`, notParticipating).as('sessionDetailRequest')

      cy.get('input[formControlName=email]').type('user@test.com')
      cy.get('input[formControlName=password]').type('test1234')
      cy.get('button[type=submit]').click()

      cy.wait('@loginRequest')
      cy.wait('@sessionsRequest')

      cy.contains('.item', session.name).within(() => {
        cy.contains('button', 'Detail').click()
      })

      cy.wait('@sessionDetailRequest')
      cy.wait('@teacherRequest')
      cy.contains('button', 'Participate').should('be.visible')

      // Le composant ne met pas a jour isParticipate de facon optimiste : apres le POST
      // participate, il refait un GET /api/session/{id} complet (detail.component.ts:62-66
      // et :74-86). Il faut donc mocker les deux appels : l'action ET le GET de rafraichissement.
      cy.intercept('POST', `/api/session/${session.id}/participate/2`, {
        statusCode: 200,
        body: '',
      }).as('participateRequest')

      const nowParticipating = { ...session, users: [2] }
      cy.intercept('GET', `/api/session/${session.id}`, nowParticipating).as('sessionDetailRefreshRequest')

      cy.contains('button', 'Participate').click()

      cy.wait('@participateRequest')
      cy.wait('@sessionDetailRefreshRequest')

      cy.contains('button', 'Do not participate').should('be.visible')
      cy.contains('button', 'Participate').should('not.exist')
    })
  })

  it('session detail (real backend) - full lifecycle', () => {
    // Cleanup de securite enregistre via l'alias @createdSessionId, lu par le hook afterEach
    // ci-dessous (meme pattern que front/cypress/e2e/sessions-list.cy.ts) : si une assertion
    // echoue avant le clic sur Delete, la session reelle creee ici sera quand meme supprimee.
    // failOnStatusCode:false car, dans le cas nominal, ce test supprime deja la session
    // lui-meme via l'UI : le DELETE de securite tombe alors sur un 404 attendu, sans faire
    // echouer la suite.
    cy.request('POST', '/api/auth/login', {
      email: 'yoga@studio.com',
      password: 'test!1234',
    }).then((loginResponse) => {
      const authHeaders = { Authorization: `Bearer ${loginResponse.body.token}` }
      const sessionName = `E2E detail real session ${Date.now()}`

      cy.request({
        method: 'POST',
        url: '/api/session',
        headers: authHeaders,
        body: {
          name: sessionName,
          date: new Date().toISOString(),
          // Depend du teacher id=1 seede par back/src/main/resources/sql/insert_teacher.sql
          // ("Margot DELAHAYE"), applique manuellement au prealable sur cet environnement.
          teacher_id: 1,
          description: 'Session created by the real-backend detail e2e test.',
          users: [],
        },
      }).then((createResponse) => {
        cy.wrap(createResponse.body.id).as('createdSessionId')

        cy.get('input[formControlName=email]').type('yoga@studio.com')
        cy.get('input[formControlName=password]').type('test!1234')
        cy.get('button[type=submit]').click()

        cy.url().should('include', '/sessions')
        cy.contains('.item', sessionName).within(() => {
          cy.contains('button', 'Detail').click()
        })

        cy.url().should('include', '/sessions/detail/')
        // Le h1 passe session.name dans le pipe titlecase (detail.component.html:10), qui
        // change la casse (ex. "E2E" -> "E2e") : comparaison insensible a la casse pour
        // rester robuste a cette transformation, plutot que dupliquer sa logique ici.
        cy.contains('h1', new RegExp(sessionName, 'i')).should('be.visible')
        cy.contains('Margot DELAHAYE').should('be.visible')

        cy.contains('button', 'Delete').should('be.visible').click()

        cy.url().should('match', /\/sessions$/)
        cy.contains('.item', sessionName).should('not.exist')
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
        failOnStatusCode: false,
      })
    })
  })
})
