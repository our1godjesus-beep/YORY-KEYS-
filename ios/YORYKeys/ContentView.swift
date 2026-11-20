import SwiftUI

struct ContentView: View {
    var body: some View {
        NavigationStack {
            VStack(spacing: 18) {
                Text("YORY KEYS")
                    .font(.system(size: 34, weight: .bold, design: .rounded))
                Text("Your keyboard. Your universe.")
                    .foregroundStyle(.secondary)
                Image(systemName: "keyboard")
                    .font(.system(size: 52, weight: .light))
                    .padding(.vertical, 18)
                Text("Enable YORY KEYS in Settings → General → Keyboard → Keyboards.")
                    .multilineTextAlignment(.center)
                    .font(.callout)
                Text("The keyboard works without Full Access. Optional online features will explain their permissions before use.")
                    .multilineTextAlignment(.center)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            .padding(28)
            .navigationTitle("Welcome")
        }
    }
}
