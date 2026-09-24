// Page "Account" (User information) — scénarios Cypress.
//
// MeComponent (front/src/app/components/me/me.component.ts) charge GET /api/user/{id}, où {id} est
// l'id de l'utilisateur CONNECTÉ (sessionService.sessionInformation.id, me.component.ts:26-31) —
// jamais un paramètre de route (la route /me, app.routes.ts:55-59, ne prend pas d'id). On accède
// donc toujours à la page en cliquant sur le lien "Account" de la toolbar (app.component.html:7,
// visible seulement si connecté), jamais via cy.visit direct.
//
// Le UserDto renvoyé par le back n'inclut jamais le mot de passe (@JsonIgnore,
// back/.../dto/UserDto.java:35-37) : la fixture user-account.json ne porte donc pas de champ
// password, ce qui reflète la réponse réelle (le composant ne le lit de toute façon jamais).
//
// Le bouton "Delete account" n'existe que pour un non-admin (me.component.html:19-27) : les tests
// mockés utilisent donc un login non-admin par défaut, sauf le test 2 qui vérifie spécifiquement le
// cas admin (masquage du bouton). Comme sessions-detail.cy.ts, ce login non-admin n'a pas de
// fixture dédiée (login-success.json est admin: true) : il est défini localement ci-dessous.
//
// delete() (me.component.ts:37-46) n'a AUCUNE gestion d'erreur (pas de callback error sur le
// subscribe), même famille que les autres composants déjà audités : aucun scénario "erreur DELETE
// affichée" n'est testé ici. Après suppression, delete() navigue vers ['/'], qui redirige aussitôt
// vers /login (route '' -> redirectTo: 'login', app.routes.ts:12-17) : les assertions ciblent donc
// /login, l'état final réellement observable dans le navigateur.
//
// Le dernier test ("real backend") tape le vrai backend Spring Boot (Docker, http://localhost:8080,
// proxy /api) et crée un compte JETABLE via register dynamique (email horodaté, même pattern que
// register.cy.ts::"register success (real backend)"), plutôt que de réutiliser yoga@studio.com
// (admin, qui n'a pas le bouton Delete). Aucun cleanup afterEach n'est nécessaire ni possible : le
// compte est supprimé par le test lui-même via le clic UI sur "Delete", et il n'existe côté front
// aucun endpoint permettant de supprimer un utilisateur autrement que par ce flux (UserService côté
// back n'expose pas non plus de suppression "admin pour un autre user" utilisable ici,
// UserService.java:28-34 exige que le compte ciblé soit celui de l'appelant). La preuve que la
// suppression a bien eu lieu est un login raté (401) avec les mêmes identifiants juste après.

const nonAdminLogin = {
  token: 'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqYW5lLmRvZUB0ZXN0LmNvbSJ9.mocked-signature-for-e2e-tests',
  type: 'Bearer',
  id: 2,
  username: 'jane.doe@test.com',
  firstName: 'Jane',
  lastName: 'Doe',
  admin: false,
}

// Reproduit le format 'longDate' du DatePipe Angular (aucun LOCALE_ID custom dans l'app -> en-US
// par défaut, format prédéfini 'longDate' = 'MMMM d, y').
const formatLongDate = (iso: string) =>
  new Date(iso).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })

describe('Account spec', () => {
  beforeEach(() => {
    cy.visit('/login')
  })

  it('account - displays user information (mock)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      body: nonAdminLogin,
    }).as('loginRequest')
    cy.intercept('GET', '/api/session', []).as('sessionsRequest')

    cy.get('input[formControlName=email]').type('jane.doe@test.com')
    cy.get('input[formControlName=password]').type('test1234')
    cy.get('button[type=submit]').click()

    cy.wait('@loginRequest')
    cy.wait('@sessionsRequest')

    cy.fixture('user-account.json').then((user) => {
      cy.intercept('GET', `/api/user/${user.id}`, user).as('userRequest')

      cy.contains('Account').click()
      cy.wait('@userRequest')

      cy.url().should('include', '/me')
      cy.contains('h1', 'User information').should('be.visible')

      // me.component.html:14-15 : Name affiche le lastName en majuscules (pipe uppercase).
      cy.contains(`Name: ${user.firstName} ${user.lastName.toUpperCase()}`).should('be.visible')
      cy.contains(`Email: ${user.email}`).should('be.visible')
      cy.contains(`Create at: ${formatLongDate(user.createdAt)}`).should('be.visible')
      cy.contains(`Last update: ${formatLongDate(user.updatedAt)}`).should('be.visible')
    })
  })

  it('account - shows admin status and hides delete button for admin (mock)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      fixture: 'login-success.json', // admin: true, id: 1
    }).as('loginRequest')
    cy.intercept('GET', '/api/session', []).as('sessionsRequest')

    cy.get('input[formControlName=email]').type('yoga@studio.com')
    cy.get('input[formControlName=password]').type('test!1234')
    cy.get('button[type=submit]').click()

    cy.wait('@loginRequest')
    cy.wait('@sessionsRequest')

    cy.fixture('user-account.json').then((user) => {
      const adminUser = { ...user, id: 1, admin: true }
      cy.intercept('GET', '/api/user/1', adminUser).as('userRequest')

      cy.contains('Account').click()
      cy.wait('@userRequest')

      // me.component.html:16-18 / :19-27 : blocs mutuellement exclusifs sur user.admin.
      cy.contains('You are admin').should('be.visible')
      cy.contains('button', 'Delete').should('not.exist')
      cy.contains('Delete my account:').should('not.exist')
    })
  })

  it('account - delete account logs out and redirects (mock)', () => {
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      body: nonAdminLogin,
    }).as('loginRequest')
    cy.intercept('GET', '/api/session', []).as('sessionsRequest')

    cy.get('input[formControlName=email]').type('jane.doe@test.com')
    cy.get('input[formControlName=password]').type('test1234')
    cy.get('button[type=submit]').click()

    cy.wait('@loginRequest')
    cy.wait('@sessionsRequest')

    cy.fixture('user-account.json').then((user) => {
      cy.intercept('GET', `/api/user/${user.id}`, user).as('userRequest')

      cy.contains('Account').click()
      cy.wait('@userRequest')

      cy.intercept('DELETE', `/api/user/${user.id}`, {
        statusCode: 200,
        body: '',
      }).as('deleteRequest')

      cy.contains('button', 'Delete').should('be.visible').click()

      cy.wait('@deleteRequest')

      // delete() (me.component.ts:37-46) : snackbar puis logOut() puis navigate(['/']), qui
      // redirige immédiatement vers /login (app.routes.ts:12-17).
      cy.contains('Your account has been deleted !').should('be.visible')
      cy.url().should('include', '/login')

      // Preuve que logOut() a bien été appliqué : la toolbar repasse à l'état "non connecté".
      cy.contains('Login').should('be.visible')
      cy.contains('Account').should('not.exist')
    })
  })

  it('account (real backend) - displays real user data and delete works', () => {
    const uniqueEmail = `e2e_account_${Date.now()}@test.com`
    const password = 'test1234'
    const firstName = 'E2E'
    const lastName = 'Runner'

    cy.visit('/register')
    cy.get('input[formControlName=firstName]').type(firstName)
    cy.get('input[formControlName=lastName]').type(lastName)
    cy.get('input[formControlName=email]').type(uniqueEmail)
    cy.get('input[formControlName=password]').type(password)
    cy.get('button[type=submit]').click()

    cy.url().should('include', '/login')

    cy.get('input[formControlName=email]').type(uniqueEmail)
    cy.get('input[formControlName=password]').type(password)
    cy.get('button[type=submit]').click()

    cy.url().should('include', '/sessions')

    cy.contains('Account').click()
    cy.url().should('include', '/me')

    // Données réelles renvoyées par GET /api/user/{id}, correspondant à ce qui a été fourni au
    // register (me.component.html:14-15 : lastName en majuscules).
    cy.contains(`Name: ${firstName} ${lastName.toUpperCase()}`).should('be.visible')
    cy.contains(`Email: ${uniqueEmail}`).should('be.visible')
    cy.contains('Create at:').should('be.visible')

    cy.contains('button', 'Delete').should('be.visible').click()

    cy.contains('Your account has been deleted !').should('be.visible')
    cy.url().should('include', '/login')

    // Le compte ne doit plus pouvoir se connecter : preuve directe (hors UI) que la suppression
    // a bien été persistée côté back, sans dépendre d'un endpoint de cleanup externe.
    cy.request({
      method: 'POST',
      url: '/api/auth/login',
      body: { email: uniqueEmail, password },
      failOnStatusCode: false,
    }).then((response) => {
      expect(response.status).to.eq(401)
    })
  })
})
