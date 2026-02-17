import { OAuth2Server } from 'oauth2-mock-server';

const server = new OAuth2Server();

// Generate a new RSA key for signing tokens
await server.issuer.keys.generate('RS256');

// Customize the token to include orgId
server.service.on('beforeTokenSigning', (token, req) => {
    token.payload.orgId = "1"; // Hardcoded for this simulation as requested
});

// Start the server
await server.start(8083, '127.0.0.1');

console.log('OAuth2 Mock Server listening on http://127.0.0.1:8083');
console.log('Issuer URL:', server.issuer.url);
console.log('JWKS: http://127.0.0.1:8083/jwks');

// By default, the server accepts any client_id and client_secret
// Using the "client_credentials" flow or simply requesting a token directly works.
// For implicit flow simulation or just getting a token:
// curl -X POST http://localhost:8083/token -d "grant_type=client_credentials&scope=openid"
