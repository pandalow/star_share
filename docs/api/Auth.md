## AUTH API

- Description: AuthController endpoints for verification, login, tokens, and profile lookup.

### 1. SendCode
Description: Sends a verification code for login/registration/password reset.

**EndPoint:**
```
POST api/v1/auth/send-code
```

**Authentication:**
Not required

**Request Body:**
```json
{
  "scene": "LOGIN",
  "identifierType": "EMAIL",
  "identifier": "user@example.com"
}
```

**Response:**
- **Success (200 OK):**
```json
{
  "identifier": "user@example.com",
  "scene": "LOGIN",
  "expireSeconds": 300
}
```

**Example Request:**
```bash
curl -X POST https://api.example.com/api/v1/auth/send-code \
  -H "Content-Type: application/json" \
  -d '{"scene":"LOGIN","identifierType":"EMAIL","identifier":"user@example.com"}'
```

### 2. Register
Description: Registers a new account after verifying the code.

**EndPoint:**
```
POST api/v1/auth/register
```

**Authentication:**
Not required

**Request Body:**
```json
{
  "identifierType": "EMAIL",
  "identifier": "user@example.com",
  "code": "834112",
  "password": "MyS3cret!",
  "agreeTerms": true
}
```

**Response:**
- **Success (200 OK):** Returns `AuthResponse` with user info and tokens.

**Example Request:**
```bash
curl -X POST https://api.example.com/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"identifierType":"EMAIL","identifier":"user@example.com","code":"834112"}'
```

### 3. Login
Description: Logs in using password or verification code.

**EndPoint:**
```
POST api/v1/auth/login
```

**Authentication:**
Not required

**Request Body:**
```json
{
  "identifierType": "EMAIL",
  "identifier": "user@example.com",
  "password": "MyS3cret!"
}
```

**Response:**
- **Success (200 OK):** Returns `AuthResponse`.

### 4. RefreshToken
Description: Exchanges a refresh token for new tokens.

**EndPoint:**
```
POST api/v1/auth/token/refresh
```

**Authentication:**
Not required

**Request Body:**
```json
{
  "refreshToken": "long-refresh-token"
}
```

**Response:**
- **Success (200 OK):** Returns `TokenResponse`.

### 5. Logout
Description: Revokes the supplied refresh token.

**EndPoint:**
```
POST api/v1/auth/logout
```

**Authentication:**
Not required

**Request Body:**
```json
{
  "refreshToken": "long-refresh-token"
}
```

**Response:**
- **Success (204 No Content):** Empty body.

### 6. ResetPassword
Description: Resets password using verification code.

**EndPoint:**
```
POST api/v1/auth/password/reset
```

**Authentication:**
Not required

**Request Body:**
```json
{
  "identifierType": "EMAIL",
  "identifier": "user@example.com",
  "code": "834112",
  "newPassword": "MyS3cret!"
}
```

**Response:**
- **Success (204 No Content):** Empty body.

### 7. Me
Description: Returns profile data for the authenticated user.

**EndPoint:**
```
GET api/v1/auth/me
```

**Authentication:**
Required (Bearer token)

**Request Body:**
None

**Response:**
- **Success (200 OK):** Returns `AuthUserResponse`.

**Example Request:**
```bash
curl -X GET https://api.example.com/api/v1/auth/me \
  -H "Authorization: Bearer <TOKEN>"
```
