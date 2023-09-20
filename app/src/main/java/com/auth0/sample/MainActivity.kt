package com.auth0.sample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
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
import com.auth0.sample.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var account: Auth0
    private lateinit var binding: ActivityMainBinding
    private var cachedCredentials: Credentials? = null
    private var cachedUserProfile: UserProfile? = null

    /** local storage for user, required params : */
    /*private var localApiClient: AuthenticationAPIClient? = null
    private var localCredentialsManager: CredentialsManager? = null*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the account object with the Auth0 application details
        account = Auth0(
            getString(R.string.com_auth0_client_id),
            getString(R.string.com_auth0_domain)
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.buttonLogin.setOnClickListener { loginWithBrowser() }
        binding.buttonLogout.setOnClickListener { Auth0CachingHelper.logout(this, account) { isLoggedOut, message ->
            if (isLoggedOut) {
                // logout user
                updateUI()
            } else {
                // show error:
                Log.e(TAG, "Error: $message")
            }
        } }

        Auth0CachingHelper.initLocalAuth0Repository(this, account)
        Auth0CachingHelper.handleUserAuth()
        updateUI()
    }

    private fun updateUI() {

        Log.w(TAG, "updateUI: ${Auth0CachingHelper.isAuthenticated()}")
        binding.buttonLogout.isEnabled = Auth0CachingHelper.isAuthenticated() == true
        binding.buttonLogin.isEnabled = Auth0CachingHelper.isAuthenticated() == false
        binding.userProfile.isVisible = Auth0CachingHelper.isAuthenticated() == true
        binding.textViewWelcome.text = if (Auth0CachingHelper.isAuthenticated()) {
            "Welcome"
        } else {
            "Log in using the Browser"
        }

        Auth0CachingHelper.getUserProfile(account = account) { userProfile, message ->
            Log.i(TAG, "updateUI: $message")
            cachedUserProfile = userProfile
            ("Name: ${cachedUserProfile?.name ?: ""}\n" +
                    "Email: ${cachedUserProfile?.email ?: ""}\n" +
                    "Nick Name: ${cachedUserProfile?.nickname ?: ""}\n").also {
                binding.userProfile.text = it
            }
        }
    }

    private fun loginWithBrowser() {
        // Setup the WebAuthProvider, using the custom scheme and scope.
        WebAuthProvider.login(account)
            .withScheme(getString(R.string.com_auth0_scheme))
            .withScope("openid profile email read:current_user update:current_user_metadata")
            .withAudience("https://${getString(R.string.com_auth0_domain)}/api/v2/")

            // Launch the authentication passing the callback where the results will be received
            .start(this, object : Callback<Credentials, AuthenticationException> {
                override fun onFailure(exception: AuthenticationException) {
                    Log.e(TAG, "onFailure: ${exception.getCode()} ${exception.message}")
                    showSnackBar("Failure: ${exception.getCode()}")
                }

                override fun onSuccess(credentials: Credentials) {
                    /*if (localCredentialsManager != null) {
                        localCredentialsManager?.saveCredentials(credentials)
                    }*/

                    // saving the user
                    Auth0CachingHelper.saveAuth0UserInLocal(credentials)

                    cachedCredentials = credentials
                    showSnackBar("Success: ${credentials.accessToken}")
                    showUserProfile()
                    updateUI()
                }
            })
    }

    /*private fun logout() {
        WebAuthProvider.logout(account)
            .withScheme(getString(R.string.com_auth0_scheme))
            .start(this, object : Callback<Void?, AuthenticationException> {
                override fun onSuccess(payload: Void?) {
                    // The user has been logged out!
                    cachedCredentials = null
                    cachedUserProfile = null
                    //localCredentialsManager?.clearCredentials()
                    updateUI()
                }

                override fun onFailure(exception: AuthenticationException) {
                    updateUI()
                    showSnackBar("Failure: ${exception.getCode()}")
                }
            })
    }*/

    private fun showUserProfile() {
        val client = AuthenticationAPIClient(account)
        // Use the access token to call userInfo endpoint.
        // In this sample, we can assume cachedCredentials has been initialized by this point.
        client.userInfo(cachedCredentials!!.accessToken!!)
            .start(object : Callback<UserProfile, AuthenticationException> {
                override fun onFailure(error: AuthenticationException) {
                    showSnackBar("Failure: ${error.getCode()}")
                    Log.e(TAG, "onFailureShowUserProfile: ${error.message}")
                }

                override fun onSuccess(result: UserProfile) {
                    cachedUserProfile = result
                    updateUI()
                    //extractDataFromExtraInfoMap(result)
                }
            })
    }

    private fun extractDataFromExtraInfoMap(result: UserProfile) {
        val userMap = result.getExtraInfo()

        // this model is to be used in calling user registration API in givhero app.
        val cachedUserFromAuth0 = CachedUserProfileModel(
            userName = "${result.name}",
            userNickName = "${result.nickname}",
            userImageUrl = "${result.pictureURL}",
            userEmail = "${result.email}",
            userAuth0UserId = "${userMap.getValue(MappingKeys.AUTH0_USER_ID)}",
            userCompany = "${userMap.getValue(MappingKeys.AUTH0_COMPANY_NAME)}",
            userDepartment = "${userMap.getValue(MappingKeys.AUTH0_DEPARTMENT)}",
            userEmployeeId = "${userMap.getValue(MappingKeys.AUTH0_EMPLOYEE_ID)}",
            userLocation = "${userMap.getValue(MappingKeys.AUTH0_LOCATION)}",
            userCity = "${userMap.getValue(MappingKeys.AUTH0_CITY_NAME)}",
            userCountry = "${userMap.getValue(MappingKeys.AUTH0_COUNTRY_NAME)}",
        )

        Log.e(TAG, "extractDataFromExtraInfoMap: ${cachedUserFromAuth0.userCompany}")
    }

    private fun showSnackBar(text: String) {
        Snackbar.make(
            binding.root,
            text,
            Snackbar.LENGTH_LONG
        ).show()
    }
}