// Suppression d'une session — scénarios Cypress.
//
// delete() (detail.component.ts:51-60) : aucune confirmation avant suppression, action immédiate
// au clic sur le bouton Delete. Succès -> MatSnackBar "Session deleted !" (3000ms) puis
// router.navigate(['sessions']). Aucune gestion d'erreur si le DELETE échoue (pas de callback
// error sur le subscribe, même famille que create/update déjà rencontrée) : aucun scénario
// "erreur DELETE affichée" n'est testé ici, ce serait un faux test.
//
// SessionService garde l'état de connexion en mémoire uniquement (BehaviorSubject, aucun
// localStorage/sessionStorage dans le code) : un cy.visit direct vers une route protégée par
// AuthGuard, même après un login réussi, provoque un rechargement complet qui réinitialise cet
// état et renvoie sur /login (login.component.ts:52 navigue toujours vers /sessions, sans
// mécanisme de returnUrl — impossible d'atteindre /sessions/detail/:id par ce biais après login).
// Le test 3 ci-dessous teste donc l'état "session introuvable" via un clic Detail normal depuis
// la liste (comme tous les autres tests de cette suite), en faisant échouer uniquement le GET
// /api/session/:id du détail — ce qui simule une suppression concurrente survenue entre le
// chargement de la liste et l'ouverture du détail, sans jamais recourir à un accès direct.
//
// Le test avec le vrai backend (Docker, http://localhost:8080, proxy /api) dépend du teacher
// id=1 seedé par back/src/main/resources/sql/insert_teacher.sql, même dépendance que
// sessions-list.cy.ts / sessions-detail.cy.ts / sessions-create.cy.ts.

const teacherResponse = {
  id: 1,
  firstName: 'Margot',
  lastName: 'DELAHAYE',
  createdAt: '2026-01-01T10:00:00',
  updatedAt: '2026-01-01T10:00:00',
}

describe('Session delete spec', () => {
  beforeEach(() => {
    cy.visit('/login')
  })

  it('session delete - removes session and redirects (mock)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      fixture: 'login-success.json', // admin: true
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

      cy.intercept('DELETE', `/api/session/${session.id}`, {
        statusCode: 200,
        body: '',
      }).as('deleteRequest')

      cy.contains('button', 'Delete').should('be.visible').click()

      cy.wait('@deleteRequest')

      // exitPage n'existe pas ici : delete() (detail.component.ts:55-58) ouvre le snackbar puis
      // navigate de façon synchrone, même schéma que exitPage() dans form.component.ts.
      cy.contains('Session deleted !').should('be.visible')
      cy.url().should('match', /\/sessions$/)
    })
  })

  it('session delete - session no longer appears in list after real deletion (real backend)', () => {
    // Cleanup de sécurité enregistré via l'alias @createdSessionId, même pattern que
    // sessions-list.cy.ts / sessions-detail.cy.ts / sessions-create.cy.ts : si une assertion
    // échoue avant le clic Delete, la session réelle créée ici sera quand même supprimée.
    // failOnStatusCode:false car, dans le cas nominal, ce test supprime déjà la session lui-même
    // via l'UI : le DELETE de sécurité tombe alors sur un 404 attendu, sans faire échouer la suite.
    const sessionName = `E2E delete real session ${Date.now()}`

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
          name: sessionName,
          date: new Date().toISOString(),
          // Dépend du teacher id=1 seedé par back/src/main/resources/sql/insert_teacher.sql
          // ("Margot DELAHAYE"), appliqué manuellement au préalable sur cet environnement.
          teacher_id: 1,
          description: 'Session created by the real-backend delete e2e test.',
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
        cy.contains('button', 'Delete').should('be.visible').click()

        cy.url().should('match', /\/sessions$/)
        cy.contains('.item', sessionName).should('not.exist')
      })
    })
  })

  it('session detail - stale/deleted session id shows empty page (mock)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      fixture: 'login-success.json', // admin: true
    }).as('loginRequest')

    cy.fixture('session-detail.json').then((session) => {
      cy.intercept('GET', '/api/session', [session]).as('sessionsRequest')
      // Simule une suppression concurrente : la session apparaît toujours dans la liste, mais
      // son GET de détail échoue déjà en 404 corps vide (comportement réel confirmé en live).
      cy.intercept('GET', `/api/session/${session.id}`, {
        statusCode: 404,
        body: '',
      }).as('sessionDetailRequest')

      cy.get('input[formControlName=email]').type('yoga@studio.com')
      cy.get('input[formControlName=password]').type('test!1234')
      cy.get('button[type=submit]').click()

      cy.wait('@loginRequest')
      cy.wait('@sessionsRequest')

      cy.contains('.item', session.name).within(() => {
        cy.contains('button', 'Detail').click()
      })

      cy.wait('@sessionDetailRequest')

      // Template entier enveloppé dans @if (session) (detail.component.html:2-80) : sur 404,
      // rien ne s'affiche. Vérifie l'absence de tout contenu du composant, sans erreur JS
      // bloquante visible (la page reste utilisable, pas de crash de rendu Angular).
      cy.url().should('include', '/sessions/detail/')
      cy.get('h1').should('not.exist')
      cy.contains('button', 'Delete').should('not.exist')
      cy.contains('button', 'Participate').should('not.exist')
      cy.contains('button', 'Do not participate').should('not.exist')
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
