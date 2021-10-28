/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.pojos

import timber.log.Timber

/**
 * Very simple POJO for pairing a program title and its Firebase key. Used primarily by
 * the Favorites database.
 *
 * Created by Michael on 2/25/2017.
 */
class TitleKeyObject(title: String, key: String) : Comparable<Any?> {
    var title: String
    var key: String

    init {
        Timber.d("TitleKeyObject constructor(): $title, $key")
        this.title = title
        this.key = key
    }

    override fun compareTo(other: Any?): Int {
        val other1 = other as TitleKeyObject
        return title.compareTo(other1.title)
    }
}