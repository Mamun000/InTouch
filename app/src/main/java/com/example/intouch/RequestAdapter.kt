package com.example.intouch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RequestAdapter(
    private val requests: List<ConnectionRequest>,
    private val onAcceptClick: (ConnectionRequest) -> Unit,
    private val onRejectClick: (ConnectionRequest) -> Unit,
    private val onCardClick: (ConnectionRequest) -> Unit,
    private val showActions: Boolean = true // For "I Requested" tab, don't show actions
) : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProfilePicture: ImageView = itemView.findViewById(R.id.ivProfilePicture)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvProfession: TextView = itemView.findViewById(R.id.tvProfession)
        val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        val btnReject: Button = itemView.findViewById(R.id.btnReject)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val layoutActions: View = itemView.findViewById(R.id.layoutActions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_connection_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]

        // Set request data
        holder.tvName.text = request.requesterName
        holder.tvProfession.text = request.requesterProfession ?: "No profession"
        holder.tvEmail.text = request.requesterEmail ?: "No email"

        // Set profile picture
        if (request.requesterProfileImage != null) {
            holder.ivProfilePicture.setImageBitmap(request.requesterProfileImage)
        } else {
            holder.ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
        }

        // Show/hide actions based on tab type
        if (showActions && request.status == "pending") {
            holder.layoutActions.visibility = View.VISIBLE
            holder.tvStatus.visibility = View.GONE
            holder.btnAccept.visibility = View.VISIBLE
            holder.btnReject.visibility = View.VISIBLE
        } else {
            holder.layoutActions.visibility = View.GONE
            holder.tvStatus.visibility = View.VISIBLE
            holder.btnAccept.visibility = View.GONE
            holder.btnReject.visibility = View.GONE
            
            // Show status
            when (request.status) {
                "accepted" -> {
                    holder.tvStatus.text = "Accepted"
                    holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
                }
                "rejected" -> {
                    holder.tvStatus.text = "Rejected"
                    holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
                }
                "pending" -> {
                    holder.tvStatus.text = "Pending"
                    holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.darker_gray))
                }
            }
        }

        // Card click listener - view profile
        holder.itemView.setOnClickListener {
            onCardClick(request)
        }

        // Accept button click listener
        holder.btnAccept.setOnClickListener {
            onAcceptClick(request)
        }

        // Reject button click listener
        holder.btnReject.setOnClickListener {
            onRejectClick(request)
        }
    }

    override fun getItemCount(): Int = requests.size
}
