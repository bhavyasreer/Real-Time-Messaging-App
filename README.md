# BLink - Real-Time Messaging App

A modern, feature-rich real-time messaging application built with Android and Firebase, designed to provide seamless communication between users with advanced features like group chats, message status tracking, and real-time updates.

## 🚀 Features

### Core Messaging
- **Real-time messaging** with instant message delivery
- **Message status tracking** (sent → delivered → read)
- **Message deletion** (for me / for everyone)
- **Read receipts** and unread message counts
- **Message timestamps** with formatted display

### User Management
- **Firebase Authentication** for secure user login/registration
- **User profiles** with customizable names
- **Online/offline status** tracking
- **Profile picture support** with text-based avatars

### Chat Features
- **Individual chats** between two users
- **Group chats** with multiple participants
- **Chat search** functionality
- **Favorites system** for important conversations
- **Chat tabs** (All, Groups, Favorites)
- **Unread message indicators**

### Group Chat Features
- **Create group chats** with custom names
- **Add multiple participants** to groups
- **Group info display** with participant list
- **Group-specific message handling**

### UI/UX Features
- **Material Design** components
- **Smooth animations** and transitions
- **Responsive layouts** for different screen sizes
- **Dark/Light theme** support
- **Search functionality** for chats and users
- **Loading indicators** and error states

## 🛠️ Technology Stack

- **Frontend**: Android (Java)
- **Backend**: Firebase
  - **Authentication**: Firebase Auth
  - **Database**: Cloud Firestore
  - **Storage**: Firebase Storage (for profile pictures)
- **Real-time Updates**: Firestore Listeners
- **UI Components**: Material Design Components

## 📱 Screenshots

*[Add screenshots of your app here]*

## 🏗️ Project Structure

```
app/
├── src/main/
│   ├── java/com/example/messenger/
│   │   ├── Activities/
│   │   │   ├── SplashActivity.java
│   │   │   ├── LoginActivity.java
│   │   │   ├── ChatListActivity.java
│   │   │   ├── ChatActivity.java
│   │   │   ├── NewChatActivity.java
│   │   │   └── ProfileSettingsActivity.java
│   │   ├── Adapters/
│   │   │   ├── ChatListAdapter.java
│   │   │   ├── MessageAdapter.java
│   │   │   ├── UserAdapter.java
│   │   │   └── GroupParticipantsAdapter.java
│   │   ├── Models/
│   │   │   ├── User.java
│   │   │   ├── Chat.java
│   │   │   └── Message.java
│   │   └── Utils/
│   │       ├── ErrorHandler.java
│   │       └── TextDrawableHelper.java
│   ├── res/
│   │   ├── layout/
│   │   ├── drawable/
│   │   ├── values/
│   │   └── anim/
│   └── AndroidManifest.xml
└── build.gradle
```

## 🔧 Setup Instructions

### Prerequisites
- Android Studio (latest version)
- Android SDK (API level 21 or higher)
- Google account for Firebase

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/bhavyasreer/Real-Time-Messaging-App.git
   cd Real-Time-Messaging-App
   ```

2. **Set up Firebase**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project
   - Add Android app to your project
   - Download `google-services.json` and place it in the `app/` directory
   - Enable Authentication (Email/Password)
   - Enable Cloud Firestore
   - Set up Firestore security rules

3. **Configure Firebase in your project**
   - Add Firebase dependencies in `app/build.gradle`
   - Sync project with Gradle files

4. **Run the app**
   - Connect an Android device or start an emulator
   - Click "Run" in Android Studio

### Firebase Configuration

1. **Authentication Setup**
   ```
   Firebase Console → Authentication → Sign-in method
   Enable Email/Password authentication
   ```

2. **Firestore Database Setup**
   ```
   Firebase Console → Firestore Database → Create database
   Start in test mode (for development)
   ```

3. **Security Rules** (for production)
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
       match /chats/{chatId} {
         allow read, write: if request.auth != null && 
           request.auth.uid in resource.data.participants;
       }
     }
   }
   ```

## 📊 Database Schema

### Users Collection
```javascript
users/{userId}
{
  name: string,
  email: string,
  uid: string,
  isOnline: boolean,
  lastSeen: timestamp
}
```

### Chats Collection
```javascript
chats/{chatId}
{
  participants: [userId1, userId2, ...],
  isGroup: boolean,
  groupName: string,
  lastMessageText: string,
  lastMessageTime: timestamp,
  favourite: [userId1, userId2, ...],
  unreadCounts: {
    userId1: number,
    userId2: number
  }
}
```

### Messages Subcollection
```javascript
chats/{chatId}/messages/{messageId}
{
  text: string,
  senderId: string,
  timestamp: timestamp,
  status: string // "sent", "delivered", "read"
}
```

## 🚀 Key Features Explained

### Real-time Messaging
- Uses Firestore's real-time listeners to instantly update message lists
- Messages appear immediately for all participants
- Status updates happen in real-time

### Message Status Tracking
- **Sent**: Message successfully stored in database
- **Delivered**: Message received by recipient's device
- **Read**: Message viewed by recipient

### Group Chat Creation
1. Long press the FAB in chat list
2. Enter group name
3. Select participants from the list
4. Create group with custom name

### Search Functionality
- **Chat Search**: Search through existing conversations
- **User Search**: Find users to start new conversations
- Case-insensitive partial matching

## 🔒 Security Features

- Firebase Authentication for user verification
- Firestore security rules for data protection
- User-specific data access controls
- Secure message storage and retrieval

## 🎨 UI/UX Highlights

- **Splash Screen**: Animated logo and app name
- **Material Design**: Modern UI components
- **Smooth Transitions**: Between activities
- **Responsive Design**: Works on various screen sizes
- **Loading States**: User feedback during operations

## 🐛 Known Issues

- [List any known issues here]
- [Or remove this section if no issues]

## 🔮 Future Enhancements

- [ ] Push notifications
- [ ] File sharing (images, documents)
- [ ] Voice messages
- [ ] Video calls
- [ ] Message encryption
- [ ] Custom themes
- [ ] Message reactions
- [ ] User status messages

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Bhavya Sree**
- GitHub: [@bhavyasreer](https://github.com/bhavyasreer)

## 🙏 Acknowledgments

- Firebase team for the excellent backend services
- Material Design team for the UI components
- Android community for resources and support

## 📞 Support

If you have any questions or need help, please:
- Open an issue on GitHub
- Contact the author via GitHub

---

**Made with ❤️ by Bhavya Sree**
