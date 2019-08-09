/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.database;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import tech.michaeloverman.mscount.R;
import tech.michaeloverman.mscount.pojos.TitleKeyObject;
import tech.michaeloverman.mscount.utils.AlphanumComparator;
import timber.log.Timber;

/**
 * Adapter for supplying recycler view with proper data.
 * Created by Michael on 2/25/2017.
 */

public class WorksListAdapter extends RecyclerView.Adapter<WorksListAdapter.WorksViewHolder> {

    private final Context mContext;
    private List<TitleKeyObject> mTitles;
    private final WorksListAdapterOnClickHandler mClickHandler;

    public WorksListAdapter(@NonNull Context context, WorksListAdapterOnClickHandler handler) {
        mContext = context;
//        mTitles = titles;
        mClickHandler = handler;
    }

    interface WorksListAdapterOnClickHandler {
        void onClick(String key, String title);
    }

    @NotNull
    @Override
    public WorksViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
//        Timber.d("onCreateViewHolder()");
        View item = LayoutInflater.from(mContext).inflate(R.layout.list_item_work, parent, false);
        return new WorksViewHolder(item);
    }

    @Override
    public void onBindViewHolder(WorksViewHolder holder, int position) {
//        Timber.d("onBindViewHolder()");
        holder.title.setText(mTitles.get(position).getTitle());
        holder.title.setHorizontallyScrolling(true);
        holder.title.setEllipsize(TextUtils.TruncateAt.MIDDLE);
//        ViewCompat.setTransitionName(holder.title, "titleViewTrans" + position);
    }

    @Override
    public int getItemCount() {
        return mTitles == null ? 0 : mTitles.size();
    }

    public void setTitles(List<TitleKeyObject> titles) {
        mTitles = titles;
        //noinspection unchecked
        Collections.sort(mTitles, new AlphanumComparator());
        Timber.d("setTitles() - %s titles...", mTitles.size());
        notifyDataSetChanged();
    }

    public void newCursor(Cursor data) {
        List<TitleKeyObject> titles = new ArrayList<>();
        if(data == null) {
            Timber.d("Null cursor...");
            mTitles = titles;
            return;
        }

        Timber.d("newCursor() data.getCount() == %s", data.getCount());

        try {
            data.moveToFirst();
            while (!data.isAfterLast()) {
                titles.add(new TitleKeyObject(
                        data.getString(ProgramDatabaseSchema.MetProgram.POSITION_TITLE),
                        data.getString(ProgramDatabaseSchema.MetProgram.POSITION_ID)));
                data.moveToNext();
            }
        } catch (Exception exception) {
            Timber.d("Problem here: check this out...");
        }
        setTitles(titles);
    }

    class WorksViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.work_title)
        TextView title;

        WorksViewHolder(View itemView) {
            super(itemView);
//            Timber.d("WorksViewHolder constructor()");
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Timber.d("WorksViewHolder onClick()");
            int position = getAdapterPosition();
            String key = mTitles.get(position).getKey();
            String title = mTitles.get(position).getTitle();
            mClickHandler.onClick(key, title);
        }
    }
}
