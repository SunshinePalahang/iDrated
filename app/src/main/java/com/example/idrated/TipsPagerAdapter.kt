package com.example.idrated

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.idrated.databinding.FragmentTipsBinding

class TipsPagerAdapter(
    private val tipsList: List<TipItem>,
    private val onSeeMoreClick: (String) -> Unit
) : RecyclerView.Adapter<TipsPagerAdapter.TipViewHolder>() {

    inner class TipViewHolder(private val binding: FragmentTipsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tipItem: TipItem) {
            binding.imageView.setImageResource(tipItem.imageRes)
            binding.title.text = tipItem.title
            binding.description.text = tipItem.description
            binding.seeMore.setOnClickListener {
                onSeeMoreClick(tipItem.link)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TipViewHolder {
        val binding = FragmentTipsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TipViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TipViewHolder, position: Int) {
        holder.bind(tipsList[position])
    }

    override fun getItemCount(): Int = tipsList.size
}
