package com.auth0.sample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the account object with the Auth0 application details
        account = Auth0(
            getString(R.string.com_auth0_client_id),
            getString(R.string.com_auth0_domain)
        )

        // Bind the button click with the login action
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.buttonLogin.setOnClickListener { loginWithBrowser() }
        binding.buttonLogout.setOnClickListener { logout() }
    }

    private fun updateUI() {
        binding.buttonLogout.isEnabled = cachedCredentials != null
        binding.buttonLogin.isEnabled = cachedCredentials == null
        binding.userProfile.isVisible = cachedCredentials != null

        ("Name: ${cachedUserProfile?.name ?: ""}\n" +
                "Email: ${cachedUserProfile?.email ?: ""}\n" +
                "Nick Name: ${cachedUserProfile?.nickname ?: ""}\n" +
                "EmpId: ${cachedUserProfile?.getExtraInfo()?.getValue(MappingKeys.AUTH0_EMPLOYEE_ID) ?: "*NA*"}\n" +
                "City: ${cachedUserProfile?.getExtraInfo()?.getValue(MappingKeys.AUTH0_CITY_NAME) ?: "*NA*"}\n" +
                "Country: ${cachedUserProfile?.getExtraInfo()?.getValue(MappingKeys.AUTH0_COUNTRY_NAME) ?: "*NA*"}\n" +
                "Location: ${cachedUserProfile?.getExtraInfo()?.getValue(MappingKeys.AUTH0_LOCATION) ?: "*NA*"}\n" +
                "Dept.: ${cachedUserProfile?.getExtraInfo()?.getValue(MappingKeys.AUTH0_DEPARTMENT) ?: "*NA*"}\n" +
                "Company: ${cachedUserProfile?.getExtraInfo()?.getValue(MappingKeys.AUTH0_COMPANY_NAME) ?: "*NA*"}\n" +
                "Auth0UserId: ${cachedUserProfile?.getExtraInfo()?.getValue(MappingKeys.AUTH0_USER_ID) ?: "*NA*"}\n" +
                "Updated At: ${cachedUserProfile?.getExtraInfo()?.getValue(MappingKeys.UPDATED_AT) ?: "*NA*"}\n"
                ).also { binding.userProfile.text = it }
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
                    cachedCredentials = credentials
                    showSnackBar("Success: ${credentials.accessToken}")
                    Log.e(TAG, "onSuccess: accessToken = \n${credentials.accessToken}")
                    updateUI()
                    showUserProfile()
                }
            })
    }

    private fun logout() {
        WebAuthProvider.logout(account)
            .withScheme(getString(R.string.com_auth0_scheme))
            .start(this, object : Callback<Void?, AuthenticationException> {
                override fun onSuccess(payload: Void?) {
                    // The user has been logged out!
                    cachedCredentials = null
                    cachedUserProfile = null
                    updateUI()
                }

                override fun onFailure(exception: AuthenticationException) {
                    updateUI()
                    showSnackBar("Failure: ${exception.getCode()}")
                }
            })
    }

    private fun showUserProfile() {
        val client = AuthenticationAPIClient(account)
        // Use the access token to call userInfo endpoint.
        // In this sample, we can assume cachedCredentials has been initialized by this point.
        client.userInfo(cachedCredentials!!.accessToken!!)
            .start(object : Callback<UserProfile, AuthenticationException> {
                override fun onFailure(error: AuthenticationException) {
                    showSnackBar("Failure: ${error.getCode()}")
                }

                override fun onSuccess(result: UserProfile) {
                    cachedUserProfile = result
                    updateUI()
                    extractDataFromExtraInfoMap(result)
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