# Integração de login com Google — Frontend

Este documento descreve como o frontend deve integrar o login Google com a API.

## Visão geral

O frontend obtém um **Google ID Token** usando o Google Identity Services e o
envia para a API. A API valida o token e devolve o JWT próprio da aplicação.

```text
Google Identity Services
        |
        | Google ID Token (credential)
        v
Frontend
        |
        | POST /api/v1/auth/google
        v
API
        |
        | JWT da aplicação
        v
Frontend autenticado
```

O ID Token do Google não é o token de sessão da aplicação e não deve ser usado
como `Bearer` nas rotas protegidas da API. Após o login, use exclusivamente o
JWT retornado pela API.

## Configuração necessária

### Variáveis do frontend

Configure o client ID Web do Google no ambiente do frontend. Exemplo para Vite:

```env
VITE_GOOGLE_CLIENT_ID=1234567890-abc123.apps.googleusercontent.com
```

O valor deve ser o mesmo client ID configurado no backend em `GOOGLE_CLIENT_ID`.
Não exponha client secrets no frontend; esta integração utiliza somente o client
ID público.

### Google Cloud Console

No cliente OAuth 2.0 do tipo **Web application**:

1. Adicione as URLs do frontend em **Authorized JavaScript origins**.
2. Inclua os ambientes utilizados, por exemplo:
   - `http://localhost:5173`
   - `https://app.exemplo.com`
3. Use o client ID Web resultante tanto no frontend quanto no backend.

### CORS do backend

O domínio do frontend também deve estar presente na variável `FRONTEND_ORIGINS`
do backend. Caso contrário, o navegador bloqueará as chamadas à API.

Exemplo:

```env
FRONTEND_ORIGINS=http://localhost:5173,https://app.exemplo.com
```

## Carregamento do Google Identity Services

Carregue o script oficial uma vez na aplicação:

```html
<script src="https://accounts.google.com/gsi/client" async defer></script>
```

Após o script estar disponível, inicialize o cliente e renderize o botão. O
exemplo abaixo usa JavaScript; adapte-o ao framework utilizado.

```js
window.google.accounts.id.initialize({
    client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID,
    callback: handleGoogleCredential
});

window.google.accounts.id.renderButton(
    document.getElementById("google-login-button"),
    { theme: "outline", size: "large", text: "signin_with" }
);
```

O callback recebe um objeto com `credential`. Esse valor é o Google ID Token a
ser enviado à API.

## Login normal com Google

### Requisição

```http
POST /api/v1/auth/google
Content-Type: application/json

{
  "idToken": "<credential recebida do Google>"
}
```

Não envie access tokens, tokens do Firebase ou o `client_id` nesta chamada.

### Implementação sugerida

```js
async function handleGoogleCredential(googleResponse) {
    const response = await fetch(`${API_URL}/api/v1/auth/google`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ idToken: googleResponse.credential })
    });

    const body = await response.json();

    if (response.ok) {
        saveApplicationToken(body.token);
        navigateToAuthenticatedArea();
        return;
    }

    handleGoogleLoginError(response.status, body, googleResponse.credential);
}
```

### Sucesso — `200 OK`

```json
{
  "token": "<jwt da aplicação>"
}
```

Salve e envie esse token seguindo a estratégia de autenticação já adotada pelo
frontend. Em chamadas protegidas, use:

```http
Authorization: Bearer <jwt da aplicação>
```

## Vínculo de conta existente

Uma conta criada anteriormente com e-mail e senha não é vinculada
automaticamente a uma conta Google que possua o mesmo e-mail. Isso evita que o
e-mail seja usado como prova de identidade.

Nesse caso, o login Google retorna `409 Conflict`:

```json
{
  "status": 409,
  "error": "GoogleAccountLinkRequired",
  "message": "Google account must be linked from an authenticated session",
  "path": "/api/v1/auth/google"
}
```

### Fluxo de vínculo

1. Mostre que a conta já existe e solicite o login normal com e-mail e senha.
2. Após receber o JWT da aplicação, solicite novamente a credencial do Google
   caso a anterior não esteja disponível ou possa ter expirado.
3. Chame o endpoint autenticado de vínculo.
4. Com `204 No Content`, informe sucesso. O JWT atual permanece válido.

### Requisição de vínculo

```http
POST /api/v1/auth/google/link
Authorization: Bearer <jwt da aplicação>
Content-Type: application/json

{
  "idToken": "<credential recebida do Google>"
}
```

Exemplo:

```js
async function linkGoogleAccount(googleCredential, applicationToken) {
    const response = await fetch(`${API_URL}/api/v1/auth/google/link`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${applicationToken}`
        },
        body: JSON.stringify({ idToken: googleCredential })
    });

    if (response.status === 204) {
        showSuccess("Conta Google vinculada com sucesso.");
        return;
    }

    const body = await response.json();
    handleGoogleLinkError(response.status, body);
}
```

Após o vínculo, os próximos logins Google usarão o mesmo endpoint público
`POST /api/v1/auth/google`.

## Tratamento de erros

| Endpoint | Status | Código de erro | Ação sugerida no frontend |
| --- | --- | --- | --- |
| `/auth/google` | `200` | — | Guardar o JWT da aplicação e redirecionar. |
| `/auth/google` | `400` | `ValidationError` | A credencial não foi enviada; solicitar login Google novamente. |
| `/auth/google` | `401` | `InvalidTokenException` | Informar que o login Google expirou ou é inválido; solicitar nova credencial. |
| `/auth/google` | `409` | `GoogleAccountLinkRequired` | Direcionar para login local e, depois, para o vínculo. |
| `/auth/google/link` | `204` | — | Exibir confirmação de vínculo. |
| `/auth/google/link` | `401` | `Unauthorized` ou `InvalidTokenException` | Solicitar novamente o login da aplicação ou a credencial Google, conforme o caso. |
| `/auth/google/link` | `409` | `GoogleAccountAlreadyLinked` | Informar que a conta Google já está vinculada a outro usuário. |

Também trate erros de rede e respostas `5xx` com uma mensagem genérica e opção
de tentar novamente. Não exiba o Google ID Token em telas, logs do navegador ou
mensagens de erro.

## Critérios de aceite

- O botão Google aparece apenas após o script do Google estar carregado.
- Um novo usuário Google recebe o JWT da API após o login.
- Um usuário já vinculado entra novamente sem precisar de senha local.
- Uma conta local sem vínculo recebe o fluxo de vínculo explícito, não um login
  automático por e-mail.
- O endpoint de vínculo é chamado sempre com o JWT da aplicação.
- Um token Google inválido, expirado ou ausente não autentica o usuário.
- O frontend nunca envia o Google ID Token como `Authorization: Bearer` para a
  API.
