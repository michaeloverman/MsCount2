/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.database;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import tech.michaeloverman.mscount.BuildConfig;
import tech.michaeloverman.mscount.R;
import tech.michaeloverman.mscount.SingleFragmentActivity;
import tech.michaeloverman.mscount.programmed.ProgrammedMetronomeFragment;
import tech.michaeloverman.mscount.utils.PrefUtils;
import timber.log.Timber;

/**
 * This activity manages the fragments which lead a user through program selection and/or
 * deletion.
 *
 * Created by Michael on 3/31/2017.
 */

public class LoadNewProgramActivity extends SingleFragmentActivity {

    public static final String EXTRA_NEW_PROGRAM = "new_program_extra";
    private static final int FIREBASE_SIGN_IN = 451;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    boolean useFirebase;
    String mCurrentComposer;

    @Override
    protected Fragment createFragment() {
        return PieceSelectFragment.newInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        supportPostponeEnterTransition();

        Intent intent = getIntent();
        if(intent.hasExtra(ProgrammedMetronomeFragment.EXTRA_COMPOSER_NAME)) {
            mCurrentComposer = intent.getStringExtra(ProgrammedMetronomeFragment.EXTRA_COMPOSER_NAME);
        } else {
            mCurrentComposer = null;
        }
//        if(intent.hasExtra(ProgrammedMetronomeFragment.EXTRA_USE_FIREBASE)) {
//            useFirebase = intent.getBooleanExtra(ProgrammedMetronomeFragment.EXTRA_USE_FIREBASE, true);
//            Timber.d("useFirebase received from intent: " + useFirebase);
//        }
        useFirebase = PrefUtils.usingFirebase(this);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = (@NonNull FirebaseAuth firebaseAuth) -> {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Timber.d("onAuthStateChanged:signed_in: %s", user.getUid());
//                    useFirebase = true;
                } else {
                    // User is signed out
                    Timber.d("onAuthStateChanged:signed_out");
                    useFirebase = false;
                }
        };

        if(mAuth.getCurrentUser() == null && useFirebase) {
            signInToFirebase();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Timber.d("onStart useFirebase: %s", useFirebase);
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        Timber.d("onStop useFirebase: %s", useFirebase);
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Timber.d("onCreateOptionsMenu()");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.programmed_global_menu, menu);
        MenuItem item = menu.findItem(R.id.firebase_local_database);
        item.setTitle(useFirebase ? R.string.use_local_database : R.string.use_cloud_database);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.d("Activity menu option");
        if (item.getItemId() == R.id.firebase_local_database) {
            useFirebase = !useFirebase;
            PrefUtils.saveFirebaseStatus(this, useFirebase);
            if (useFirebase) {
                item.setTitle(R.string.use_local_database);
                if (mAuth.getCurrentUser() == null) {
                    signInToFirebase();
                }
            } else {
                item.setTitle(R.string.use_cloud_database);
            }
            updateData();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    void setProgramResult(String pieceId) {
        if(pieceId == null) {
            Timber.d("null piece recieved to return");
        } else {
            Timber.d("setting new program on result intent: %s", pieceId);
        }
        Intent data = new Intent();
        data.putExtra(EXTRA_NEW_PROGRAM, pieceId);
        setResult(RESULT_OK, data);
    }

    private void updateData() {
        DatabaseAccessFragment f = (DatabaseAccessFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (!useFirebase && f instanceof ComposerSelectFragment) {
//            Timber.d("popping......");
            f.getFragmentManager().popBackStackImmediate();
        } else {
//            Timber.d("switching to cloud");
            f.updateData();
        }
    }

    private void signInToFirebase() {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(!BuildConfig.DEBUG)
                        .setTheme(R.style.AppTheme)
                        .build(),
                FIREBASE_SIGN_IN);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == FIREBASE_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            // Successfully signed in
            if (resultCode == RESULT_OK) {
                Timber.d("signed into Firebase");
                return;
            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    Toast.makeText(this, R.string.sign_in_cancelled, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (response.getError().getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                    Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            Toast.makeText(this, R.string.unknown_sign_in_response, Toast.LENGTH_SHORT).show();
        }
    }

}
