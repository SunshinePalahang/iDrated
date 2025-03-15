package com.example.idrated

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class AvatarAdapter(
    private val avatars: List<Int>, // List of avatar resource IDs
    private val onAvatarSelected: (Int) -> Unit // Callback when an avatar is selected
) : RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder>() {

    inner class AvatarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatarImage: ImageView = itemView.findViewById(R.id.avatarImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.dialog_avatar_item, parent, false)
        return AvatarViewHolder(view)
    }

    override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
        val avatarResId = avatars[position]
        holder.avatarImage.setImageResource(avatarResId)

        // No need for special border, just set the image
        holder.avatarImage.setBackgroundResource(R.drawable.avatar_border)

        // Handle avatar click
        holder.itemView.setOnClickListener {
            onAvatarSelected(avatarResId) // Notify the fragment or activity about the selection
        }
    }

    override fun getItemCount(): Int = avatars.size
}
