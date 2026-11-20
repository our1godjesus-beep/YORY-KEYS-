# iOS native implementation

YORY KEYS uses a SwiftUI host app and a UIKit Custom Keyboard Extension.

## Generate the Xcode project

Install XcodeGen, then run from this directory:

```bash
brew install xcodegen
xcodegen generate
open YORYKeys.xcodeproj
```

The generated project contains:
- `YORYKeys`: host app and local YORY Studio.
- `YORYKeyboard`: systemwide custom keyboard extension.

## Required signing setup

1. Select the host app target and set your Apple Developer Team.
2. Select the keyboard extension target and set the same Team.
3. Register and enable the App Group `group.app.yory.keys` for both targets in the Apple Developer account.
4. Confirm unique bundle identifiers before device installation.

## Privacy behavior

The extension declares `RequestsOpenAccess = true`, which makes the iOS “Allow Full Access” option available in Settings; it does not grant access automatically and the user may leave it off. Basic key input, deleting, space, return, layouts and bundled suggestions must remain usable without Full Access. In the current implementation, optional personal-word learning, clearing the App Group dictionary from the keyboard, shared preference writes and keyboard click audio require the user to enable Full Access. The keyboard does not send keystrokes or prediction context to a server. Explain these optional dependencies before asking users to enable Full Access. Any future cloud feature must be separately disclosed and opt-in.

The source is an initial functional slice, not a signed release. Build and test on a real iPhone; simulator success alone does not establish real-world keyboard behavior.
