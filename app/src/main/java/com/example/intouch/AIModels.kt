package com.example.intouch

import com.google.gson.annotations.SerializedName

data class HuggingFaceRequest(
    val inputs: String,
    val parameters: Parameters = Parameters()
)

data class Parameters(
    val max_new_tokens: Int = 256,
    val return_full_text: Boolean = false,
    val temperature: Double = 0.7,
    val top_p: Double = 0.9
)

data class HuggingFaceResponse(
    @SerializedName("generated_text")
    val generatedText: String
)

