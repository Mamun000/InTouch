package com.example.intouch

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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

        showInitialMessage()
    }

    private fun showInitialMessage() {
        addMessage(
            "Hi! 👋 I'm your InTouch AI Assistant. Tell me what you're looking for, and I'll suggest friends from your network who have the right skills!\n\nTry asking:\n• \"I need help with web development\"\n• \"Find someone who knows Android\"\n• \"I'm looking for a designer\"",
            isUser = false
        )
    }

    private fun addMessage(text: String, isUser: Boolean) {
        messages.add(ChatMessage(text, isUser))
        adapter.notifyItemInserted(messages.size - 1)
        recyclerView.smoothScrollToPosition(messages.size - 1)
        emptyStateText.visibility = View.GONE
    }

    private fun generateAIResponse(userMessage: String) {
        val messageLower = userMessage.toLowerCase().trim()

        // Check if user is authenticated
        if (auth.currentUser == null) {
            addMessage("You need to be logged in to use the AI assistant. Please log in and try again.", isUser = false)
            return
        }

        // Check if user selected a suggestion by number
        if (messageLower in listOf("1", "2", "3")) {
            val suggestionIndex = messageLower.toInt() - 1
            if (suggestionIndex < lastSuggestions.size) {
                val selectedUser = lastSuggestions[suggestionIndex]
                val userId = selectedUser["uid"] as? String ?: return

                val intent = Intent(this, SuggestionDetailActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
                return
            }
        }

        // Show loading message
        addMessage("Searching for matches... 🔍", isUser = false)

        // Get all users from Firebase and process suggestions
        val allUsers = mutableListOf<Map<String, Any>>()
        database.reference.child("users")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
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
                        addMessage("No users available. Please try again later.", isUser = false)
                    } else {
                        processSuggestions(userMessage, allUsers, emptySet())
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    addMessage("Sorry, I couldn't fetch user data. Error: ${error.message}\n\nPlease check your internet connection.", isUser = false)
                }
            })
    }

    private fun processSuggestions(query: String, allUsers: List<Map<String, Any>>, contactIds: Set<String> = emptySet()) {
        val currentUserId = auth.currentUser?.uid ?: return
        val queryLower = query.toLowerCase()

        // Extract skills/keywords from query
        val skillKeywords = extractKeywords(queryLower)

        // If no keywords extracted, try to use the query itself
        val searchKeywords = if (skillKeywords.isNotEmpty()) skillKeywords else listOf(queryLower)

        // Find matching users and prioritize contacts
        val allMatches = allUsers.filter { user ->
            val userId = user["uid"].toString()
            val userSkills = (user["skills"] as? String)?.toLowerCase() ?: ""
            val userBio = (user["bio"] as? String)?.toLowerCase() ?: ""
            val userProfession = (user["profession"] as? String)?.toLowerCase() ?: ""
            val userName = (user["fullName"] as? String)?.toLowerCase() ?: ""

            // Exclude current user
            val isNotCurrentUser = userId != currentUserId
            
            // Check if user has any useful info (skills, bio, or profession)
            val hasRelevantInfo = userSkills.isNotEmpty() || userBio.isNotEmpty() || userProfession.isNotEmpty()
            
            // More flexible matching: check if any keywords match skills, bio, profession, or name
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
        }
    }

    private fun extractKeywords(query: String): List<String> {
        // Common stop words to filter out
        val stopWords = setOf("i", "me", "my", "we", "us", "our", "you", "your", "he", "she", "him", "her", "his", "hers", 
            "they", "them", "their", "it", "its", "what", "which", "who", "whom", "this", "that", "these", "those",
            "a", "an", "the", "and", "or", "but", "if", "then", "else", "of", "at", "by", "for", "with", "from", 
            "to", "in", "on", "up", "about", "into", "through", "during", "before", "after", "above", "below",
            "out", "off", "over", "under", "again", "further", "then", "once", "here", "there", "when", "where", 
            "why", "how", "all", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor",
            "not", "only", "own", "same", "so", "than", "too", "very", "can", "will", "just", "should", "now",
            "need", "looking", "for", "find", "someone", "help", "with", "knows", "is", "am", "are", "was", "were",
            "be", "been", "being", "have", "has", "had", "do", "does", "did", "get", "got", "go", "want", "try")

        // Extract meaningful words from the query (words longer than 3 chars, not stop words)
        val words = query.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ") // Replace punctuation with spaces
            .split("\\s+".toRegex())
            .filter { 
                it.length > 3 && it !in stopWords && it !in listOf("the", "and", "for", "are", "but", "not", "you", "all", "can", "her", "was", "one", "our", "out", "day", "get", "has", "him", "his", "how", "its", "may", "new", "now", "old", "see", "two", "way", "who", "boy", "did", "man", "men", "put", "say", "say", "use", "war", "eat", "got", "got", "let", "run", "set")
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
            "html", "css", "sql", "php", "swift", "objective", "xcode", "android", "dart",
            "bootstrap", "vue", "angular", "typescript", "nextjs", "node",
            "tailwind", "sass", "less", "stylus", "gulp", "webpack", "babel"
        )

        val skillMatches = commonSkills.filter { query.contains(it) }
        
        // Combine extracted words with skill matches, ensuring we return something meaningful
        val allKeywords = (words + skillMatches).distinct()
        
        // If we didn't find anything useful, return the query words as-is
        return if (allKeywords.isNotEmpty()) allKeywords else words
    }

    private fun buildSuggestionResponse(query: String, matches: List<Map<String, Any>>, contactIds: Set<String> = emptySet()): String {
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
}
