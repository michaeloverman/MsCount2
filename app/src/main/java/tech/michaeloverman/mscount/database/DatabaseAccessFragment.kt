/* Copyright (C) 2017 Michael Overman - All Rights Reserved */
package tech.michaeloverman.mscount.database

import androidx.fragment.app.Fragment

/**
 * This is a parent class for the database access, program selection activities/fragments.
 * It allows for actions across the fragments to initiate data updating, in order to maintain
 * accurate presentation of data through addition/deletion of data.
 *
 * Created by Michael on 4/11/2017.
 */
abstract class DatabaseAccessFragment : Fragment() {
    open fun updateData() {}
}