# ✦ Inkblot
> *Your writer's sanctuary.*

Inkblot is a cyberpunk-themed Android journaling and creative writing app built for writers who take their craft seriously. It combines a powerful rich text editor, AI-powered writing feedback, streak tracking, community features, and a competitions board — all in a dark, neon-accented UI.

---

## Table of Contents
- [Features](#features)
- [Screenshots](#screenshots)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Firebase Setup](#firebase-setup)
- [Admin Setup](#admin-setup)
- [Project Structure](#project-structure)
- [Color Palette](#color-palette)
- [Contributing](#contributing)
- [License](#license)

---

## Features

### ✍️ Writing
- Rich text editor powered by [richeditor-android](https://github.com/wasabeef/richeditor-android) with support for bold, italic, underline, strikethrough, H1/H2 headings, bullet lists, blockquotes, and left/center alignment
- **Autosave** — drafts save automatically 3 seconds after you stop typing, with a 30-second backup timer. A toast confirms each autosave
- Live word count updates as you type
- New entries and edits are stored separately — editing never duplicates an entry
- Each entry can be toggled **Public** or **Private**

### 🏠 Home Screen
- Time-aware greeting using your saved username (Good morning / afternoon / evening)
- Stats dashboard showing total entries, total words, average words per entry, and current day streak
- Three most recent entries for quick access
- Deadline tracker with urgency-based color coding:
  - 🔴 Overdue / Due today
  - 🟠 Due tomorrow
  - 🟡 Due in 1–3 days
  - ⚪ Upcoming
- Long press any deadline card to delete it

### 📁 My Work
- Full scrollable list of all your entries, ordered by most recent
- **Search bar** — filters entries by title or content in real time
- Per-card actions: Edit, Make Public/Private, Delete
- Word count recalculated from actual content for accuracy
- Confirmation dialog before deletion

### 🔥 Streak Tracker
- Write one entry per day to keep your streak alive
- **Grace period** — miss one day and your streak freezes (not resets). Miss two days in a row and it resets to 1
- The grace period can only be used once per streak — you can't alternate days indefinitely
- Current streak and longest streak both stored in Firestore

### ⏰ Deadlines
- Add standalone deadlines with a title and a date picker
- Push notifications fire automatically at **3 days before** and **1 day before** the deadline (scheduled via WorkManager)
- Deadlines from the Compete screen can be added to your personal deadline list with one tap

### 🏆 Compete
- Community competitions board — **any logged-in user can add a competition**
- Each competition card shows: name, description, status badge (Ongoing / Upcoming), mode (Online / Offline), location (if offline), event date and time, and submission deadline
- **"+ Add to my deadlines"** button on each card links directly to your home screen deadlines
- **Admins** (users with `isAdmin: true` in Firestore) can edit and delete any competition

### 🌐 The Inklings (Community)
- Community feed where writers share thoughts, updates, and snippets
- Posts are attributed to your Inkblot username (not your Google display name)
- **Search writers by username** — results show their name and about blurb
- **Tap any username** on a post or search result to visit their public profile
- Public profiles show the writer's about blurb and all their public entries, which are readable in full

### ✦ AI Writing Analysis
- Powered by the **Mistral AI API** (`mistral-small-latest`)
- On demand — tap "Analyse my writing" on any entry detail screen
- Gives structured feedback on: writing style and voice, strengths, and one improvement suggestion
- Response renders with **bold** and *italic* markdown formatting

### ⚙️ Settings
- Set or update your **username** (minimum 3 characters) — shown in greetings and community posts
- Add an **About Me** blurb visible on your public profile
- Save button greys out after saving and re-enables when you make changes
- **Sign out** clears the Google account session and shows the account picker on next sign-in (via `revokeAccess`)

### 👤 First-Time Setup
- New users are routed to a **username setup screen** immediately after Google Sign-In before reaching the home screen
- Returning users skip this screen automatically

---

## Screenshots
*Coming soon*

---

## Tech Stack

| Area | Technology |
|---|---|
| Language | Java |
| Platform | Android (min SDK 26, target SDK 36) |
| Authentication | Firebase Auth — Google Sign-In |
| Database | Firebase Firestore |
| Navigation | Jetpack Navigation Component |
| Rich Text Editor | wasabeef/richeditor-android 2.0.0 |
| AI Analysis | Mistral AI REST API |
| Background Tasks | AndroidX WorkManager |
| UI | Material Components for Android |
| Networking | OkHttp 4.12.0 |

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android device or emulator running API 26+
- A Firebase project with Firestore and Google Auth enabled
- A free Mistral AI API key from [console.mistral.ai](https://console.mistral.ai)

### 1. Clone the repository
```bash
git clone https://github.com/your-username/inkblot.git
cd inkblot
```

### 2. Add your `google-services.json`
- Go to [Firebase Console](https://console.firebase.google.com)
- Open your project → Project Settings → Your apps
- Download `google-services.json`
- Place it inside the `app/` directory

### 3. Add your Mistral API key
In `app/src/main/java/com/inkblot/app/EntryDetailFragment.java`:
```java
private static final String API_KEY = "your-mistral-api-key-here";
```

### 4. Build and run
Open the project in Android Studio and click **Run ▶**

---

## Firebase Setup

### Authentication
Firebase Console → Authentication → Sign-in method → Enable **Google**

### Firestore
Firebase Console → Firestore Database → Create database, then apply the following security rules:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Users can read/write their own profile and subcollections
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;

      match /entries/{entryId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }

      match /deadlines/{deadlineId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }

    // Any logged-in user can read another user's public entries
    match /users/{userId}/entries/{entryId} {
      allow read: if request.auth != null && resource.data.isPublic == true;
    }

    // Any logged-in user can read any user's profile (for community features)
    match /users/{userId} {
      allow read: if request.auth != null;
    }

    // Any logged-in user can read/write competitions
    match /competitions/{compId} {
      allow read, write: if request.auth != null;
    }

    // Any logged-in user can read/write community posts
    match /community/{postId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## Admin Setup

Admins can edit and delete competitions on the Compete screen.

To grant admin access to a user:
1. Firebase Console → Firestore → `users` collection
2. Open the document matching the user's UID
3. Add field: `isAdmin` → type `boolean` → value `true`

---

## Project Structure

```
app/src/main/
├── java/com/inkblot/app/
│   ├── LoginActivity.java                  # Google Sign-In entry point
│   ├── UsernameSetupActivity.java          # First-time username + about setup
│   ├── MainActivity.java                   # Bottom nav host activity
│   ├── HomeFragment.java                   # Dashboard: stats, recent entries, deadlines
│   ├── WriteFragment.java                  # Rich text editor with autosave + streak
│   ├── MyWorkFragment.java                 # Entry list with search, edit, delete
│   ├── EntryDetailFragment.java            # Full entry view + AI writing analysis
│   ├── CompeteFragment.java                # Competitions board with admin controls
│   ├── CommunityFragment.java              # The Inklings feed + user search
│   ├── UserProfileFragment.java            # Public writer profile + public entries
│   ├── SettingsFragment.java               # Username, about me, sign out
│   ├── AddDeadlineDialogFragment.java      # Deadline creation dialog + notifications
│   ├── DeadlineNotificationWorker.java     # WorkManager worker for push notifications
│   └── StatsFragment.java                  # Placeholder for future stats screen
│
└── res/
    ├── layout/                             # All XML layouts
    ├── drawable/                           # Nav icons + card backgrounds
    ├── color/                              # Bottom nav tint selector
    ├── menu/                               # Bottom navigation menu
    ├── navigation/                         # nav_graph.xml
    └── values/
        ├── colors.xml                      # Cyberpunk color palette
        ├── strings.xml
        └── themes.xml
```

---

## Color Palette

Inkblot uses a per-screen cyberpunk dark theme with neon accents.

| Name | Hex | Used for |
|---|---|---|
| `neon_cyan` | `#00FFE5` | Primary accent, active nav, links |
| `neon_green` | `#39FF14` | Secondary accent, streak, compete screen |
| `bg_dark` | `#0D0D0D` | Global fallback background |
| `bg_home` | `#0A0F0F` | Home screen |
| `bg_write` | `#080D0A` | Write screen |
| `bg_mywork` | `#0A0A0F` | My Work + Entry Detail |
| `bg_compete` | `#0F0D08` | Compete screen |
| `bg_community` | `#0D0A0F` | Community + User Profile |
| `bg_card` | `#141A1A` | All card backgrounds |
| `text_primary` | `#E8F5F3` | Body text |
| `text_muted` | `#4A6060` | Secondary text, labels |

---

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you'd like to change.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m 'Add your feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

---

## License

MIT License — free to use, modify, and distribute with attribution.

---

*Built with ✦ by the Inkblot team*
