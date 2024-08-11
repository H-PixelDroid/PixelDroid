package org.pixeldroid.app.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.pixeldroid.app.R
import org.pixeldroid.app.databinding.AccountListItemBinding
import org.pixeldroid.app.utils.db.entities.UserDatabaseEntity

class AccountListAdapter(
    private val items: StateFlow<List<UserDatabaseEntity>>,
    lifecycleScope: LifecycleCoroutineScope,
    private val onClick: (UserDatabaseEntity?) -> Unit
) : RecyclerView.Adapter<AccountListAdapter.ViewHolder>() {
    private val itemsList: MutableList<UserDatabaseEntity> = mutableListOf()

    init {
        lifecycleScope.launch {
            items.collect {
                itemsList.clear()
                itemsList.addAll(it.filter { !it.isActive })
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = AccountListItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.root.setOnClickListener{onClick(itemsList.getOrNull(position))}
        if (position == itemsList.size) {
            Glide.with(holder.itemView)
                .load(R.drawable.add)
                .into(holder.binding.imageView)
            holder.binding.accountName.setText(R.string.add_account_name)
            holder.binding.accountUsername.setText(R.string.add_account_description)
            return
        }

        val user = itemsList[position]
        Glide.with(holder.itemView)
            .load(user.avatar_static)
            .placeholder(R.drawable.ic_default_user)
            .circleCrop()
            .into(holder.binding.imageView)
        holder.binding.accountName.text = user.display_name
        holder.binding.accountUsername.text = user.fullHandle
    }

    override fun getItemCount(): Int = itemsList.size + 1

    class ViewHolder(val binding: AccountListItemBinding) : RecyclerView.ViewHolder(binding.root)
}