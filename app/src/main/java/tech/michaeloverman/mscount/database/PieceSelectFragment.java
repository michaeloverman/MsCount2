/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.database;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import tech.michaeloverman.mscount.R;
import tech.michaeloverman.mscount.pojos.TitleKeyObject;
import timber.log.Timber;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PieceSelectFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 * Handles display and click handling of programs in the database.
 */
public class PieceSelectFragment extends DatabaseAccessFragment
        implements WorksListAdapter.WorksListAdapterOnClickHandler,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int ID_PROGRAM_LOADER = 432;

    @BindView(R.id.piece_list_recycler_view) RecyclerView mRecyclerView;
    @BindView(R.id.composers_name_label) TextView mComposersNameView;
    @BindView(R.id.local_database_label) TextView mLocalDatabaseView;
    @BindView(R.id.error_view) TextView mErrorView;
    @BindView(R.id.program_select_progress_bar) ProgressBar mProgressSpinner;
    @BindView(R.id.select_composer_button) Button mSelectComposerButton;

    private MenuItem mDeleteCancelMenuItem;

    private String mCurrentComposer;
    private WorksListAdapter mAdapter;
    private boolean mDeleteFlag;
    private LoadNewProgramActivity mActivity;

    public static Fragment newInstance() {
        return new PieceSelectFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        mActivity = (LoadNewProgramActivity) getActivity();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.program_select_fragment, container, false);
        ButterKnife.bind(this, view);

        Timber.d("onCreateView useFirebase: %s", mActivity.useFirebase);

        LinearLayoutManager manager = new LinearLayoutManager(this.getActivity());
        mRecyclerView.setLayoutManager(manager);
        mAdapter = new WorksListAdapter(getContext(), this);
        mRecyclerView.setAdapter(mAdapter);

        mCurrentComposer = mActivity.mCurrentComposer;
        Timber.d("onCreate() Composer: %s", mCurrentComposer);

        if(mActivity.useFirebase) {
            mActivity.setTitle(getString(R.string.select_piece_by));
        } else {
            mActivity.setTitle(getString(R.string.select_a_piece));
            makeComposerRelatedViewsInvisible();
        }

        if (mActivity.useFirebase && mCurrentComposer == null) {
            selectComposer();
        } else {
            composerSelected();
        }

        Timber.d("Returning completed view....!!!");
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Timber.d("onCreateOptionsMenu");
        inflater.inflate(R.menu.delete_menu_item, menu);
        super.onCreateOptionsMenu(menu, inflater);
        mDeleteCancelMenuItem = menu.findItem(R.id.delete_program_menu_item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.d("Fragment menu option");
        if (item.getItemId() == R.id.delete_program_menu_item) {
            if (!mDeleteFlag) {
                toastDeleteInstructions();
                prepareProgramDelete();
            } else {
                cleanUpProgramDelete();
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void toastDeleteInstructions() {
        Toast.makeText(mActivity, R.string.select_to_delete, Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("AlwaysShowAction")
    private void prepareProgramDelete() {
        mDeleteFlag = true;
        mDeleteCancelMenuItem.setTitle(R.string.cancel_delete);
        mDeleteCancelMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    private void cleanUpProgramDelete() {
        mDeleteFlag = false;
        mDeleteCancelMenuItem.setTitle(R.string.delete_program);
        mDeleteCancelMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        mAdapter.notifyDataSetChanged();
        progressSpinner(false);
    }

    @OnClick( { R.id.select_composer_button, R.id.composers_name_label} )
    public void selectComposer() {

        Fragment fragment = ComposerSelectFragment.newInstance();

//        android.transition.ChangeBounds changeBounds = (android.transition.ChangeBounds) TransitionInflater.from(mActivity).inflateTransition(R.transition.change_bounds);
//        fragment.setSharedElementEnterTransition(changeBounds);
        
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
//                .addSharedElement(mComposersNameView, getString(R.string.transition_composer_name_view))
                .commit();
    }

    @Override
    public void onClick(String pieceId, final String title) {
        Timber.d("ProgramSelect onClick() pieceId: %s", pieceId);
        progressSpinner(true);

        if(!mDeleteFlag) {
            mActivity.setProgramResult(pieceId);
            mActivity.finish();
        } else {
            dialogDeleteConfirmation(pieceId, title);
        }
    }

    private void dialogDeleteConfirmation(final String pieceId, final String title) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(mActivity);
        dialog.setCancelable(false)
                .setTitle(R.string.delete_program_dialog_title)
                .setMessage(getString(R.string.delete_confirmation_question, title))
                .setPositiveButton(android.R.string.yes, (DialogInterface dialogInt, int which) -> {
                    if(mActivity.useFirebase) {
                        checkFirebaseAuthorizationToDelete(pieceId, title);
                    } else {
                        deletePieceFromSql(pieceId, title);
                    }
                })
                .setNegativeButton(R.string.cancel, (DialogInterface dialogInt, int which) -> cleanUpProgramDelete() );
        dialog.create().show();
    }

    private void deletePieceFromSql(String id, String title) {
        Toast.makeText(mActivity, R.string.delete_from_sql_toast, Toast.LENGTH_SHORT).show();
        int idInt;
        try {
            idInt = Integer.parseInt(id);
        } catch (NumberFormatException numE) {
            Timber.d(getString(R.string.incorrect_format_database_id));
            return;
        }
        new DeleteFromSqlTask(idInt, title).execute();
        cleanUpProgramDelete();
    }

    private void checkFirebaseAuthorizationToDelete(final String id, final String title) {

        final String userId = getFirebaseAuthId();

        FirebaseDatabase.getInstance().getReference().child("pieces").child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                        Timber.d("userId: %s", userId);
                        Timber.d("creatorId: %s", dataSnapshot.child("creatorId"));
                        if(dataSnapshot.child("creatorId").getValue().equals(userId)) {
                            completeAuthorizedFirebaseDelete(id, title);
                        } else {
                            Toast.makeText(mActivity, R.string.not_authorized_to_delete_toast,
                                    Toast.LENGTH_SHORT).show();
                            cleanUpProgramDelete();
                        }
                    }

                    @Override
                    public void onCancelled(@NotNull DatabaseError databaseError) {

                    }
                });

    }

    @SuppressWarnings("ConstantConditions")
    private String getFirebaseAuthId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if(auth != null) {
            return auth.getCurrentUser().getUid();
        }
        return null;
    }

    private void completeAuthorizedFirebaseDelete(final String id, final String title) {
        Toast.makeText(mActivity, R.string.delete_from_firebase_toast, Toast.LENGTH_SHORT).show();
        //TODO delete from Firebase....
        // delete from composers
        FirebaseDatabase.getInstance().getReference().child("composers").child(mCurrentComposer)
                .child(title).removeValue( (DatabaseError databaseError, DatabaseReference databaseReference) ->
                Timber.d("%s deleted from cloud database...", title)
        );
        // delete from pieces
        FirebaseDatabase.getInstance().getReference().child("pieces").child(id).removeValue(
            (DatabaseError databaseError, DatabaseReference databaseReference) -> {
                cleanUpProgramDelete();
                selectComposer();
            }
        );
    }

    private void composerSelected() {
        progressSpinner(true);
        Timber.d("composerSelected() - %s", mCurrentComposer);

        if(mActivity.useFirebase) {
            Timber.d("Checking Firebase for composer %s", mCurrentComposer);
            FirebaseDatabase.getInstance().getReference().child("composers").child(mCurrentComposer)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                            Iterable<DataSnapshot> pieceList = dataSnapshot.getChildren();
                            ArrayList<TitleKeyObject> list = new ArrayList<>();
                            for (DataSnapshot snap : pieceList) {
                                list.add(new TitleKeyObject(snap.getKey(), snap.getValue().toString()));
                            }
                            mAdapter.setTitles(list);
                            mComposersNameView.setText(mCurrentComposer);
                            progressSpinner(false);
                        }

                        @Override
                        public void onCancelled(@NotNull DatabaseError databaseError) {

                        }
                    });
        } else {
            Timber.d("Checking SQL for pieces ");
            LoaderManager.getInstance(mActivity).initLoader(ID_PROGRAM_LOADER, null, this);
        }
    }
    
    @NotNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == ID_PROGRAM_LOADER) {
            Uri queryUri = ProgramDatabaseSchema.MetProgram.CONTENT_URI;
            Timber.d("onCreateLoader() queryUri: %s", queryUri);
            String sortOrder = ProgramDatabaseSchema.MetProgram.COLUMN_TITLE + " ASC";
    
            return new CursorLoader(mActivity,
                    queryUri,
                    null,
                    null,
                    null,
                    sortOrder);
        } else {
            throw new RuntimeException("Loader Not Implemented: " + id);
        }
    }

    @Override
    public void onLoadFinished(@NotNull Loader<Cursor> loader, Cursor data) {
        Timber.d("onLoadFinished - cursor data ready");
        progressSpinner(false);
        if(data == null) {
            Timber.d("data == null");
            mErrorView.setVisibility(View.VISIBLE);
            updateEmptyProgramList();
        } else if (data.getCount() == 0) {
            Timber.d("data.getCount() == 0");
            mErrorView.setVisibility(View.VISIBLE);
            updateEmptyProgramList();
        } else {
            mErrorView.setVisibility(View.GONE);
            mAdapter.newCursor(data);
        }
    }

    @Override
    public void onLoaderReset(@NotNull Loader<Cursor> loader) {
        mAdapter.newCursor(null);
    }

    @Override
    public void updateData() {
        Timber.d("updateData()");
        if(mActivity.useFirebase) {
            makeComposerRelatedViewsVisible();
            selectComposer();
        } else {
            mCurrentComposer = null;
            makeComposerRelatedViewsInvisible();
            composerSelected();
        }
    }

    private void makeComposerRelatedViewsVisible() {
        Timber.d("showing views");
        mSelectComposerButton.setVisibility(View.VISIBLE);
        mComposersNameView.setText(mCurrentComposer);
        if(mDeleteCancelMenuItem != null) mDeleteCancelMenuItem.setEnabled(true);
        mLocalDatabaseView.setVisibility(View.INVISIBLE);
        mActivity.setTitle(getString(R.string.select_piece_by));
    }
    private void makeComposerRelatedViewsInvisible() {
        Timber.d("removing views");
        mComposersNameView.setVisibility(View.INVISIBLE);
        mLocalDatabaseView.setVisibility(View.VISIBLE);
        mSelectComposerButton.setVisibility(View.GONE);
        mActivity.setTitle(getString(R.string.select_a_piece));
    }

    private void updateEmptyProgramList() {
        mErrorView.setText(getString(R.string.no_programs_currently_in_database));
        mErrorView.setContentDescription(getString(R.string.no_programs_currently_in_database));
        if(mDeleteCancelMenuItem != null) mDeleteCancelMenuItem.setEnabled(false);
    }

    private void progressSpinner(boolean on) {
        if(on) {
            mComposersNameView.setVisibility(View.INVISIBLE);
            mProgressSpinner.setVisibility(View.VISIBLE);
        } else {
            mComposersNameView.setVisibility(View.VISIBLE);
            mProgressSpinner.setVisibility(View.INVISIBLE);
        }
    }

    class DeleteFromSqlTask extends AsyncTask<Void, Void, Void> {
        private final int _id;
        private final String mTitle;
        private final ProgressDialog dialog = new ProgressDialog(mActivity);

        private DeleteFromSqlTask(int itemId, String title) {
            _id = itemId;
            mTitle = title;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Uri uri = ProgramDatabaseSchema.MetProgram.CONTENT_URI;
            String whereClause = "_id=?";
            String[] args = new String[] { Integer.toString(_id) };
            mActivity.getContentResolver().delete(uri, whereClause, args);

            return null;
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage(getString(R.string.deleting_title, mTitle));
            dialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }
}
