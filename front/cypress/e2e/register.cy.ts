describe('Register spec', () => {
  beforeEach(() => {
    cy.visit('/register')
  })

  it('register success (mock)', () => {
    // MOCK : aucune dépendance au back réel. Réponse réelle du endpoint confirmée en lisant
    // AuthController.registerUser (retourne un MessageResponse, pas un JwtResponse) :
    // { "message": "User registered successfully!" } avec HTTP 200.
    cy.intercept('POST', '/api/auth/register', {
      statusCode: 200,
      fixture: 'register-success.json',
    }).as('registerRequest')

    cy.get('input[formControlName=firstName]').type('Test')
    cy.get('input[formControlName=lastName]').type('User')
    cy.get('input[formControlName=email]').type('mock.register@test.com')
    cy.get('input[formControlName=password]').type('test1234')
    cy.get('button[type=submit]').click()

    cy.wait('@registerRequest')
    cy.url().should('include', '/login')
  })

  it('register error - email already taken (mock)', () => {
    // MOCK : simule la collision d'email, comportement confirmé empiriquement sur le back réel
    // (400 + message exact "Error: Email is already taken!", via BadRequestException).
    cy.intercept('POST', '/api/auth/register', {
      statusCode: 400,
      fixture: 'register-conflict.json',
    }).as('registerRequest')

    cy.get('input[formControlName=firstName]').type('Test')
    cy.get('input[formControlName=lastName]').type('User')
    cy.get('input[formControlName=email]').type('yoga@studio.com')
    cy.get('input[formControlName=password]').type('test1234')
    cy.get('button[type=submit]').click()

    cy.wait('@registerRequest')
    cy.get('.error').should('be.visible').and('contain.text', 'An error occurred')
    cy.url().should('include', '/register')
  })

  it('register error - required field missing (validation front only)', () => {
    // PAS DE MOCK DE REPONSE : spy uniquement (pas de stub), même logique que pour le login.
    // Le back renvoie 401 corps vide pour une validation invalide, indistinguable d'une autre
    // erreur 401 : la seule preuve fiable est le DOM (bouton disabled) + absence de requête.
    cy.intercept('POST', '/api/auth/register').as('registerRequest')

    // Email volontairement laissé vide, tous les autres champs valides.
    cy.get('input[formControlName=firstName]').type('Test')
    cy.get('input[formControlName=lastName]').type('User')
    cy.get('input[formControlName=password]').type('test1234')

    cy.get('button[type=submit]').should('be.disabled')

    // Un Enter dans un champ du formulaire ne déclenche pas de soumission implicite
    // tant que l'unique bouton submit reste disabled.
    cy.get('input[formControlName=password]').type('{enter}')

    cy.get('@registerRequest.all').should('have.length', 0)
    cy.url().should('include', '/register')
  })

  it('register success (real backend)', () => {
    // TEST REEL — aucun cy.intercept : la requête part vers le vrai backend Spring Boot
    // (Docker, http://localhost:8080, proxy /api). Email généré dynamiquement pour éviter
    // toute collision "Email is already taken!" entre deux exécutions successives : aucun
    // cleanup API n'est disponible côté front, le compte créé reste en base après le test
    // (le volume MySQL est persistant entre redémarrages du back).
    const uniqueEmail = `e2e_${Date.now()}@test.com`
    const password = 'test1234'

    cy.get('input[formControlName=firstName]').type('E2E')
    cy.get('input[formControlName=lastName]').type('Runner')
    cy.get('input[formControlName=email]').type(uniqueEmail)
    cy.get('input[formControlName=password]').type(password)
    cy.get('button[type=submit]').click()

    cy.url().should('include', '/login')

    // Preuve du cycle complet register -> login réel : le compte fraîchement créé doit
    // pouvoir se connecter immédiatement contre le vrai backend (appel direct à l'API,
    // en dehors de l'UI, pour isoler la vérification de la seule authentification).
    cy.request('POST', '/api/auth/login', {
      email: uniqueEmail,
      password,
    }).then((response) => {
      expect(response.status).to.eq(200)
      expect(response.body).to.have.property('token')
      expect(response.body.username).to.eq(uniqueEmail)
      expect(response.body.admin).to.eq(false)
    })
  })
})
