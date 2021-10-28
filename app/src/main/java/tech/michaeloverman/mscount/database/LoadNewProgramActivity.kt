/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.database

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import tech.michaeloverman.mscount.BuildConfig
import tech.michaeloverman.mscount.R
import tech.michaeloverman.mscount.SingleFragmentActivity
import tech.michaeloverman.mscount.programmed.ProgrammedMetronomeFragment
import tech.michaeloverman.mscount.utils.PrefUtils.saveFirebaseStatus
import tech.michaeloverman.mscount.utils.PrefUtils.usingFirebase
import timber.log.Timber

/**
 * This activity manages the fragments which lead a user through program selection and/or
 * deletion.
 *
 * Created by Michael on 3/31/2017.
 */
class LoadNewProgramActivity : SingleFragmentActivity() {
    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: AuthStateListener? = null
    @JvmField
    var useFirebase = false
    @JvmField
    var mCurrentComposer: String? = null
    override fun createFragment(): Fragment? {
        return PieceSelectFragment.newInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        supportPostponeEnterTransition();
        val intent = intent
        mCurrentComposer = if (intent.hasExtra(ProgrammedMetronomeFragment.EXTRA_COMPOSER_NAME)) {
            intent.getStringExtra(ProgrammedMetronomeFragment.EXTRA_COMPOSER_NAME)
        } else {
            null
        }
        //        if(intent.hasExtra(ProgrammedMetronomeFragment.EXTRA_USE_FIREBASE)) {
//            useFirebase = intent.getBooleanExtra(ProgrammedMetronomeFragment.EXTRA_USE_FIREBASE, true);
//            Timber.d("useFirebase received from intent: " + useFirebase);
//        }
        useFirebase = usingFirebase(this)
        mAuth = FirebaseAuth.getInstance()
        mAuthListener = AuthStateListener { firebaseAuth: FirebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // User is signed in
                Timber.d("onAuthStateChanged:signed_in: %s", user.uid)
                //                    useFirebase = true;
            } else {
                // User is signed out
                Timber.d("onAuthStateChanged:signed_out")
                useFirebase = false
            }
        }
        if (mAuth!!.currentUser == null && useFirebase) {
            signInToFirebase()
        }
    }

    public override fun onStart() {
        super.onStart()
        Timber.d("onStart useFirebase: %s", useFirebase)
        mAuth!!.addAuthStateListener(mAuthListener!!)
    }

    public override fun onStop() {
        super.onStop()
        Timber.d("onStop useFirebase: %s", useFirebase)
        if (mAuthListener != null) {
            mAuth!!.removeAuthStateListener(mAuthListener!!)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Timber.d("onCreateOptionsMenu()")
        val inflater = menuInflater
        inflater.inflate(R.menu.programmed_global_menu, menu)
        val item = menu.findItem(R.id.firebase_local_database)
        item.setTitle(if (useFirebase) R.string.use_local_database else R.string.use_cloud_database)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("Activity menu option")
        return if (item.itemId == R.id.firebase_local_database) {
            useFirebase = !useFirebase
            saveFirebaseStatus(this, useFirebase)
            if (useFirebase) {
                item.setTitle(R.string.use_local_database)
                if (mAuth!!.currentUser == null) {
                    signInToFirebase()
                }
            } else {
                item.setTitle(R.string.use_cloud_database)
            }
            updateData()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    fun setProgramResult(pieceId: String?) {
        if (pieceId == null) {
            Timber.d("null piece recieved to return")
        } else {
            Timber.d("setting new program on result intent: %s", pieceId)
        }
        val data = Intent()
        data.putExtra(EXTRA_NEW_PROGRAM, pieceId)
        setResult(RESULT_OK, data)
    }

    private fun updateData() {
        val f = supportFragmentManager.findFragmentById(R.id.fragment_container) as DatabaseAccessFragment?
        if (!useFirebase && f is ComposerSelectFragment) {
//            Timber.d("popping......");
            f.getFragmentManager()!!.popBackStackImmediate()
        } else {
//            Timber.d("switching to cloud");
            f!!.updateData()
        }
    }

    private fun signInToFirebase() {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(!BuildConfig.DEBUG)
                        .setTheme(R.style.AppTheme)
                        .build(),
                FIREBASE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FIREBASE_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            // Successfully signed in
            if (resultCode == RESULT_OK) {
                Timber.d("signed into Firebase")
                return
            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    Toast.makeText(this, R.string.sign_in_cancelled, Toast.LENGTH_SHORT).show()
                    return
                }
                if (response.error!!.errorCode == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show()
                    return
                }
                if (response.error!!.errorCode == ErrorCodes.UNKNOWN_ERROR) {
                    Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_SHORT).show()
                    return
                }
            }
            Toast.makeText(this, R.string.unknown_sign_in_response, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_NEW_PROGRAM = "new_program_extra"
        private const val FIREBASE_SIGN_IN = 451
    }
}