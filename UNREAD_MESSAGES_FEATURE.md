# Unread Messages Feature

## Overview
Your InTouch app now has WhatsApp-style unread message count badges! The feature automatically tracks and displays unread messages for each chat in your chats list.

## How It Works

### 1. **Unread Count Tracking** 📊
- Each chat maintains a separate unread count for each user
- Stored in Firebase: `/chats/{chatId}/unreadCount/{userId}/`
- Automatically incremented when you receive a new message
- Automatically reset to 0 when you open that chat

### 2. **Visual Badge** 🔴
- Red circular badge appears next to the timestamp in the chats list
- Shows the number of unread messages
- Only appears when there are unread messages (count > 0)
- Hidden when all messages are read

### 3. **Real-Time Updates** ⚡
- Badge count updates in real-time as new messages arrive
- No need to manually refresh the chats list
- Similar to WhatsApp behavior

## Implementation Details

### Firebase Structure
```
/chats/{chatId}/
  ├── lastMessage/         # Last message info
  ├── messages/            # All messages
  └── unreadCount/         # Unread counts
      ├── {userId1}/       # Unread count for user 1
      └── {userId2}/       # Unread count for user 2
```

### Key Features

#### **Increment Unread Count** ➕
When someone sends you a message:
- Unread count for your user ID is incremented in Firebase
- Badge appears in the chats list
- Real-time updates even if you're viewing the chats list

**Location:** `ChatActivity.kt` → `sendMessage()`

#### **Reset Unread Count** 🔄
When you open a chat:
- Unread count is automatically reset to 0
- Badge disappears from the chats list
- You can read the messages without the count persisting

**Location:** `ChatActivity.kt` → `markMessagesAsRead()`

#### **Display Badge** 🎨
In the chats list:
- Badge shows unread count if > 0
- Badge is hidden if count = 0
- Real-time updates using Firebase ValueEventListener

**Location:** `ChatsActivity.kt` → `loadChatItem()`

## User Experience

### Example Flow:
1. **User A** sends a message to **User B**
2. **User B**'s unread count increments (e.g., from 0 to 1)
3. In **User B**'s chats list, a badge shows "1"
4. When **User B** opens the chat, the count resets to 0
5. Badge disappears from the chats list
6. When **User A** sends another message, count becomes 1 again

### Edge Cases Handled:
✅ Self-messages don't increment unread count  
✅ Opening a chat immediately resets count  
✅ Real-time updates without manual refresh  
✅ Only shows badge when count > 0  
✅ Survives app restarts (data in Firebase)  

## Testing

### Test Unread Count:
1. Have two users open the app
2. User A sends a message to User B
3. Check User B's chats list - should show badge "1"
4. User B opens the chat - badge disappears
5. User A sends another message
6. Check User B's chats list - badge shows "1" again

### Test Real-Time Updates:
1. Keep chats list open on device
2. Send message from another device
3. Badge count should update automatically
4. No need to close and reopen the app

### Test Multiple Chats:
1. Get messages from multiple people
2. Each chat should show its own unread count
3. Opening one chat doesn't affect others
4. Each count is independent

## UI Components

### Badge Design:
- **Location:** Top-right of each chat item
- **Shape:** Circular/oval
- **Color:** Primary color (from your theme)
- **Size:** 20dp
- **Text:** Bold, white, 11sp
- **Position:** Below timestamp

**File:** `item_chat.xml` (line 79-90)

### Badge Background:
```xml
android:id="@+id/tvUnreadCount"
android:layout_width="20dp"
android:layout_height="20dp"
android:background="@drawable/unread_count_background"
android:gravity="center"
android:visibility="gone"
```

## Code Locations

### Files Modified:
1. **`ChatActivity.kt`**
   - Added `markMessagesAsRead()` method
   - Modified `sendMessage()` to increment unread counts

2. **`ChatsActivity.kt`**
   - Added real-time unread count listener
   - Load and display unread counts from Firebase

3. **`ChatsAdapter.kt`**
   - Already had badge display logic (no changes needed)

### Methods Added:
- `markMessagesAsRead()` - Resets unread count when chat is opened
- Updated `sendMessage()` - Increments unread count for receiver
- Updated `loadChatItem()` - Real-time listener for unread counts

## Future Enhancements

### Optional Improvements:
1. **Unread badge on bottom navigation** - Show total unread count
2. **Sound/notification** - Alert when new messages arrive
3. **Read receipts** - Show "read" indicators
4. **Message preview** - Show snippet of unread messages
5. **Last read timestamp** - Track when user last read messages

## Troubleshooting

### Badge Not Showing?
- Check if unread count is stored in Firebase
- Verify the drawable `unread_count_background.xml` exists
- Check that `tvUnreadCount` is in your layout

### Badge Not Updating?
- Ensure Firebase listeners are active
- Check network connection
- Verify user authentication

### Count Incorrect?
- Check if chat ID is generated correctly
- Verify sender/receiver IDs are different
- Ensure Firebase path is correct

## Database Queries

### Get Unread Count for User:
```javascript
// Firebase Console
/chats/{chatId}/unreadCount/{userId}
```

### Set Unread Count:
```kotlin
database.reference.child("chats").child(chatId)
    .child("unreadCount").child(userId)
    .setValue(count)
```

### Reset Unread Count:
```kotlin
database.reference.child("chats").child(chatId)
    .child("unreadCount").child(userId)
    .setValue(0)
```

## Summary

✅ Unread message tracking implemented  
✅ WhatsApp-style badge display  
✅ Real-time updates  
✅ Automatic read marking  
✅ Firebase persistence  
✅ Edge cases handled  

Your app now has professional unread message functionality! 🎉

