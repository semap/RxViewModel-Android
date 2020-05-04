/*
 * Copyright 2019, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.navigationadvancedsample.listscreen

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import com.example.android.navigationadvancedsample.R
import com.example.android.navigationadvancedsample.databinding.FragmentUserProfileBinding
import semap.rx.viewmodel.observe

/**
 * Shows a profile screen for a user, taking the name from the arguments.
 */
class UserDetailsFragment : Fragment(R.layout.fragment_user_profile) {
    private val viewModel: UsersViewModel by navGraphViewModels(R.id.list)
    private lateinit var binding: FragmentUserProfileBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.binding = FragmentUserProfileBinding.bind(view)
        bindViewModelToView()
    }

    private fun bindViewModelToView() {
        viewModel.selectedUser.observe(viewLifecycleOwner) {
            renderUser(it)
        }
    }

    private fun renderUser(user: User) {
        binding.apply {
            profilePic.setImageResource(user.avatar)
            profileUserName.text = user.name
            includedLayout.rankTextView.text = user.rank.toString()
            includedLayout.winTextView.text = user.win.toString()
        }
    }
}
