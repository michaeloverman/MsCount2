/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.favorites

import android.net.Uri
import android.provider.BaseColumns

/**
 * SQLite Database contract for storing movies marked as favorites.
 * Created by Michael on 12/20/2016.
 */
object FavoritesContract {
    const val CONTENT_AUTHORITY = "tech.michaeloverman.android.mscount"
    private val BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY)
    const val PATH_FAVORITES = "favorites"

    object FavoriteEntry : BaseColumns {
        val CONTENT_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(PATH_FAVORITES)
                .build()
        const val _ID = "_id"
        const val TABLE_NAME = "favorites"
        const val COLUMN_PIECE_ID = "favorite_firebase_id"
        const val COLUMN_PIECE_TITLE = "favorite_title"
        //        public static Uri buildMovieUriWithId(int id) {
        //            return CONTENT_URI.buildUpon()
        //                    .appendPath(Integer.toString(id))
        //                    .build();
        //        }
    }
}