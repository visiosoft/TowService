# Google Sign-In Integration for TruckTow App

## Overview
This document describes the Google Sign-In integration implemented in the TruckTow Android application.

## Features Implemented

### 1. Dependency Addition
- Added Google Sign-In dependency in `app/build.gradle`:
  ```gradle
  implementation 'com.google.android.gms:play-services-auth:21.0.0'
  ```

### 2. UI Components
- Added Google Sign-In button to login screen (`fragment_login.xml`)
- Created Google icon drawable (`ic_google.xml`)
- Added visual divider between regular login and Google Sign-In

### 3. MainActivity Configuration
- Configured GoogleSignInOptions with email request
- Added GoogleSignInClient initialization
- Provided access method for fragments

### 4. LoginFragment Integration
- Added Google Sign-In button click handler
- Implemented ActivityResultLauncher for handling sign-in results
- Added user information extraction (name, email, photo URL)
- Integrated with existing SessionManager for credential storage

### 5. Session Management
- Enhanced SessionManager to handle Google Sign-In users
- Added method to identify Google Sign-In users
- Updated auto-login logic to support Google Sign-In

## How It Works

### Sign-In Flow
1. User clicks "Sign in with Google" button
2. Google Sign-In intent is launched
3. User selects their Google account
4. App receives sign-in result with user information
5. User information is saved to SessionManager
6. User is navigated to home screen

### User Information Retrieved
- Display Name
- Email Address
- Profile Photo URL (if available)

### Session Management
- Google Sign-In users are identified by password "google_sign_in"
- Auto-login works for both regular and Google Sign-In users
- Session persistence across app restarts

## Code Structure

### Key Files Modified
1. `app/build.gradle` - Added dependency
2. `app/src/main/res/layout/fragment_login.xml` - Added UI components
3. `app/src/main/res/drawable/ic_google.xml` - Google icon
4. `app/src/main/java/com/mpo/trucktow/MainActivity.kt` - Google Sign-In configuration
5. `app/src/main/java/com/mpo/trucktow/fragments/LoginFragment.kt` - Sign-In logic
6. `app/src/main/java/com/mpo/trucktow/SessionManager.kt` - Enhanced session management

### Key Methods
- `configureGoogleSignIn()` - Sets up Google Sign-In options
- `startGoogleSignIn()` - Launches sign-in intent
- `handleGoogleSignInResult()` - Processes sign-in results
- `isGoogleSignIn()` - Identifies Google Sign-In users

## Testing

### Test Class
- `GoogleSignInTest.kt` - Utility class for testing Google Sign-In functionality
- Provides methods for sign-in, sign-out, and result handling

### Manual Testing Steps
1. Launch the app
2. Navigate to login screen
3. Click "Sign in with Google" button
4. Select a Google account
5. Verify successful sign-in and navigation to home screen
6. Restart app to test auto-login functionality

## Security Considerations
- Only email is requested (no additional scopes)
- User information is stored locally using SharedPreferences
- No sensitive data is transmitted to external servers
- Google Sign-In follows Google's security best practices

## Future Enhancements
- Add profile photo display in user interface
- Implement sign-out functionality
- Add additional Google account scopes if needed
- Integrate with backend authentication system
- Add user profile management features

## Troubleshooting

### Common Issues
1. **Sign-In Fails**: Check internet connection and Google Play Services
2. **App Crashes**: Verify Google Sign-In dependency is properly added
3. **Auto-Login Issues**: Check SessionManager implementation

### Debug Information
- Check Logcat for "GoogleSignIn" tags
- Verify Google Sign-In configuration in MainActivity
- Test with different Google accounts

## Dependencies
- Google Play Services Auth: 21.0.0
- Compatible with Android API 24+ (minSdk 24)
- Requires Google Play Services on device 