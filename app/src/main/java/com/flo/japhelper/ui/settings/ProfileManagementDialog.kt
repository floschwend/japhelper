/*
 Copyright (c) 2024 Florian Schwendener <naturalnesscheck@gmail.com>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.flo.japhelper.ui.settings

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.flo.japhelper.R
import com.flo.japhelper.utils.SharedPrefsHelper

class ProfileManagementDialog(
    private val sharedPrefsHelper: SharedPrefsHelper,
    private val onProfileChanged: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val profiles = sharedPrefsHelper.getProfiles()
        val currentProfileId = sharedPrefsHelper.getCurrentProfileId()

        val profileNames = profiles.map { profile ->
            val indicator = if (profile.id == currentProfileId) "● " else "○ "
            "$indicator${profile.name}"
        }.toTypedArray()

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, profileNames)
        val listView = ListView(requireContext())
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedProfile = profiles[position]
            sharedPrefsHelper.setCurrentProfileId(selectedProfile.id)
            onProfileChanged()
            dismiss()
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            val selectedProfile = profiles[position]
            showProfileOptionsDialog(selectedProfile)
            true
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Select Profile")
            .setView(listView)
            .setPositiveButton("New Profile") { _, _ ->
                showCreateProfileDialog()
            }
            .setNegativeButton("Close", null)
            .create()
    }

    private fun showProfileOptionsDialog(profile: ApiProfile) {
        val options = arrayOf("Rename", "Duplicate", "Delete")

        AlertDialog.Builder(requireContext())
            .setTitle("Profile: ${profile.name}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameProfileDialog(profile)
                    1 -> duplicateProfile(profile)
                    2 -> showDeleteConfirmation(profile)
                }
            }
            .show()
    }

    private fun showCreateProfileDialog() {
        val input = EditText(requireContext())
        input.hint = "Profile name"

        AlertDialog.Builder(requireContext())
            .setTitle("Create New Profile")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val currentProfile = sharedPrefsHelper.getCurrentProfileWithApiKey()
                    val newProfile = currentProfile.copy(
                        id = "",
                        name = name
                    )
                    val createdProfile = sharedPrefsHelper.addProfile(newProfile)
                    sharedPrefsHelper.setCurrentProfileId(createdProfile.id)
                    onProfileChanged()
                    dismiss()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRenameProfileDialog(profile: ApiProfile) {
        val input = EditText(requireContext())
        input.setText(profile.name)
        input.hint = "Profile name"

        AlertDialog.Builder(requireContext())
            .setTitle("Rename Profile")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val updatedProfile = sharedPrefsHelper.getCurrentProfileWithApiKey()
                        .copy(id = profile.id, name = newName)
                    sharedPrefsHelper.updateProfile(updatedProfile)
                    onProfileChanged()
                    dismiss()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun duplicateProfile(profile: ApiProfile) {
        val duplicatedProfile = sharedPrefsHelper.getCurrentProfileWithApiKey()
            .copy(id = "", name = "${profile.name} (Copy)")
        val createdProfile = sharedPrefsHelper.addProfile(duplicatedProfile)
        sharedPrefsHelper.setCurrentProfileId(createdProfile.id)
        onProfileChanged()
        dismiss()
    }

    private fun showDeleteConfirmation(profile: ApiProfile) {
        val profiles = sharedPrefsHelper.getProfiles()
        if (profiles.size <= 1) {
            AlertDialog.Builder(requireContext())
                .setTitle("Cannot Delete")
                .setMessage("You must have at least one profile.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Delete Profile")
            .setMessage("Are you sure you want to delete \"${profile.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                sharedPrefsHelper.deleteProfile(profile.id)
                onProfileChanged()
                dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}