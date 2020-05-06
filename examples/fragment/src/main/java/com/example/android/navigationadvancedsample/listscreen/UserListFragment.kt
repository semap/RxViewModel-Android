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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.android.navigationadvancedsample.R
import com.example.android.navigationadvancedsample.databinding.FragmentLeaderboardBinding
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import semap.rx.viewmodel.asLiveData
import semap.rx.viewmodel.observe

/**
 * Shows a static leaderboard with multiple users.
 */
class UserListFragment : Fragment(R.layout.fragment_leaderboard) {
    private val viewModel: UsersViewModel by navGraphViewModels(R.id.list)
    private lateinit var binding: FragmentLeaderboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupNav()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.binding = FragmentLeaderboardBinding.bind(view)
        bindViewToViewModel()
        bindViewModelToView()

    }

    private fun bindViewToViewModel() {
        this.binding.apply {
            leaderboardList.adapter = MyAdapter {
                viewModel.execute(UsersAction.SelectUser(it))
            }

            swipeLayout.refreshes()
                    .asLiveData(viewModel)
                    .observe(viewLifecycleOwner) {
                        viewModel.execute(UsersAction.LoadUsers)
                        swipeLayout.isRefreshing = false
                    }
        }

    }

    private fun bindViewModelToView() {

        viewModel.loadingIndicatorVisibility.observe(viewLifecycleOwner) {
            binding.loadingIndicator.visibility = it
        }

        viewModel.users.observe(viewLifecycleOwner) {
            (binding.leaderboardList.adapter as? MyAdapter)?.updateItems(it)
        }
    }

    private fun setupNav() {
        viewModel.userSelected.observe(this) {
            findNavController().navigate(R.id.action_leaderboard_to_userProfile)
        }
    }
}

class MyAdapter(private val onClickListener: (userId: String) -> Unit) :
    RecyclerView.Adapter<MyAdapter.ViewHolder>() {

    private val items: ArrayList<User> = ArrayList()


    fun updateItems(items: List<User>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class ViewHolder(val item: View) : RecyclerView.ViewHolder(item)


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ViewHolder {
        // create a new view
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_view_item, parent, false)

        return ViewHolder(itemView)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.item.findViewById<TextView>(R.id.user_name_text).text = items[position].name

        holder.item.findViewById<ImageView>(R.id.user_avatar_image)
                .setImageResource(items[position].avatar)

        holder.item.findViewById<TextView>(R.id.user_points_text).text = items[position].points.toString()

        holder.item.setOnClickListener {
            onClickListener.invoke(items[position].id)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return items.size
    }
}
