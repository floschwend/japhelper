package com.flo.japhelper.context

import android.content.Context

class NaturalContextMenu(
    context: Context,
) : SystemContextMenu(context) {
    override val activityName: String
        get() = "com.flo.japhelper.context.NaturalContextMenuAction"

    companion object {
        fun ensureConsistentStateWithPreferenceStatus(
            context: Context,
            preferenceStatus: Boolean,
        ) {
            NaturalContextMenu(context).ensureConsistentStateWithPreferenceStatus(preferenceStatus)
        }
    }
}