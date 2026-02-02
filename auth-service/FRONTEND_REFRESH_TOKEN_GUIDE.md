# Frontend Refresh Token Usage Guide

This guide explains how the frontend can use refresh tokens to obtain new access tokens when the current access token expires.

## Overview

When a user completes OAuth login via `/oauth2/callback/{provider}`, the service returns:
- **Access Token** (JWT) - Short-lived token for API authentication
- **Refresh Token** - Long-lived token used to obtain new access tokens
- **Expires In** - Access token expiration time in seconds

## Flow

### 1. Initial Login (OAuth Callback)

After OAuth login, the frontend receives:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "dGhpc2lzYXJlZnJlc2h0b2tlbg...",
  "expiresIn": 3600,
  "tokenType": "Bearer",
  "userId": 123,
  "provider": "google",
  "providerUserId": "106097090889524444320"
}
```

**Frontend should:**
- Store both tokens securely (e.g., in memory, httpOnly cookies, or secure storage)
- Track the expiration time (`expiresIn` seconds from now)
- Use the `accessToken` for API requests

### 2. Using Access Token

Include the access token in API requests:

```javascript
// Example: Making an authenticated API request
fetch('https://api.example.com/protected-endpoint', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
})
```

### 3. Refreshing the Access Token

When the access token expires (or is about to expire), use the refresh token to get a new one.

#### Endpoint

```
POST /refresh
```

#### Request Format

The refresh token can be sent in two ways:

**Option 1: As Bearer Token (Recommended)**
```javascript
fetch('https://api.example.com/refresh', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${refreshToken}`
  }
})
```

**Option 2: In Request Body**
```javascript
fetch('https://api.example.com/refresh', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    refreshToken: refreshToken
  })
})
```

#### Response

On success (200 OK):
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "new_refresh_token_here...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

On error (400/401):
```json
{
  "error": "invalid_grant",
  "error_description": "refresh token not found" // or "refresh token revoked"
}
```

### 4. Frontend Implementation Example

Here's a complete example using JavaScript/TypeScript:

```typescript
interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
  userId: number;
  provider: string;
  providerUserId: string;
}

interface RefreshResponse {
  access_token: string;
  refresh_token: string;
  token_type: string;
  expires_in: number;
}

class AuthService {
  private accessToken: string | null = null;
  private refreshToken: string | null = null;
  private expiresAt: number | null = null;

  // Store tokens after OAuth login
  setTokens(response: TokenResponse) {
    this.accessToken = response.accessToken;
    this.refreshToken = response.refreshToken;
    this.expiresAt = Date.now() + (response.expiresIn * 1000);
    
    // Optionally store in localStorage/sessionStorage
    localStorage.setItem('accessToken', response.accessToken);
    localStorage.setItem('refreshToken', response.refreshToken);
    localStorage.setItem('expiresAt', this.expiresAt.toString());
  }

  // Get current access token, refresh if needed
  async getAccessToken(): Promise<string> {
    // Check if token is expired or about to expire (within 5 minutes)
    if (!this.accessToken || !this.expiresAt || Date.now() >= this.expiresAt - 300000) {
      await this.refreshAccessToken();
    }
    return this.accessToken!;
  }

  // Refresh the access token
  async refreshAccessToken(): Promise<void> {
    if (!this.refreshToken) {
      throw new Error('No refresh token available');
    }

    try {
      const response = await fetch('https://api.example.com/refresh', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.refreshToken}`
        }
      });

      if (!response.ok) {
        throw new Error('Failed to refresh token');
      }

      const data: RefreshResponse = await response.json();
      
      this.accessToken = data.access_token;
      this.refreshToken = data.refresh_token;
      this.expiresAt = Date.now() + (data.expires_in * 1000);

      // Update stored tokens
      localStorage.setItem('accessToken', data.access_token);
      localStorage.setItem('refreshToken', data.refresh_token);
      localStorage.setItem('expiresAt', this.expiresAt.toString());
    } catch (error) {
      // Refresh token is invalid/revoked - user needs to login again
      this.clearTokens();
      throw error;
    }
  }

  // Make authenticated API request with automatic token refresh
  async authenticatedFetch(url: string, options: RequestInit = {}): Promise<Response> {
    const token = await this.getAccessToken();
    
    const headers = {
      ...options.headers,
      'Authorization': `Bearer ${token}`
    };

    let response = await fetch(url, { ...options, headers });

    // If 401, try refreshing token once
    if (response.status === 401) {
      await this.refreshAccessToken();
      const newToken = await this.getAccessToken();
      headers['Authorization'] = `Bearer ${newToken}`;
      response = await fetch(url, { ...options, headers });
    }

    return response;
  }

  clearTokens() {
    this.accessToken = null;
    this.refreshToken = null;
    this.expiresAt = null;
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('expiresAt');
  }
}

// Usage
const authService = new AuthService();

// After OAuth login
authService.setTokens(oauthResponse);

// Make authenticated requests
const response = await authService.authenticatedFetch('https://api.example.com/users/me');
```

### 5. React Hook Example

```typescript
import { useState, useEffect, useCallback } from 'react';

interface UseAuthReturn {
  accessToken: string | null;
  refreshToken: () => Promise<void>;
  isAuthenticated: boolean;
}

function useAuth(): UseAuthReturn {
  const [accessToken, setAccessToken] = useState<string | null>(
    localStorage.getItem('accessToken')
  );
  const [refreshTokenValue, setRefreshTokenValue] = useState<string | null>(
    localStorage.getItem('refreshToken')
  );

  const refreshToken = useCallback(async () => {
    if (!refreshTokenValue) {
      throw new Error('No refresh token available');
    }

    const response = await fetch('/refresh', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${refreshTokenValue}`
      }
    });

    if (!response.ok) {
      // Token is invalid - clear and redirect to login
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      setAccessToken(null);
      setRefreshTokenValue(null);
      throw new Error('Failed to refresh token');
    }

    const data = await response.json();
    setAccessToken(data.access_token);
    setRefreshTokenValue(data.refresh_token);
    localStorage.setItem('accessToken', data.access_token);
    localStorage.setItem('refreshToken', data.refresh_token);
  }, [refreshTokenValue]);

  // Auto-refresh token before it expires
  useEffect(() => {
    if (!accessToken || !refreshTokenValue) return;

    const checkAndRefresh = async () => {
      // Check token expiration (assuming JWT - decode to get exp)
      // Or use expiresAt from initial response
      try {
        await refreshToken();
      } catch (error) {
        console.error('Failed to refresh token:', error);
      }
    };

    // Check every 5 minutes
    const interval = setInterval(checkAndRefresh, 5 * 60 * 1000);
    return () => clearInterval(interval);
  }, [accessToken, refreshTokenValue, refreshToken]);

  return {
    accessToken,
    refreshToken,
    isAuthenticated: !!accessToken
  };
}
```

## Security Best Practices

1. **Storage**: 
   - Prefer httpOnly cookies for refresh tokens (most secure)
   - If using localStorage, ensure HTTPS only
   - Consider using secure storage APIs (e.g., Keychain on iOS, Keystore on Android)

2. **Token Rotation**: 
   - The service may issue a new refresh token on each refresh
   - Always update the stored refresh token

3. **Error Handling**:
   - If refresh fails, clear tokens and redirect to login
   - Don't retry indefinitely on refresh failures

4. **Expiration Handling**:
   - Refresh tokens before they expire (e.g., 5 minutes before)
   - Handle token expiration gracefully

5. **HTTPS Only**:
   - Always use HTTPS in production
   - Never send tokens over unencrypted connections

## Error Scenarios

### Refresh Token Not Found
```json
{
  "error": "invalid_grant",
  "error_description": "refresh token not found"
}
```
**Action**: Clear tokens and redirect user to login

### Refresh Token Revoked
```json
{
  "error": "invalid_grant",
  "error_description": "refresh token revoked"
}
```
**Action**: Clear tokens and redirect user to login (token was explicitly revoked)

### Access Token Expired (401 Unauthorized)
**Action**: Automatically attempt refresh, then retry the request

## Notes

- The refresh endpoint is provided by Micronaut Security framework
- Refresh tokens are stored in the database and can be revoked
- Each refresh may return a new refresh token (token rotation)
- Access tokens are JWT tokens that can be decoded (but not modified) on the client side



