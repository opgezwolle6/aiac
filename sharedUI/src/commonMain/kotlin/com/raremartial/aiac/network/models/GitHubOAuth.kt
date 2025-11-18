package com.raremartial.aiac.network.models

import kotlinx.serialization.Serializable

/**
 * Модели для OAuth 2.1 с PKCE аутентификации GitHub MCP Server
 */

/**
 * Запрос на получение authorization code
 */
@Serializable
data class OAuthAuthorizationRequest(
    val client_id: String,
    val redirect_uri: String,
    val scope: String,
    val response_type: String = "code",
    val code_challenge: String,
    val code_challenge_method: String = "S256",
    val state: String? = null
)

/**
 * Ответ с authorization code
 */
@Serializable
data class OAuthAuthorizationResponse(
    val code: String,
    val state: String? = null
)

/**
 * Запрос на обмен authorization code на access token
 */
@Serializable
data class OAuthTokenRequest(
    val grant_type: String = "authorization_code",
    val code: String,
    val redirect_uri: String,
    val client_id: String,
    val code_verifier: String
)

/**
 * Ответ с access token
 */
@Serializable
data class OAuthTokenResponse(
    val access_token: String,
    val token_type: String = "Bearer",
    val expires_in: Int? = null,
    val refresh_token: String? = null,
    val scope: String? = null
)

/**
 * Ошибка OAuth
 */
@Serializable
data class OAuthError(
    val error: String,
    val error_description: String? = null,
    val error_uri: String? = null
)

