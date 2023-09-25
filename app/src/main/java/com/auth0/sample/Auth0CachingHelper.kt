package com.auth0.sample

import android.content.Context
import android.util.Log
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.authentication.storage.CredentialsManager
import com.auth0.android.authentication.storage.CredentialsManagerException
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.auth0.android.result.UserProfile

private const val TAG = "Auth0CachingHelper"
object Auth0CachingHelper {

    /** flow: remote login >> save cred >> use cred to get profile etc >> logout & remove cred.
     * [initLocalAuth0Repository] needs to be called before actual SSO login, can be called in splash screen.
     * [saveAuth0UserInLocal] For storing a user in local storage.
     * [isAuthenticated] checks for user authentication, returns true/false
     * [handleUserAuth] called everytime user opens the app
     * [reAuthenticateUserSilently] is an internal function to get credentials.
     * [logout] simply logs out user with callbacks, true -> logout, false -> error occurred
     * [getUserProfile] simply fetches user profile & returns the same
     * */

    private var localApiClient: AuthenticationAPIClient? = null
    private var localCredentialsManager: CredentialsManager? = null
    private var cachedCredentials: Credentials? = null
    private var cachedUserProfile: UserProfile? = null

    fun initLocalAuth0Repository(context: Context, account: Auth0) {
        localApiClient = AuthenticationAPIClient(auth0 = account)
        localCredentialsManager = CredentialsManager(
            authenticationClient = localApiClient ?: AuthenticationAPIClient(account),
            storage = SharedPreferencesStorage(context)
        )
    }

    fun saveAuth0UserInLocal(credentials: Credentials) {
        if (localCredentialsManager != null) {
            localCredentialsManager?.saveCredentials(credentials)
        }
    }

    fun isAuthenticated(): Boolean {
        return localCredentialsManager?.hasValidCredentials() == true
    }

    fun handleUserAuth() {
        if (isAuthenticated()) {
            // normal flow, no need to authenticate
        } else {
            // token expired, re-authenticating user again silently
            reAuthenticateUserSilently()
        }
    }

    private fun reAuthenticateUserSilently() {
        if (localCredentialsManager == null) {
            Log.e(TAG, "localCredentialsManager is Null, function initLocalAuth0Repository(context, auth0) isn't called.")
            return
        }
        if (!isAuthenticated()) {
            Log.e(TAG, "Cached user doesn't have valid credentials. Request for Login again.")
            return
        }
        localCredentialsManager?.getCredentials(object :
            Callback<Credentials, CredentialsManagerException> {
            override fun onSuccess(credential: Credentials) {
                // Use credentials
                cachedCredentials = credential
                Log.e(TAG, "onSuccess: checking if authenticated = ${localCredentialsManager?.hasValidCredentials()} ${isAuthenticated()}")
            }

            override fun onFailure(error: CredentialsManagerException) {
                // No credentials were previously saved or they couldn't be refreshed
                Log.e(TAG, "onFailure: ${error.message}")
            }
        })

        /**
         * If the accessToken has expired, the [localCredentialsManager] automatically uses the refreshToken and renews the credentials for you. New credentials will be stored for future access.
         * [doc_link] : https://auth0.com/docs/libraries/auth0-android/auth0-android-save-and-renew-tokens
         * */
    }

    fun logout(context: Context, account: Auth0, callback: (isLoggedOut: Boolean, message: String) -> Unit) {
        WebAuthProvider.logout(account)
            .withScheme(context.getString(R.string.com_auth0_scheme))
            .start(context, object : Callback<Void?, AuthenticationException> {
                override fun onSuccess(payload: Void?) {
                    // The user has been logged out!
                    cachedCredentials = null
                    cachedUserProfile = null
                    localCredentialsManager?.clearCredentials()
                    callback(true, "Logging out...")
                }

                override fun onFailure(exception: AuthenticationException) {
                    Log.e(TAG, "onFailureLogout: ${exception.message}")
                    callback(false, "${exception.message}")
                }
            })
    }

    fun getUserProfile(account: Auth0, callback: (userProfile: UserProfile?, message: String) -> Unit) {
        if (localCredentialsManager == null) {
            Log.e(TAG, "localCredentialsManager is Null, function initLocalAuth0Repository(context, auth0) isn't called.")
            return
        }

        localCredentialsManager?.getCredentials(object :
            Callback<Credentials, CredentialsManagerException> {
            override fun onFailure(error: CredentialsManagerException) {
                Log.e(TAG, "getCredentialsOnFailure: ${error.message}")
                callback(null, "${error.message}")
            }

            override fun onSuccess(result: Credentials) {
                cachedCredentials = result
                AuthenticationAPIClient(account).userInfo(cachedCredentials?.accessToken ?: "")
                    .start(object : Callback<UserProfile, AuthenticationException> {
                        override fun onFailure(error: AuthenticationException) {
                            Log.e(TAG, "userInfoOnFailure: ${error.message}")
                            callback(null, "${error.message}")
                        }

                        override fun onSuccess(result: UserProfile) {
                            Log.i(TAG, "onSuccess: UserProfile = ${result.getExtraInfo()}\n")
                            Log.i(TAG, "onSuccess: UserProfile ID = ${result.getId()}\n")
                            Log.i(TAG, "onSuccess: AccessToken = ${cachedCredentials?.accessToken}\n")
                            Log.i(TAG, "onSuccess: Expire time = ${cachedCredentials?.expiresAt}\n Refresh token= ${cachedCredentials?.refreshToken}\n Id token= ${cachedCredentials?.idToken}\n")
                            cachedUserProfile = result
                            callback(cachedUserProfile, "Profile fetched successfully.")
                        }
                    })
            }
        })
    }

}