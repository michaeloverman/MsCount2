/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.favorites;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * SQLite Database contract for storing movies marked as favorites.
 * Created by Michael on 12/20/2016.
 */

public class FavoritesContract {

    public static final String CONTENT_AUTHORITY = "tech.michaeloverman.android.mscount";
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_FAVORITES = "favorites";

    private FavoritesContract() {}

    public static final class FavoriteEntry implements BaseColumns {

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_FAVORITES)
                .build();

        public static final String TABLE_NAME = "favorites";

        public static final String COLUMN_PIECE_ID = "favorite_firebase_id";
        public static final String COLUMN_PIECE_TITLE = "favorite_title";

//        public static Uri buildMovieUriWithId(int id) {
//            return CONTENT_URI.buildUpon()
//                    .appendPath(Integer.toString(id))
//                    .build();
//        }

    }
}
