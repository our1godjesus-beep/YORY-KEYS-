# iOS implementation

This directory contains the native Swift source foundation. Create an Xcode project with:
- Host app target: `YORYKeys` (SwiftUI).
- Keyboard extension target: `YORYKeyboard` (UIKit, `UIInputViewController`).
- App Group capability enabled on both targets with the same identifier, for example `group.app.yory.keys`.
- Extension point identifier: `com.apple.keyboard-service`.

The extension must work with Open Access disabled. Network-backed features are optional and must not be required for typing. iOS controls whether the keyboard can access the network and limits what text context is exposed.

The checked-in Swift files are source scaffolding; an Xcode project, signing team, provisioning, entitlements, and device testing are still required before installation.
