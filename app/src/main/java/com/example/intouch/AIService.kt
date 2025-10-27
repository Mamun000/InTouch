package com.example.intouch

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AIService {
    // Using Mistral-7B-Instruct for better conversational AI responses
    @Headers("Content-Type: application/json")
    @POST("api/models/mistralai/Mistral-7B-Instruct-v0.1")
    fun getAIResponse(@Body request: HuggingFaceRequest): Call<List<HuggingFaceResponse>>
}

