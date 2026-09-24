// Déconnexion — scénarios Cypress. Dernier écran de la série e2e Ex2-Front.
//
// logout() (front/src/app/app.component.ts:24-27) : sessionService.logOut() (reset
// isLogged/sessionInformation, session.service.ts:25-29) puis router.navigate(['']), qui redirige
// aussitôt vers /login (route '' -> redirectTo: 'login', app.routes.ts:12-17). L'état final
// observable dans le navigateur est donc /login, pas /.
//
// Le lien "Logout" (app.component.html:8, sélecteur .link) n'existe que dans le bloc
// @if ($isLogged() | async) du template (app.component.html:4-9) : sa disparition après logout est
// une preuve UI, mais pas une preuve que les routes protégées sont réellement bloquées — d'où le
// test 2 ci-dessous, qui vérifie AuthGuard.canActivate() (front/src/app/guards/auth.guard.ts:14-20)
// directement via un cy.visit direct sur une route protégée, indépendamment de l'état de la
// toolbar.
//
// Le dernier test ("real backend") tape le vrai backend Spring Boot (Docker, http://localhost:8080,
// proxy /api) avec le compte admin pré-seedé yoga@studio.com / test!1234 (même dépendance que
// login.cy.ts::"login success (real backend)"). Aucune donnée n'est créée par ce test : aucun
// cleanup n'est nécessaire.

describe('Logout spec', () => {
  beforeEach(() => {
    cy.visit('/login')
  })

  it('logout - clears session, redirects to login, and toolbar reverts (mock)', () => {
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
    cy.contains('Logout').should('be.visible')

    cy.contains('Logout').click()

    cy.url().should('include', '/login')

    // Toolbar : app.component.html:4-15 bascule sur le bloc @else (Login/Register) dès que
    // $isLogged() repasse à false.
    cy.contains('Login').should('be.visible')
    cy.contains('Register').should('be.visible')
    cy.contains('Sessions').should('not.exist')
    cy.contains('Account').should('not.exist')
    cy.contains('Logout').should('not.exist')
  })

  it('logout - protected routes become inaccessible after logout (mock)', () => {
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

    cy.contains('Logout').click()
    cy.url().should('include', '/login')

    // AuthGuard.canActivate() (auth.guard.ts:14-20) : sessionService.isLogged est un état en
    // mémoire (pas de localStorage), déjà remis à false par logOut() ; cy.visit recharge la page
    // et le SPA repart donc systématiquement non connecté, comme un accès direct par URL le ferait.
    cy.visit('/sessions')
    cy.url().should('include', '/login')

    cy.visit('/me')
    cy.url().should('include', '/login')
  })

  it('logout (real backend) - full real session teardown', () => {
    cy.get('input[formControlName=email]').type('yoga@studio.com')
    cy.get('input[formControlName=password]').type('test!1234')
    cy.get('button[type=submit]').click()

    cy.url().should('include', '/sessions')
    cy.contains('Logout').should('be.visible').click()

    cy.url().should('include', '/login')
    cy.contains('Login').should('be.visible')
    cy.contains('Register').should('be.visible')

    cy.visit('/sessions')
    cy.url().should('include', '/login')
  })
})
