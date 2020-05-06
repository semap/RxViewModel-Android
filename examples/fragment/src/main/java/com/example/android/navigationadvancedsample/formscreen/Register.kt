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

package com.example.android.navigationadvancedsample.formscreen

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.android.navigationadvancedsample.R
import com.example.android.navigationadvancedsample.databinding.FragmentRegisterBinding
import com.example.android.navigationadvancedsample.formscreen.RegisterAction.*
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.textChanges
import semap.rx.viewmodel.execute
import semap.rx.viewmodel.observe
import java.util.concurrent.TimeUnit


/**
 * Shows a register form to showcase UI state persistence. It has a button that goes to [Registered]
 */
class Register : Fragment(R.layout.fragment_register) {
    private lateinit var binding: FragmentRegisterBinding
    private val viewModel: RegisterViewModel by navGraphViewModels(R.id.form)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupNavRules()

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.binding = FragmentRegisterBinding.bind(view)
        bindViewToViewModel()
        bindViewModelToView()
    }

    private fun bindViewToViewModel() {
        this.binding.apply {
            usernameText
                    .textChanges()
                    .skipInitialValue()
                    .map { SetName(it.toString()) }
                    .execute(viewModel, viewLifecycleOwner)

            emailText
                    .textChanges()
                    .skipInitialValue()
                    .map { SetEmail(it.toString()) }
                    .execute(viewModel, viewLifecycleOwner)

            passwordText
                    .textChanges()
                    .skipInitialValue()
                    .map { SetPassword(it.toString()) }
                    .execute(viewModel, viewLifecycleOwner)

            signupBtn
                    .clicks()
                    .throttleFirst(500, TimeUnit.MILLISECONDS)
                    .map { SignUp }
                    .execute(viewModel, viewLifecycleOwner)

        }
    }

    private fun bindViewModelToView() {
        viewModel.loadingIndicatorVisibility.observe(viewLifecycleOwner) {
            binding.loadingIndicator.visibility = it
        }
    }

    private fun setupNavRules() {
        this.viewModel.apply {
            signUpComplete.observe(this@Register) {
                findNavController().navigate(R.id.action_register_to_registered)
            }
        }

    }

}
