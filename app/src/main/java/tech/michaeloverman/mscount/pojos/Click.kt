/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.pojos

/**
 * POJO to handle aspects of the sound files for clicks
 * Created by Michael on 10/6/2016.
 */
class Click(val assetPath: String) {
    val name: String
    var soundId: Int = -1

    init {
        val components = assetPath.split("/").toTypedArray()
        val filename = components[components.size - 1]
        name = filename.replace(".ogg", "")
    }
}