# Sdd Marketplace

## Project Overview

A production-ready Android marketplace app built with Kotlin, Jetpack Compose, and a Supabase backend.

### Features
- Authentication: Email/password, phone/OTP, guest sign-in
- Home Feed: Featured products, categories, infinite scroll with pagination
- Product Listings: Full CRUD — create, edit, delete, mark sold
- Product Detail: Image gallery, color selector, ratings/reviews, seller info
- Real-time Chat: Text, images, location messages, typing indicator, read receipts
- Push Notifications via Firebase Cloud Messaging (FCM)
- Wishlist, Search, Profile/Seller pages

### Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: Clean Architecture + MVVM
- **DI**: Hilt (Dagger)
- **Backend**: Supabase (Postgres, Auth, Realtime, Storage, Edge Functions)
- **Local DB**: Room + SQLCipher encryption
- **Networking**: Ktor (via Supabase SDK)
- **Images**: Coil
- **Paging**: Paging 3
- **Maps**: Google Maps Compose
- **Push Notifications**: Firebase Cloud Messaging
- **Background Sync**: WorkManager
- **Build System**: Gradle (Kotlin DSL) with Version Catalogs

### Project Structure
```
app/src/main/java/com/sdd/marketplace/
├── core/         DI modules, navigation, UI components, theme, utilities
├── data/         Room DB, DTOs, repository implementations
├── domain/       Domain models and repository interfaces
└── feature/      auth, chat, home, notifications, product, profile, search
supabase/
├── config.toml   Supabase local config
└── functions/    TypeScript Edge Functions (create-order, verify-payment)
supabase_schema.sql  Full DB schema with RLS policies
```

## Important Notes

This is a **native Android application**. It cannot be run or previewed in a browser. To develop and run this app you need:
1. Android Studio (or IntelliJ IDEA with Android plugin)
2. An Android device or emulator

## Setup Requirements

1. **Google Maps API Key** — add to `app/build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "MAPS_API_KEY", "\"YOUR_GOOGLE_MAPS_API_KEY\"")
   ```

2. **Supabase** — already configured:
   - Project URL: `https://fkeuioagahwqgpqjuwqj.supabase.co`
   - Anon key is set in `app/build.gradle.kts`
   - Run `supabase_schema.sql` in your Supabase SQL editor to set up the database

3. **Firebase** — place `google-services.json` in the `/app/` directory for FCM push notifications

4. **Build**:
   ```bash
   ./gradlew assembleDebug
   ```

## User Preferences

(Add preferences here as needed)
