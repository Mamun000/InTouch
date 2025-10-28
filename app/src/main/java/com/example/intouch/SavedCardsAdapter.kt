package com.example.intouch

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SavedCardsAdapter(
    private val savedCards: List<SavedCard>,
    private val onCardClick: (SavedCard) -> Unit,
    private val onDeleteClick: (SavedCard) -> Unit
) : RecyclerView.Adapter<SavedCardsAdapter.SavedCardViewHolder>() {

    inner class SavedCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProfilePicture: ImageView = itemView.findViewById(R.id.ivProfilePicture)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvProfession: TextView = itemView.findViewById(R.id.tvProfession)
        val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        val tvPhone: TextView = itemView.findViewById(R.id.tvPhone)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedCardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_card, parent, false)
        return SavedCardViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavedCardViewHolder, position: Int) {
        val card = savedCards[position]

        // Set card data
        holder.tvName.text = card.fullName
        holder.tvProfession.text = card.profession
        holder.tvEmail.text = card.email
        holder.tvPhone.text = card.phone

        // Set profile picture
        if (card.profileImage != null) {
            holder.ivProfilePicture.setImageBitmap(card.profileImage)
        } else {
            holder.ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
        }

        // Card click listener
        holder.itemView.setOnClickListener {
            onCardClick(card)
        }

        // Delete button click listener
        holder.btnDelete.setOnClickListener {
            onDeleteClick(card)
        }
    }

    override fun getItemCount(): Int = savedCards.size
}

// Data class for Saved Card
data class SavedCard(
    val userId: String,
    val fullName: String,
    val profession: String,
    val email: String,
    val phone: String,
    val profileImage: Bitmap?,
    val timestamp: Long
)

