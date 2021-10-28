/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.pojos

import java.io.Serializable

/**
 * POJO for a single piece of data: an integer value and a boolean. Boolean whether it is a barline
 * or not, value is the measure number for barlines, and the number of subdivisions otherwise.
 * Created by Michael on 3/13/2017.
 */
class DataEntry : Serializable {
    var data = 0
    var isBarline = false

    constructor(data: Int, bar: Boolean) {
        this.data = data
        isBarline = bar
    }

    constructor() {}

    override fun toString(): String {
        return "$data;$isBarline"
    }
}