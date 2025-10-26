package com.example.intouch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

// Carousel Adapter for ViewPager2 - Uses actual image drawables
class CarouselAdapter(private val images: List<Int>) : RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder>() {

    inner class CarouselViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.carouselImage)

        fun bind(imageRes: Int) {
            // Load the drawable image with scaling
            imageView.apply {
                setImageResource(imageRes)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carousel, parent, false)
        return CarouselViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount() = images.size
}

// Data class for image metadata (optional)
data class CarouselImage(
    val imageRes: Int,
    val title: String = "",
    val description: String = ""
)