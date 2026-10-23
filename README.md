<div align="center">
  <h1>InTouch</h1>
  <p><strong>A digital business card and contact-sharing Android application</strong></p>
  <p>Share your professional identity. Connect with people. Stay in touch.</p>
</div>

---

## Overview

**InTouch** is an Android application concept designed to make exchanging professional contact information more convenient through digital cards. Instead of relying only on physical business cards, users can create a digital identity and share or discover contact details through supported sharing workflows, including QR scanning and NFC-based sharing.

The application brings identity sharing and contact-related tools together in one interface, with features such as account access, a home dashboard, QR scanning, saved cards, scan history, chats, requests, and settings.

## Screenshots

### Authentication and account management

The authentication flow includes login, account creation, Google sign-in entry points, and password-reset screens.

<p align="center">
  <img src="docs/images/authentication-screens.png" alt="InTouch login, create account, and reset password screens" width="900" />
</p>

### Home dashboard, digital card, and QR scanner

The home screen provides quick access to digital-card creation, scan history, saved cards, and settings. The navigation also includes NFC sharing, chats, requests, and QR scanning.

<p align="center">
  <img src="docs/images/dashboard-and-qr-scanner.png" alt="InTouch home dashboard, NFC card-sharing concept, and QR scanner screen" width="900" />
</p>

## Key Features

- **Account access:** Login and account-creation interfaces.
- **Password recovery:** A dedicated flow to request a password-reset link.
- **Google sign-in:** A Google sign-in option shown in the authentication screens.
- **Digital cards:** Create and share a digital contact card.
- **QR code scanning:** Scan a QR code to access shared card information.
- **NFC sharing:** An interface entry point for NFC-based card sharing.
- **Saved cards:** A dedicated area for accessing saved contacts or cards.
- **Scan history:** A place to review recently scanned cards.
- **Chats and requests:** Navigation entry points for communication and connection requests.
- **Settings:** A dedicated area for managing app preferences.

> Feature availability may depend on the current implementation and device support. The list above reflects the app concept and the supplied screens; it should be updated as the codebase evolves.

## Technology

The project is an Android application. Update this section to match the libraries and services in the current repository.

| Area | Technology |
|---|---|
| Platform | Android |
| UI | Android app interface (see repository implementation) |
| Authentication | Confirm the provider and enabled methods in the source/configuration |
| Data storage | Confirm the database and storage services in the source/configuration |
| Sharing | QR scanning and NFC-based sharing workflows |

If the project uses Firebase Authentication, Cloud Firestore, or Firebase Storage, list the exact services and relevant libraries here once verified against the implementation.

## Getting Started

The steps below are a general Android project setup guide. Follow the repository's Gradle configuration for the exact requirements.

### Prerequisites

- Android Studio
- A compatible JDK version for the project's Gradle configuration
- Android SDK and an emulator or physical Android device
- Git

### Run locally

1. Clone the repository:

   ```bash
   git clone <YOUR_REPOSITORY_URL>
   ```

2. Open the cloned project in Android Studio.
3. Allow Gradle to sync and install any required SDK components.
4. Configure any required local properties, API keys, or service configuration files according to the project's setup instructions.
5. Select an emulator or connect an Android device with USB debugging enabled.
6. Build and run the app from Android Studio.

**Configuration note:** Do not commit passwords, signing keys, private service-account credentials, or other secrets. Document required configuration using placeholders or an example file instead.

## Suggested Project Structure

The actual structure depends on the repository. A typical Android project may look like this:

```text
InTouch/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/ or kotlin/
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   └── build.gradle or build.gradle.kts
├── docs/
│   └── images/
│       ├── authentication-screens.png
│       └── dashboard-and-qr-scanner.png
├── README.md
├── build.gradle or build.gradle.kts
└── settings.gradle or settings.gradle.kts
```

This is an illustrative structure, not a claim about the exact files in this repository.

## Security and Privacy Considerations

- Apply authentication and authorization checks to user-specific data.
- Restrict database and file-storage access with rules appropriate to the application's data model.
- Request camera and NFC-related permissions only when needed, and explain their purpose to users.
- Validate shared profile identifiers and URLs before opening or displaying content.
- Avoid exposing private contact information without the user's knowledge and consent.
- Keep API keys and signing credentials out of version control; review platform-specific guidance for any client-side configuration values.

## Testing Checklist

- [ ] Verify registration and login flows.
- [ ] Verify Google sign-in, if enabled in the project configuration.
- [ ] Verify password-reset requests and error states.
- [ ] Validate digital-card creation, editing, and display.
- [ ] Test QR scanning with valid, invalid, and unsupported QR codes.
- [ ] Test NFC sharing on compatible physical devices, if implemented.
- [ ] Verify saved-card and scan-history behavior.
- [ ] Test loading, empty, offline, and failure states.
- [ ] Check permission handling and accessibility on multiple screen sizes.
- [ ] Confirm user data access rules before release.

## Roadmap

Potential areas for continued development include:

- Improve digital-card customization and profile completeness.
- Add clear feedback for successful or failed sharing and scanning.
- Strengthen accessibility and support for different screen sizes.
- Expand automated tests for authentication and card-sharing flows.
- Add privacy controls for the information included on a shared card.
- Document release, backup, and deployment procedures.

## Contributing

Contributions and suggestions are welcome. For substantial changes, open an issue first to discuss the proposed improvement. Keep pull requests focused, include relevant test evidence, and update documentation when behavior changes.

## License

No license has been specified in this README. Add a `LICENSE` file and name the license here before allowing others to reuse or redistribute the project.

---

<div align="center">
  <sub>InTouch — making digital contact sharing simpler.</sub>
</div>
