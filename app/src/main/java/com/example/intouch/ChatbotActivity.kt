package com.example.intouch

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatbotActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var inputEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var emptyStateText: TextView
    private lateinit var backButton: ImageButton

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatMessageAdapter
    private val aiService = AIClient.service
    private var conversationHistory = StringBuilder()
    private var lastSuggestions: List<Map<String, Any>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        recyclerView = findViewById(R.id.recyclerViewChat)
        inputEditText = findViewById(R.id.inputMessage)
        sendButton = findViewById(R.id.sendButton)
        emptyStateText = findViewById(R.id.emptyStateText)
        backButton = findViewById(R.id.backButton)

        adapter = ChatMessageAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerView.adapter = adapter

        backButton.setOnClickListener {
            finish()
        }

        sendButton.setOnClickListener {
            val message = inputEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                addMessage(message, isUser = true)
                inputEditText.setText("")
                generateAIResponse(message)
            }
        }

        loadConversationHistory()
        showInitialMessage()
    }

    private fun loadConversationHistory() {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("chat_history").child(userId)
            .get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val history = snapshot.child("conversation").getValue(String::class.java) ?: ""
                    conversationHistory.append(history)
                }
            }
    }

    private fun saveConversationHistory() {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("chat_history").child(userId)
            .child("conversation").setValue(conversationHistory.toString())
    }

    private fun showInitialMessage() {
        val initialText = if (conversationHistory.isEmpty()) {
            "Hi! 👋 I'm your InTouch AI Assistant. I'm here to help you find the right connections and answer your questions!\n\nHow can I help you today?"
        } else {
            "Welcome back! How can I help you today?"
        }
        addMessage(initialText, isUser = false)
    }

    private fun addMessage(text: String, isUser: Boolean) {
        messages.add(ChatMessage(text, isUser))
        adapter.notifyItemInserted(messages.size - 1)
        recyclerView.smoothScrollToPosition(messages.size - 1)
        emptyStateText.visibility = View.GONE

        // Update conversation history
        conversationHistory.append(if (isUser) "User: $text\n" else "Assistant: $text\n")
    }

    private fun generateAIResponse(userMessage: String) {
        if (auth.currentUser == null) {
            addMessage("You need to be logged in to use the AI assistant. Please log in and try again.", isUser = false)
            return
        }

        // Check if user selected a suggestion by number
        if (userMessage.trim() in listOf("1", "2", "3")) {
            val suggestionIndex = userMessage.trim().toInt() - 1
            if (suggestionIndex < lastSuggestions.size) {
                val selectedUser = lastSuggestions[suggestionIndex]
                val userId = selectedUser["uid"] as? String ?: return

                val intent = Intent(this, SuggestionDetailActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
                return
            }
        }

        // Check if user wants to find connections
        if (shouldSearchForConnections(userMessage)) {
            searchForConnections(userMessage)
            return
        }

        // Show typing indicator
        val loadingIndex = messages.size
        addMessage("Thinking...", isUser = false)

        // Build context for AI
        val contextPrompt = buildContextPrompt(userMessage)
        
        val request = HuggingFaceRequest(
            inputs = contextPrompt,
            parameters = Parameters(
                max_new_tokens = 150,
                temperature = 0.7,
                top_p = 0.9
            )
        )

        aiService.getAIResponse(request).enqueue(object : Callback<List<HuggingFaceResponse>> {
            override fun onResponse(
                call: Call<List<HuggingFaceResponse>>,
                response: Response<List<HuggingFaceResponse>>
            ) {
                // Remove loading message
                if (loadingIndex < messages.size) {
                    messages.removeAt(loadingIndex)
                    adapter.notifyItemRemoved(loadingIndex)
                }

                if (response.isSuccessful && response.body() != null && response.body()!!.isNotEmpty()) {
                    val aiResponse = response.body()!!.firstOrNull()
                    if (aiResponse != null && aiResponse.generatedText.isNotEmpty()) {
                        val responseText = cleanAIResponse(aiResponse.generatedText)
                        addMessage(responseText, isUser = false)
                        saveConversationHistory()
                        
                        // Also search for connections if relevant
                        if (containsSkillsOrTopics(userMessage)) {
                            searchForConnections(userMessage, showInitialMessage = false)
                        }
                    } else {
                        // If no AI response, use fallback
                        addIntelligentFallback(userMessage)
                    }
                } else {
                    // If API failed, use fallback
                    addIntelligentFallback(userMessage)
                }
            }

            override fun onFailure(call: Call<List<HuggingFaceResponse>>, t: Throwable) {
                // Remove loading message
                if (loadingIndex < messages.size) {
                    messages.removeAt(loadingIndex)
                    adapter.notifyItemRemoved(loadingIndex)
                }

                // Try fallback response
                addIntelligentFallback(userMessage)
            }
        })
    }

    private fun shouldSearchForConnections(message: String): Boolean {
        val messageLower = message.lowercase()
        val connectionKeywords = listOf(
            "find", "search", "looking for", "need help with", "who knows",
            "looking", "searching", "recommend", "suggest", "recommendation",
            "android", "web", "development", "design", "programming", "coding",
            "developer", "designer", "engineer", "skills", "expert"
        )
        return connectionKeywords.any { messageLower.contains(it) }
    }

    private fun containsSkillsOrTopics(message: String): Boolean {
        val messageLower = message.lowercase()
        val skillKeywords = listOf(
            "android", "ios", "web", "python", "java", "kotlin", "javascript",
            "react", "flutter", "design", "ui", "ux", "marketing", "sales",
            "backend", "frontend", "fullstack", "devops", "database",
            "programming", "coding", "developer", "programmer",
            "html", "css", "sql", "php", "swift","ai","ml"
        )
        return skillKeywords.any { messageLower.contains(it) }
    }

    private fun searchForConnections(query: String, showInitialMessage: Boolean = true) {
        val userId = auth.currentUser?.uid ?: return

        if (showInitialMessage) {
            addMessage("Searching for matches... 🔍", isUser = false)
        }

        // Get all users from Firebase
        val allUsers = mutableListOf<Map<String, Any>>()
        database.reference.child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        if (showInitialMessage) {
                            messages.removeAt(messages.size - 1)
                            adapter.notifyItemRemoved(messages.size)
                        }
                        addMessage("No users found in the database.", isUser = false)
                        return
                    }

                    snapshot.children.forEach { userSnapshot ->
                        val userData = userSnapshot.value as? Map<String, Any>
                        if (userData != null) {
                            val mutableData = userData.toMutableMap()
                            mutableData["uid"] = userSnapshot.key ?: ""
                            allUsers.add(mutableData)
                        }
                    }

                    if (allUsers.isEmpty()) {
                        if (showInitialMessage) {
                            messages.removeAt(messages.size - 1)
                            adapter.notifyItemRemoved(messages.size)
                        }
                        addMessage("No users available. Please try again later.", isUser = false)
                    } else {
                        processSuggestions(query, allUsers, emptySet(), showInitialMessage)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (showInitialMessage) {
                        messages.removeAt(messages.size - 1)
                        adapter.notifyItemRemoved(messages.size)
                    }
                    addMessage("Sorry, I couldn't fetch user data. Error: ${error.message}\n\nPlease check your internet connection.", isUser = false)
                }
            })
    }

    private fun processSuggestions(
        query: String,
        allUsers: List<Map<String, Any>>,
        contactIds: Set<String> = emptySet(),
        showInitialMessage: Boolean = true
    ) {
        if (showInitialMessage) {
            messages.removeAt(messages.size - 1)
            adapter.notifyItemRemoved(messages.size)
        }

        val currentUserId = auth.currentUser?.uid ?: return
        val queryLower = query.lowercase()

        // Extract skills/keywords from query
        val skillKeywords = extractKeywords(queryLower)

        // If no keywords extracted, try to use the query itself
        val searchKeywords = if (skillKeywords.isNotEmpty()) skillKeywords else listOf(queryLower)

        // Find matching users
        val allMatches = allUsers.filter { user ->
            val userId = user["uid"].toString()
            val userSkills = (user["skills"] as? String)?.lowercase() ?: ""
            val userBio = (user["bio"] as? String)?.lowercase() ?: ""
            val userProfession = (user["profession"] as? String)?.lowercase() ?: ""
            val userName = (user["fullName"] as? String)?.lowercase() ?: ""

            // Exclude current user
            val isNotCurrentUser = userId != currentUserId

            // Check if user has any useful info
            val hasRelevantInfo = userSkills.isNotEmpty() || userBio.isNotEmpty() || userProfession.isNotEmpty()

            // More flexible matching
            val hasMatch = searchKeywords.any { keyword ->
                keyword.isNotEmpty() && (
                    userSkills.contains(keyword, ignoreCase = true) ||
                    userBio.contains(keyword, ignoreCase = true) ||
                    userProfession.contains(keyword, ignoreCase = true) ||
                    userName.contains(keyword, ignoreCase = true)
                )
            }

            isNotCurrentUser && hasRelevantInfo && hasMatch
        }

        // Sort: contacts first, then others
        val sortedMatches = allMatches.sortedByDescending { contactIds.contains(it["uid"]) }
        val matches = sortedMatches.take(3) // Limit to top 3 suggestions

        lastSuggestions = matches // Store for later reference

        if (matches.isNotEmpty()) {
            val response = buildSuggestionResponse(query, matches, contactIds)
            addMessage(response, isUser = false)
            saveConversationHistory()
        } else {
            val keywordsUsed = searchKeywords.joinToString(", ")
            addMessage(
                "Hmm, I didn't find anyone matching \"$keywordsUsed\" in our network right now. 💭\n\n" +
                "💡 Try asking about:\n" +
                "• Specific technologies (Android, Web, Python, React)\n" +
                "• Skills (Design, Development, Marketing)\n" +
                "• General areas (Programming, Design, Business)\n\n" +
                "Or be more general, like \"someone who knows programming\" 👨‍💻",
                isUser = false
            )
            saveConversationHistory()
        }
    }

    private fun extractKeywords(query: String): List<String> {
        // Common stop words to filter out
        val stopWords = setOf(
            "i", "me", "my", "we", "us", "our", "you", "your", "he", "she", "him", "her", "his", "hers",
            "they", "them", "their", "it", "its", "what", "which", "who", "whom", "this", "that", "these", "those",
            "a", "an", "the", "and", "or", "but", "if", "then", "else", "of", "at", "by", "for", "with", "from",
            "to", "in", "on", "up", "about", "into", "through", "during", "before", "after", "above", "below",
            "out", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where",
            "why", "how", "all", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor",
            "not", "only", "own", "same", "so", "than", "too", "very", "can", "will", "just", "should", "now",
            "need", "looking", "for", "find", "someone", "help", "with", "knows", "is", "am", "are", "was", "were",
            "be", "been", "being", "have", "has", "had", "do", "does", "did", "get", "got", "go", "want", "try"
        )

        // Extract meaningful words from the query
        val words = query.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ") // Replace punctuation with spaces
            .split("\\s+".toRegex())
            .filter {
                it.length > 3 && it !in stopWords
            }
            .distinct()

        // Also check for common skills that match
        val commonSkills = listOf(
            "android", "ios", "web", "python", "java", "kotlin", "javascript",
            "react", "flutter", "design", "ui", "ux", "marketing", "sales",
            "backend", "frontend", "fullstack", "devops", "database", "api",
            "nodejs", "express", "mongodb", "firebase", "aws", "cloud",
            "graphic", "illustrator", "photoshop", "seo", "content", "writing",
            "finance", "accounting", "law", "legal", "medical", "healthcare",
            "development", "developer", "programming", "software", "coding", "code",
            "html", "css", "sql", "php", "swift", "objective", "xcode", "dart",
            "bootstrap", "vue", "angular", "typescript", "nextjs", "node",
            "tailwind", "sass", "less", "stylus", "gulp", "webpack", "babel"
        )

        val skillMatches = commonSkills.filter { query.contains(it) }

        // Combine extracted words with skill matches
        val allKeywords = (words + skillMatches).distinct()

        return if (allKeywords.isNotEmpty()) allKeywords else words
    }

    private fun buildSuggestionResponse(
        query: String,
        matches: List<Map<String, Any>>,
        contactIds: Set<String> = emptySet()
    ): String {
        var response = "✨ Great! I found ${matches.size} person${if (matches.size > 1) "s" else ""} who match your needs:\n\n"

        matches.forEachIndexed { index, user ->
            val name = user["fullName"] as? String ?: "Unknown"
            val uid = user["uid"] as? String ?: ""
            val skills = user["skills"] as? String ?: ""
            val bio = user["bio"] as? String ?: ""
            val profession = user["profession"] as? String ?: ""
            val isContact = contactIds.contains(uid)

            response += "${index + 1}. 👤 *$name* ${if (isContact) "📌 (Contact)" else ""}\n"

            // Show skills, bio, or profession - whichever is available
            when {
                skills.isNotEmpty() -> response += "   💼 Skills: $skills\n"
                bio.isNotEmpty() -> response += "   💼 Bio: ${bio.take(60)}${if (bio.length > 60) "..." else ""}\n"
                profession.isNotEmpty() -> response += "   💼 Profession: $profession\n"
                else -> response += "   💼 Information not available\n"
            }

            response += "\n"
        }

        response += "💬 Reply with the number (1, 2, or 3) to view their profile and connect!\n"
        response += "Or ask me to find someone else?"

        return response
    }

    private fun buildContextPrompt(userMessage: String): String {
        return buildString {
            append("You are InTouch AI Assistant, a helpful AI chatbot for a professional networking app. ")
            append("Your role is to help users find connections and answer their questions. ")
            append("Be friendly, professional, and helpful. ")
            append("Keep responses concise (under 150 words). ")
            
            // Add recent conversation context
            if (conversationHistory.isNotEmpty()) {
                val recentHistory = conversationHistory.toString()
                    .lines()
                    .takeLast(6)
                    .joinToString("\n")
                append("\n\nRecent conversation:\n$recentHistory")
            }
            
            append("\n\nUser: $userMessage\n")
            append("Assistant: ")
        }
    }

    private fun cleanAIResponse(response: String): String {
        return response
            .trim()
            .replace(Regex("Assistant:\\s*"), "")
            .replace(Regex("User:\\s*"), "")
            .trim()
    }

    private fun addIntelligentFallback(userMessage: String) {
        val messageLower = userMessage.lowercase()
        val fallbackResponse = when {
            messageLower.contains("hello") || messageLower.contains("hi") || messageLower.contains("hey") ->
                "Hello! 👋 How can I help you today with your networking needs?"
            
            messageLower.contains("help") || messageLower.contains("support") ->
                "I'm here to help! 🎯 Tell me what you're looking for, and I can assist you with:\n" +
                "• Finding connections in your network\n" +
                "• Getting recommendations\n" +
                "• General questions about InTouch\n\nWhat do you need help with?"
            
            messageLower.contains("thank") || messageLower.contains("thanks") ->
                "You're welcome! 😊 Is there anything else I can help you with?"
            
            messageLower.contains("bye") || messageLower.contains("goodbye") ->
                "Goodbye! 👋 Feel free to come back anytime if you need help. Take care!"
            
            messageLower.contains("find") || messageLower.contains("search") ->
                "I can help you find people in your network! 🔍 Try asking about:\n" +
                "• Skills (Android, Web, Design)\n" +
                "• Professions\n" +
                "• General interests\n\nWhat are you looking for?"
            
            messageLower.contains("how are you") ->
                "I'm doing great, thanks for asking! 😊 I'm always here to help you with your networking needs. How can I assist you today?"
            
            else ->
                "Thanks for your message! 💬 I understand you said: \"$userMessage\"\n\n" +
                "Let me help you by checking if I can find relevant connections. " +
                "Could you tell me more specifically what you're looking for?"
        }
        
        addMessage(fallbackResponse, isUser = false)
        saveConversationHistory()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveConversationHistory()
    }
}
