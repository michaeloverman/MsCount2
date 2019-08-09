/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.database;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import tech.michaeloverman.mscount.R;
import timber.log.Timber;

/**
 * This fragment gets the complete list of composer names from Firebase database, and
 * lists them. When one is selected, the name is returned to the PreprogrammedMetronomeFragment
 * for piece selection. The ComposerCallback interface is defined here, for implementation
 * by PreprogrammedMetronomeFragment, in order to communicate the selection back.
 *
 * Created by Michael on 2/26/2017.
 */

public class ComposerSelectFragment extends DatabaseAccessFragment {

//    private static final int NO_DATA_ERROR_CODE = 42;

    @BindView(R.id.composer_recycler_view)
    RecyclerView mRecyclerView;
    @BindView(R.id.composer_select_progress_bar) ProgressBar mProgressBar;
    @BindView(R.id.empty_data_view) TextView mErrorView;
    private ComposerListAdapter mAdapter;
    private LoadNewProgramActivity mActivity;

    public static Fragment newInstance() {
        Timber.d("newInstance()");
        return new ComposerSelectFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("onCreate()");

        mActivity = (LoadNewProgramActivity) getActivity();
        Timber.d("useFirebase = %s", mActivity.useFirebase);

        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Timber.d("onCreateView()");
        View view = inflater.inflate(R.layout.select_composer_layout, container, false);
        ButterKnife.bind(this, view);

        mActivity.setTitle(getString(R.string.select_a_composer));

        LinearLayoutManager manager = new LinearLayoutManager(mActivity);
        mRecyclerView.setLayoutManager(manager);
        mAdapter = new ComposerListAdapter(this.getContext());
        mRecyclerView.setAdapter(mAdapter);

        loadComposers();

        return view;
    }

    /**
     * Contact Firebase Database, get all the composer's names, attach to adapter for
     * recycler viewing
     */
    private void loadComposers() {
        Timber.d("loadComposers()");
        progressSpinner(true);

        FirebaseDatabase.getInstance().getReference().child("composers")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                        Iterable<DataSnapshot> iterable = dataSnapshot.getChildren();
                        List<String> list = new ArrayList<>();
                        for (DataSnapshot snap : iterable) {
                            list.add(snap.getKey());
                        }
                        Collections.sort(list);
                        mAdapter.setComposers(list);
                        progressSpinner(false);
                    }

                    @Override
                    public void onCancelled(@NotNull DatabaseError databaseError) {

                    }
                });

    }

    @Override
    public void updateData() {
        Timber.d("updateData()");
        loadComposers();
    }

    /**
     * Adapter class to handle recycler view, listing composer names
     */
    class ComposerListAdapter extends RecyclerView.Adapter<ComposerListAdapter.ComposerViewHolder> {

        final Context mContext;
        private List<String> composers;

        public ComposerListAdapter(Context context) {
//            Timber.d("ComposerListAdapter constructor");
            mContext = context;
        }
        @NotNull
        @Override
        public ComposerViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
//            Timber.d("onCreateViewHolder");
            View item = LayoutInflater.from(mContext).inflate(R.layout.list_item_composer, parent, false);
            return new ComposerViewHolder(item);
        }

        @Override
        public void onBindViewHolder(ComposerViewHolder holder, int position) {
//            Timber.d("onBindViewHolder()");
            holder.composerName.setText(composers.get(position));
        }

        @Override
        public int getItemCount() {
            return composers == null ? 0 : composers.size();
        }

        public void setComposers(List<String> list) {
            composers = list;
            notifyDataSetChanged();
        }

        class ComposerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            @BindView(R.id.composer_name_tv)
            TextView composerName;

            public ComposerViewHolder(View itemView) {
                super(itemView);
//                Timber.d("ComposerViewHolder constructor");
                ButterKnife.bind(this, itemView);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                int position = getAdapterPosition();

                // send selected composer name to PreprogrammedMetronomeFragment
                mActivity.mCurrentComposer = composers.get(position);

                // close this fragment and return
                getFragmentManager().popBackStackImmediate();
            }
        }
    }

    private void progressSpinner(boolean on) {
        if(on) {
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }
}
