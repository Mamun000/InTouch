# AI Chatbot Setup Guide

## Overview
Your InTouch app now has real AI functionality with intelligent connection search! The chatbot uses Hugging Face's free Inference API to provide conversational AI responses, AND it can automatically find connections from your network based on what you're looking for.

## How It Works

### Architecture
1. **AIService.kt** - Interface for making API calls to Hugging Face
2. **AIClient.kt** - Retrofit client configuration
3. **AIModels.kt** - Data models for request/response
4. **ChatbotActivity.kt** - Main chatbot logic with AI + connection search

### Connection Search Feature 🎯
When you mention skills or topics (like "Web development", "Android", "Design"), the AI will:
1. Extract keywords from your message
2. Search Firebase for users matching those keywords in their:
   - Skills
   - Bio
   - Profession
3. Return up to 3 matching connections
4. Let you select a connection by number (1, 2, or 3) to view their profile

**Example:**
- You say: "I need help with web development"
- AI responds with conversational message
- AI also searches and shows: "I found 2 people who match your needs..."
- You can type "1" to view the first person's profile

### When Does It Search?
The chatbot automatically searches for connections when you mention:
- Specific skills: Android, Web, Python, Java, React, etc.
- Keywords: "find", "search", "looking for", "need help with"
- Professions: Developer, Designer, Engineer, etc.

You can also have regular conversations without triggering searches.

### Current AI Model
- **Model**: Mistral-7B-Instruct v0.1 (via Hugging Face)
- **Benefits**: High-quality conversational AI, free tier available
- **Fallback**: If AI service fails, intelligent rule-based responses are used

### Features
✅ Real conversational AI responses
✅ **Automatic connection search** - finds matching people in your network
✅ Conversation history stored in Firebase
✅ Intelligent fallback responses
✅ Loading indicators for better UX
✅ Context-aware responses
✅ Smart keyword detection for finding connections

## Configuration Options

### Switching AI Models

If you want to use a different model, edit `AIService.kt`:

```kotlin
// Current (Mistral - Recommended for quality)
@POST("api/models/mistralai/Mistral-7B-Instruct-v0.1")

// Alternative options:
@POST("api/models/facebook/blenderbot-3B")  // Conversational
@POST("api/models/microsoft/DialoGPT-large")  // Dialogue-focused
@POST("api/models/gpt2")  // Simpler, faster
```

### Adjusting AI Parameters

Edit `ChatbotActivity.kt` in the `generateAIResponse` method:

```kotlin
val request = HuggingFaceRequest(
    inputs = contextPrompt,
    parameters = Parameters(
        max_new_tokens = 150,  // Max response length
        temperature = 0.7,     // Creativity (0-1, higher = more creative)
        top_p = 0.9            // Nucleus sampling
    )
)
```

## Firebase Integration

### Conversation History
Conversations are automatically saved to Firebase:
```
Firebase Database: /chat_history/{userId}/conversation
```

### Usage
- History loads when activity starts
- Saves conversation context for better AI responses
- Persists between app sessions

## Troubleshooting

### AI Not Responding?
1. **Check internet connection** - AI requires active internet
2. **Check Hugging Face status** - Visit https://status.huggingface.co
3. **Fallback mode** - App uses intelligent rule-based responses if AI fails

### Slow Responses?
- Hugging Face free tier can be slow during high traffic
- Responses typically take 5-30 seconds
- Consider upgrading to a paid API for faster responses

### Poor Quality Responses?
1. Try adjusting `temperature` (0.5-0.9 range)
2. Switch to a different model
3. Modify the context prompt in `buildContextPrompt()`

## Free AI Options

### Current: Hugging Face (Free)
- ✅ No API key required
- ✅ Many open models available
- ❌ Can be slow (5-30s responses)
- ❌ Rate limits on free tier

### Alternative: Groq API (Faster, Free Tier)
If you want faster responses, you can use Groq's API:

1. Sign up at https://console.groq.com
2. Get your free API key
3. Update `AIClient.kt`:
   ```kotlin
   private const val BASE_URL = "https://api.groq.com/openai/"
   
   private val okHttpClient = OkHttpClient.Builder()
       .addInterceptor { chain ->
           val request = chain.request().newBuilder()
               .header("Authorization", "Bearer YOUR_API_KEY")
               .build()
           chain.proceed(request)
       }
   ```

## Testing

### Test the AI
1. Open the app
2. Navigate to the Chatbot activity
3. Send any message
4. Wait for AI response (may take 5-30 seconds)
5. Check if conversation is saved in Firebase

### Test Connection Search 🔍
Try these test messages:
1. "I need someone who knows Android"
2. "Find me a web developer"
3. "Looking for a designer"
4. "Do you know anyone with Python skills?"
5. "I need help with React"

The chatbot will:
- Respond conversationally with AI
- Search and show matching connections
- Let you select a connection by number

### Test Regular Chat
Send messages that DON'T include skills/keywords:
- "Hello!"
- "How are you?"
- "Tell me a joke"
- "What can you help me with?"

These will get AI responses but won't trigger connection search.

### Test Fallback
To test fallback responses, temporarily disable internet connection while chatting.

## Next Steps

### Optional Enhancements
1. **Add typing indicators** - Show "AI is typing..."
2. **Voice input** - Allow voice messages
3. **Message reactions** - Thumbs up/down for responses
4. **Export chat history** - Download conversations
5. **Multi-language support** - Support multiple languages

### Performance Tips
- Cache common responses locally
- Implement response queuing
- Add retry logic for failed requests
- Use Firebase cache for offline mode

## Support

If you encounter issues:
1. Check the console logs for error messages
2. Verify internet connectivity
3. Test with different messages
4. Check Firebase database structure

Enjoy your AI-powered chatbot! 🚀

