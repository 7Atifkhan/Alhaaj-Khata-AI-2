# Alhaaj Khata AI - Digital Khata Project Foundation

**Alhaaj Khata AI** is a mobile-first Digital Khata application designed specifically for shopkeepers and small businesses to manage credit, debit, and customer accounts effortlessly.

---

## 🚀 Key Project Foundation Features

- **📱 Mobile-First Design**: Optimized for Android phone screens with clean touch targets and edge-to-edge Material 3 interface.
- **🧭 Exact 5 Bottom Navigation Tabs**:
  1. **Dashboard**: High-level balance cards (You Will Get, You Will Give, Net Balance) and quick actions.
  2. **Customers**: Customer list directory structure, search bar, and filter tabs.
  3. **Transactions**: Debit and Credit ledger history filterable by transaction type.
  4. **Reports**: Monthly statement summaries, analytics placeholder, and export options (PDF/Excel).
  5. **Settings**: Dark & Light mode theme toggle, shop profile settings, and cloud/AI options.
- **⚡ Offline-First Architecture**: Powered by Room Database for fast local persistence without needing active internet connection.
- **☁️ Supabase Cloud Synchronization Layer**: Foundation provider ready for real-time remote sync and cloud backups.
- **🤖 Gemini AI Assistant Layer**: Built-in AI service placeholder for automated credit insights and smart receipt summaries.
- **🎨 Dark & Light Mode Theme Support**: Full M3 color scheme customization with deep emerald teal branding (`#00695C`).

---

## 🛠️ Architecture & Tech Stack

```
Alhaaj Khata AI
├── app/src/main/java/com/example/
│   ├── MainActivity.kt               # Main entry point with Edge-to-Edge setup & Scaffold
│   ├── ui/
│   │   ├── navigation/
│   │   │   ├── NavRoutes.kt          # Sealed class definitions for 5 bottom nav tabs
│   │   │   ├── BottomNavBar.kt       # Material 3 Bottom Navigation Bar
│   │   │   └── AppNavHost.kt         # Navigation controller host
│   │   ├── theme/
│   │   │   ├── Color.kt              # Deep Teal, Gold, Credit Green, Debit Red tokens
│   │   │   ├── Theme.kt              # Light/Dark MaterialTheme switcher
│   │   │   └── Type.kt               # Typography scales
│   │   └── screens/
│   │       ├── DashboardScreen.kt    # Hero balance summary & quick actions
│   │       ├── CustomersScreen.kt    # Customer directory & search
│   │       ├── TransactionsScreen.kt # Ledger history & transaction chips
│   │       ├── ReportsScreen.kt      # Analytics overview & exports
│   │       └── SettingsScreen.kt     # Theme toggle & business config
│   ├── data/
│   │   ├── local/
│   │   │   ├── CustomerEntity.kt     # Room entity for customer accounts
│   │   │   ├── TransactionEntity.kt  # Room entity for transactions
│   │   │   ├── CustomerDao.kt        # Room DAO queries
│   │   │   ├── TransactionDao.kt     # Room DAO queries
│   │   │   └── KhataDatabase.kt      # Room SQLite Database Instance
│   │   └── remote/
│   │       └── SupabaseClientProvider.kt # Supabase cloud sync foundation
│   └── services/
│       └── GeminiKhataAssistant.kt   # Gemini AI service integration
```

---

## ⚙️ Configuration & Secrets

### 🔑 Gemini AI Secret Configuration
To enable AI smart insights:
1. Open the **Secrets** panel in AI Studio.
2. Set `GEMINI_API_KEY` to your Gemini API key.
3. `BuildConfig.GEMINI_API_KEY` will automatically inject it at build time.

### ☁️ Supabase Configuration
Update `SupabaseClientProvider.kt` with your Supabase project URL and anon key for cloud backups:
```kotlin
const val SUPABASE_URL = "https://your-project.supabase.co"
const val SUPABASE_ANON_KEY = "your-anon-key"
```

---

## 🛠️ Building & Verification

- **Compile Applet**: Build the app binary using the `compile_applet` tool.
- **Test Execution**: Run unit tests via `gradle :app:testDebugUnitTest`.

---

© 2026 Alhaaj Khata AI — Mobile-First Digital Khata Application
