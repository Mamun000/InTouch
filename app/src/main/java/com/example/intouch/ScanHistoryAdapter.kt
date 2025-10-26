package com.example.intouch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ScanHistoryAdapter(
    private val contacts: List<Contact>,
    private val onContactClick: (Contact) -> Unit
) : RecyclerView.Adapter<ScanHistoryAdapter.ContactViewHolder>() {

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProfile: ImageView = itemView.findViewById(R.id.ivProfile)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvProfession: TextView = itemView.findViewById(R.id.tvProfession)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val tvScanCount: TextView? = itemView.findViewById(R.id.tvScanCount)

        fun bind(contact: Contact, position: Int) {
            tvName.text = contact.fullName
            tvProfession.text = contact.profession
            tvTimestamp.text = formatTimestamp(contact.timestamp)

            // Set profile image
            if (contact.profileImage != null) {
                ivProfile.setImageBitmap(contact.profileImage)
            } else {
                ivProfile.setImageResource(R.drawable.ic_profile_placeholder)
            }

            // Set scan count (position + 1 for display)
            tvScanCount?.text = (position + 1).toString()

            itemView.setOnClickListener {
                onContactClick(contact)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60000 -> "Just now" // Less than 1 minute
                diff < 3600000 -> "${diff / 60000}m ago" // Less than 1 hour
                diff < 86400000 -> "${diff / 3600000}h ago" // Less than 1 day
                diff < 604800000 -> "${diff / 86400000}d ago" // Less than 1 week
                diff < 2592000000 -> "${diff / 604800000}w ago" // Less than 1 month
                else -> {
                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_history, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position], position)
    }

    override fun getItemCount(): Int = contacts.size
}