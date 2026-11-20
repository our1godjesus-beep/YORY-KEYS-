import SwiftUI

private struct YoryToolbarOption: Identifiable {
    let id: String
    let title: String
    let detail: String
    let symbol: String
    let required: Bool
}

struct YoryToolbarCustomizationView: View {
    private let defaults = UserDefaults(suiteName: "group.app.yory.keys")
    private let options: [YoryToolbarOption] = [
        YoryToolbarOption(id: "emoji", title: "Emoji", detail: "Open emoji shortcuts", symbol: "face.smiling", required: false),
        YoryToolbarOption(id: "language", title: "Language", detail: "Change the active keyboard layout", symbol: "globe", required: false),
        YoryToolbarOption(id: "symbols", title: "Symbols", detail: "Switch to numbers and symbols", symbol: "character", required: false),
        YoryToolbarOption(id: "switch", title: "Next keyboard", detail: "Switch to another installed keyboard", symbol: "keyboard", required: true),
        YoryToolbarOption(id: "tools", title: "More tools", detail: "Open additional keyboard controls", symbol: "ellipsis", required: false)
    ]

    @State private var order: [String] = {
        let base = ["emoji", "language", "symbols", "switch", "tools"]
        let stored = UserDefaults(suiteName: "group.app.yory.keys")?.stringArray(forKey: "yory.toolbar.order") ?? base
        let valid = stored.filter { base.contains($0) }
        return valid + base.filter { !valid.contains($0) }
    }()
    @State private var visible: Set<String> = {
        let base = ["emoji", "language", "symbols", "switch", "tools"]
        let stored = UserDefaults(suiteName: "group.app.yory.keys")?.stringArray(forKey: "yory.toolbar.visible") ?? base
        return Set(stored.filter { base.contains($0) } + ["switch"])
    }()
    @State private var status = "Changes are stored on this device."

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.12, green: 0.16, blue: 0.25),
                    Color(red: 0.025, green: 0.035, blue: 0.065),
                    Color(red: 0.10, green: 0.09, blue: 0.17)
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            ).ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text("Keyboard toolbar")
                        .font(.system(size: 28, weight: .bold, design: .rounded))
                        .foregroundStyle(.white)
                    Text("Choose which shortcuts appear and arrange their order. The next-keyboard control stays available so you can always switch back to another keyboard.")
                        .font(.footnote)
                        .foregroundStyle(.white.opacity(0.66))

                    ForEach(Array(order.enumerated()), id: \.element) { index, id in
                        if let option = options.first(where: { $0.id == id }) {
                            VStack(alignment: .leading, spacing: 10) {
                                HStack(spacing: 12) {
                                    Image(systemName: option.symbol)
                                        .font(.system(size: 19, weight: .medium))
                                        .foregroundStyle(Color(red: 0.65, green: 0.78, blue: 1.0))
                                        .frame(width: 42, height: 42)
                                        .background(.white.opacity(0.07), in: RoundedRectangle(cornerRadius: 12))
                                    VStack(alignment: .leading, spacing: 3) {
                                        Text(option.title).font(.subheadline.weight(.semibold)).foregroundStyle(.white)
                                        Text(option.detail).font(.caption).foregroundStyle(.white.opacity(0.62))
                                    }
                                    Spacer()
                                    Toggle(
                                        option.title,
                                        isOn: Binding(
                                            get: { visible.contains(id) || option.required },
                                            set: { enabled in
                                                guard !option.required else { return }
                                                if enabled { visible.insert(id) } else { visible.remove(id) }
                                                persist()
                                            }
                                        )
                                    )
                                    .labelsHidden()
                                    .disabled(option.required)
                                }
                                HStack {
                                    Text("Position \(index + 1)").font(.caption).foregroundStyle(.white.opacity(0.62))
                                    Spacer()
                                    Button {
                                        move(id, offset: -1)
                                    } label: {
                                        Image(systemName: "arrow.up").frame(width: 40, height: 36)
                                    }
                                    .disabled(index == 0)
                                    Button {
                                        move(id, offset: 1)
                                    } label: {
                                        Image(systemName: "arrow.down").frame(width: 40, height: 36)
                                    }
                                    .disabled(index == order.count - 1)
                                }
                                .buttonStyle(.bordered)
                                .tint(Color(red: 0.65, green: 0.78, blue: 1.0))
                            }
                            .padding(13)
                            .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
                            .overlay(RoundedRectangle(cornerRadius: 16).stroke(.white.opacity(0.16), lineWidth: 0.8))
                        }
                    }

                    Text(status).font(.footnote).foregroundStyle(Color(red: 0.65, green: 0.78, blue: 1.0))
                    Button("Reset toolbar") {
                        order = options.map(\.id)
                        visible = Set(options.map(\.id))
                        persist()
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 14))
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(.white.opacity(0.18), lineWidth: 0.8))
                    .foregroundStyle(.white)
                }
                .padding(.horizontal, 16)
                .padding(.top, 18)
                .padding(.bottom, 28)
            }
        }
        .navigationTitle("Keyboard toolbar")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { load() }
    }

    private func move(_ id: String, offset: Int) {
        guard let index = order.firstIndex(of: id) else { return }
        let target = index + offset
        guard target >= 0 && target < order.count else { return }
        order.swapAt(index, target)
        persist()
    }

    private func persist() {
        let safeVisible = visible.union(["switch"])
        defaults?.set(order, forKey: "yory.toolbar.order")
        defaults?.set(Array(safeVisible).sorted(), forKey: "yory.toolbar.visible")
        status = "Toolbar saved. Reopen the keyboard to see the updated order."
    }

    private func load() {
        let base = options.map(\.id)
        let storedOrder = defaults?.stringArray(forKey: "yory.toolbar.order") ?? base
        let validOrder = storedOrder.filter { base.contains($0) }
        order = validOrder + base.filter { !validOrder.contains($0) }
        let storedVisible = defaults?.stringArray(forKey: "yory.toolbar.visible") ?? base
        visible = Set(storedVisible.filter { base.contains($0) } + ["switch"])
    }
}
