import SwiftUI
import UIKit

struct YoryLaunchFlowView: View {
    @AppStorage("yory.launch.intro.completed") private var introCompleted = false
    @State private var authReady = false
    @State private var authenticated = false

    var body: some View {
        Group {
            if !authReady {
                ZStack {
                    yoryLaunchBackground
                    ProgressView().tint(Color(red: 0.66, green: 0.78, blue: 1.0))
                }
            } else if !introCompleted {
                YoryAnimatedIntroView {
                    introCompleted = true
                }
            } else if authenticated {
                ContentView(onSignOut: {
                    Task {
                        await YoryAuthService.signOut()
                        authenticated = false
                    }
                })
            } else {
                YoryAuthGateView {
                    authenticated = true
                }
            }
        }
        .preferredColorScheme(.dark)
        .task {
            authenticated = await YoryAuthService.restoreSession()
            authReady = true
        }
    }
}

private struct YoryAnimatedIntroView: View {
    let onContinue: () -> Void
    @State private var animateLogo = false
    @State private var didContinue = false

    private var logo: UIImage {
        UIImage(named: "LOGO7")
            ?? Bundle.main.path(forResource: "LOGO7", ofType: "jpeg").flatMap(UIImage.init(contentsOfFile:))
            ?? UIImage(systemName: "keyboard")!
    }

    var body: some View {
        ZStack {
            yoryLaunchBackground
            Circle()
                .fill(Color(red: 0.60, green: 0.75, blue: 1.0).opacity(0.18))
                .frame(width: 260, height: 260)
                .blur(radius: 65)
                .offset(x: 130, y: -210)
            Circle()
                .fill(Color(red: 0.56, green: 0.50, blue: 0.91).opacity(0.14))
                .frame(width: 250, height: 250)
                .blur(radius: 70)
                .offset(x: -120, y: 260)

            VStack(spacing: 20) {
                ZStack {
                    Circle()
                        .fill(.ultraThinMaterial)
                        .overlay(Circle().stroke(.white.opacity(0.34), lineWidth: 1))
                        .frame(width: 190, height: 190)
                        .shadow(color: Color(red: 0.62, green: 0.76, blue: 1.0).opacity(0.20), radius: 28, y: 10)
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [.white.opacity(0.24), .white.opacity(0.035)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .overlay(Circle().stroke(.white.opacity(0.22), lineWidth: 0.7))
                        .frame(width: 164, height: 164)
                    Image(uiImage: logo)
                        .resizable()
                        .scaledToFit()
                        .padding(28)
                        .frame(width: 148, height: 148)
                }
                .scaleEffect(animateLogo ? 1.0 : 0.78)
                .rotationEffect(.degrees(animateLogo ? 0 : -9))
                .opacity(animateLogo ? 1 : 0.2)

                Text("YORY KEYS")
                    .font(.system(size: 29, weight: .bold, design: .rounded))
                    .tracking(4)
                    .foregroundStyle(.white)
                Text("YOUR WORDS. YOUR FLOW.")
                    .font(.system(size: 11, weight: .semibold, design: .rounded))
                    .tracking(2.4)
                    .foregroundStyle(.white.opacity(0.66))

                Button(action: finishIntro) {
                    Text("GET STARTED")
                        .font(.system(size: 13, weight: .bold, design: .rounded))
                        .tracking(1.4)
                        .foregroundStyle(Color(red: 0.06, green: 0.09, blue: 0.16))
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(
                            LinearGradient(
                                colors: [Color(red: 0.78, green: 0.87, blue: 1.0), Color(red: 0.56, green: 0.70, blue: 0.98)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            in: RoundedRectangle(cornerRadius: 16)
                        )
                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(.white.opacity(0.45), lineWidth: 0.8))
                }
                .padding(.top, 18)
                .padding(.horizontal, 24)

                Text("A keyboard shaped around you")
                    .font(.footnote)
                    .foregroundStyle(.white.opacity(0.58))
            }
            .padding(.horizontal, 28)
        }
        .onAppear {
            withAnimation(.spring(response: 1.0, dampingFraction: 0.72)) {
                animateLogo = true
            }
        }
        .task {
            try? await Task.sleep(nanoseconds: 3_200_000_000)
            finishIntro()
        }
    }

    private func finishIntro() {
        guard !didContinue else { return }
        didContinue = true
        onContinue()
    }
}

private struct YoryAuthGateView: View {
    let onAuthenticated: () -> Void

    @State private var isRegister = true
    @State private var displayName = ""
    @State private var email = ""
    @State private var password = ""
    @State private var message = ""
    @State private var isInfo = false
    @State private var isLoading = false

    var body: some View {
        ZStack {
            yoryLaunchBackground
            Circle()
                .fill(Color.blue.opacity(0.14))
                .frame(width: 230, height: 230)
                .blur(radius: 65)
                .offset(x: 145, y: -260)

            ScrollView {
                VStack(spacing: 16) {
                    YorySmallBrand()
                    Text(isRegister ? "Create your YORY account" : "Welcome back")
                        .font(.system(size: 25, weight: .semibold, design: .rounded))
                        .foregroundStyle(.white)
                        .multilineTextAlignment(.center)
                    Text(isRegister
                         ? "Register first to personalize and sync your settings."
                         : "Sign in to continue to YORY Studio.")
                        .font(.system(size: 13))
                        .foregroundStyle(.white.opacity(0.65))
                        .multilineTextAlignment(.center)

                    VStack(spacing: 13) {
                        if isRegister {
                            authField("Display name", text: $displayName, contentType: .name)
                        }
                        authField("Email address", text: $email, contentType: .emailAddress)
                            .textInputAutocapitalization(.never)
                            .keyboardType(.emailAddress)
                            .autocorrectionDisabled()
                        SecureField("Password (at least 8 characters)", text: $password)
                            .textContentType(isRegister ? .newPassword : .password)
                            .textInputAutocapitalization(.never)
                            .padding(.horizontal, 15)
                            .frame(height: 52)
                            .background(.white.opacity(0.055), in: RoundedRectangle(cornerRadius: 14))
                            .overlay(RoundedRectangle(cornerRadius: 14).stroke(.white.opacity(0.20), lineWidth: 0.8))
                            .foregroundStyle(.white)

                        if !message.isEmpty {
                            Text(message)
                                .font(.footnote)
                                .foregroundStyle(isInfo ? Color(red: 0.68, green: 0.81, blue: 1.0) : Color(red: 1.0, green: 0.67, blue: 0.72))
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }

                        Button(action: submit) {
                            HStack {
                                if isLoading { ProgressView().tint(Color.black) }
                                Text(isLoading ? "PLEASE WAIT…" : (isRegister ? "CREATE ACCOUNT" : "SIGN IN"))
                            }
                            .font(.system(size: 13, weight: .bold, design: .rounded))
                            .tracking(1.1)
                            .foregroundStyle(Color(red: 0.06, green: 0.09, blue: 0.16))
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(
                                LinearGradient(
                                    colors: [Color(red: 0.78, green: 0.87, blue: 1.0), Color(red: 0.56, green: 0.70, blue: 0.98)],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                in: RoundedRectangle(cornerRadius: 16)
                            )
                        }
                        .disabled(!canSubmit || isLoading)
                        .opacity(canSubmit && !isLoading ? 1 : 0.52)

                        Button {
                            isRegister.toggle()
                            message = ""
                        } label: {
                            Text(isRegister ? "Already have an account? Sign in" : "New to YORY? Create an account")
                                .font(.footnote.weight(.medium))
                                .foregroundStyle(Color(red: 0.67, green: 0.80, blue: 1.0))
                        }
                        .padding(.top, 2)
                    }
                    .padding(18)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 24))
                    .overlay(RoundedRectangle(cornerRadius: 24).stroke(.white.opacity(0.20), lineWidth: 0.8))

                    Text("The keyboard is separate: after enabling YORY KEYS in iOS Settings, it can type offline without opening this app or signing in again.")
                        .font(.system(size: 11))
                        .lineSpacing(3)
                        .multilineTextAlignment(.center)
                        .foregroundStyle(.white.opacity(0.55))
                        .padding(.horizontal, 12)
                }
                .padding(.horizontal, 22)
                .padding(.vertical, 24)
                .frame(maxWidth: 520)
                .frame(maxWidth: .infinity)
            }
        }
    }

    private var canSubmit: Bool {
        email.contains("@") && password.count >= 8 && (!isRegister || !displayName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
    }

    private func authField(_ title: String, text: Binding<String>, contentType: UITextContentType) -> some View {
        TextField(title, text: text)
            .textContentType(contentType)
            .padding(.horizontal, 15)
            .frame(height: 52)
            .background(.white.opacity(0.055), in: RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(.white.opacity(0.20), lineWidth: 0.8))
            .foregroundStyle(.white)
    }

    private func submit() {
        guard canSubmit, !isLoading else { return }
        isLoading = true
        message = ""
        Task {
            defer { isLoading = false }
            do {
                let result: YoryAuthResult
                if isRegister {
                    result = try await YoryAuthService.signUp(
                        displayName: displayName,
                        email: email,
                        password: password
                    )
                } else {
                    result = try await YoryAuthService.signIn(email: email, password: password)
                }
                switch result {
                case .signedIn:
                    onAuthenticated()
                case .needsEmailVerification:
                    isInfo = true
                    message = "Check your email to verify your account, then return here and sign in."
                    isRegister = false
                    password = ""
                }
            } catch {
                isInfo = false
                message = error.localizedDescription
            }
        }
    }
}

private struct YorySmallBrand: View {
    var body: some View {
        VStack(spacing: 8) {
            ZStack {
                Circle().fill(.ultraThinMaterial)
                    .overlay(Circle().stroke(.white.opacity(0.30), lineWidth: 0.8))
                    .frame(width: 72, height: 72)
                Text("Y")
                    .font(.system(size: 37, weight: .light, design: .rounded))
                    .foregroundStyle(.white)
            }
            Text("YORY KEYS")
                .font(.system(size: 11, weight: .bold))
                .tracking(3)
                .foregroundStyle(Color(red: 0.68, green: 0.81, blue: 1.0))
        }
        .padding(.bottom, 6)
    }
}

private var yoryLaunchBackground: some View {
    LinearGradient(
        colors: [
            Color(red: 0.14, green: 0.18, blue: 0.28),
            Color(red: 0.025, green: 0.035, blue: 0.065),
            Color(red: 0.09, green: 0.09, blue: 0.16)
        ],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
    .ignoresSafeArea()
}

private func authFieldBackground() -> some View {
    RoundedRectangle(cornerRadius: 14)
}
