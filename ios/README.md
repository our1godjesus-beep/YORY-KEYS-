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

The extension declares `RequestsOpenAccess = false`. It does not request network access or Full Access in this first milestone. The host app writes theme settings to the App Group; the keyboard extension reads the settings available to it. Typing remains local. Any future cloud synchronization must be a separate, opt-in host-app feature and must never transmit live keystrokes.

The source is an initial functional slice, not a signed release. Build and test on a real iPhone; simulator success alone does not establish real-world keyboard behavior.
