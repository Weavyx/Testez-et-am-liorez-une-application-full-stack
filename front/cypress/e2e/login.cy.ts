describe('Login spec', () => {
  beforeEach(() => {
    cy.visit('/login')
  })

  it('login success (mock)', () => {
    // MOCK : aucune dépendance au back réel, réponse stubée via fixture JwtResponse complète.
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 200,
      fixture: 'login-success.json',
    }).as('loginRequest')

    // Stubée car la page /sessions charge la liste des sessions au chargement.
    cy.intercept('GET', '/api/session', []).as('sessionsRequest')

    cy.get('input[formControlName=email]').type('yoga@studio.com')
    cy.get('input[formControlName=password]').type('test!1234')
    cy.get('button[type=submit]').click()

    cy.wait('@loginRequest')
    cy.url().should('include', '/sessions')
  })

  it('login error - wrong credentials (mock)', () => {
    // MOCK : le back renvoie 401 avec un corps vide aussi bien pour un mauvais mot de passe
    // que pour des champs manquants (comportement vérifié empiriquement sur le back réel) ;
    // on simule ici le cas "mauvais mot de passe" avec un formulaire valide.
    cy.intercept('POST', '/api/auth/login', {
      statusCode: 401,
      body: {},
    }).as('loginRequest')

    cy.get('input[formControlName=email]').type('yoga@studio.com')
    cy.get('input[formControlName=password]').type('wrongpassword')
    cy.get('button[type=submit]').click()

    cy.wait('@loginRequest')
    cy.contains('An error occurred').should('be.visible')
    cy.url().should('include', '/login')
  })

  it('login error - required field missing (validation front only)', () => {
    // PAS DE MOCK DE REPONSE : on espionne seulement l'appel (spy, pas de stub) pour prouver
    // qu'aucune requête n'est jamais émise. On ne peut pas tester ce cas via un code HTTP
    // (401 vide indistinguable d'un mauvais mot de passe côté back) : la seule preuve possible
    // est l'état du DOM (bouton disabled) + l'absence de requête réseau.
    cy.intercept('POST', '/api/auth/login').as('loginRequest')

    // Email rempli, mot de passe volontairement laissé vide.
    cy.get('input[formControlName=email]').type('yoga@studio.com')

    cy.get('button[type=submit]').should('be.disabled')

    // Un Enter dans un champ du formulaire ne déclenche pas de soumission implicite
    // tant que l'unique bouton submit reste disabled.
    cy.get('input[formControlName=password]').type('{enter}')

    cy.get('@loginRequest.all').should('have.length', 0)
    cy.url().should('include', '/login')
  })

  it('login success (real backend)', () => {
    // TEST REEL — aucun cy.intercept sur /api/auth/login : la requête part vers le vrai
    // backend Spring Boot (Docker, http://localhost:8080, proxy /api). Dépend du compte
    // admin pré-seedé (voir back/src/main/resources/sql/insert_user.sql, non auto-exécuté
    // par Spring mais déjà appliqué manuellement sur cet environnement local) :
    // yoga@studio.com / test!1234.
    cy.get('input[formControlName=email]').type('yoga@studio.com')
    cy.get('input[formControlName=password]').type('test!1234')
    cy.get('button[type=submit]').click()

    cy.url().should('include', '/sessions')

    // Preuve que ce n'est pas un mock qui traîne : la toolbar reflète bien l'état "connecté"
    // (le lien Logout n'existe que dans la branche @if ($isLogged() | async) du template).
    cy.contains('Logout').should('be.visible')
  })
})
