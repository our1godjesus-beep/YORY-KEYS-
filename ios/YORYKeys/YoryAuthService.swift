import Foundation
import Security

enum YoryAuthResult {
    case signedIn(email: String)
    case needsEmailVerification(email: String)
}

enum YoryAuthError: LocalizedError {
    case configurationMissing
    case invalidResponse
    case requestFailed(String)
    case secureStorageFailed

    var errorDescription: String? {
        switch self {
        case .configurationMissing:
            return "YORY account service is not connected yet. Add the YORY Supabase URL and publishable key to the app's SupabaseConfig.plist."
        case .invalidResponse:
            return "The account service returned an unexpected response."
        case .requestFailed(let message):
            return message
        case .secureStorageFailed:
            return "Secure session storage is unavailable on this device."
        }
    }
}

private struct YoryAuthSession: Codable {
    let accessToken: String
    let refreshToken: String
    let expiresAt: TimeInterval
    let email: String
}

enum YoryAuthService {
    private static let keychainService = "app.yory.keys.auth"
    private static let keychainAccount = "supabase.session"

    private static var configuration: (url: String, key: String)? {
        var values: [String: Any]?
        if let url = Bundle.main.url(forResource: "SupabaseConfig", withExtension: "plist"),
           let dictionary = NSDictionary(contentsOf: url) as? [String: Any] {
            values = dictionary
        } else {
            values = Bundle.main.infoDictionary
        }

        guard
            let rawURL = values?["SUPABASE_URL"] as? String,
            let rawKey = values?["SUPABASE_PUBLISHABLE_KEY"] as? String
        else { return nil }

        let url = rawURL.trimmingCharacters(in: .whitespacesAndNewlines).trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let key = rawKey.trimmingCharacters(in: .whitespacesAndNewlines)
        guard url.hasPrefix("https://"), !key.isEmpty else { return nil }
        return (url, key)
    }

    static func signUp(displayName: String, email: String, password: String) async throws -> YoryAuthResult {
        let response = try await post(
            path: "/auth/v1/signup",
            body: [
                "email": email.trimmingCharacters(in: .whitespacesAndNewlines),
                "password": password,
                "data": ["display_name": displayName.trimmingCharacters(in: .whitespacesAndNewlines)]
            ]
        )

        if let session = try session(from: response, fallbackEmail: email) {
            try save(session)
            return .signedIn(email: session.email)
        }
        return .needsEmailVerification(email: email)
    }

    static func signIn(email: String, password: String) async throws -> YoryAuthResult {
        let response = try await post(
            path: "/auth/v1/token?grant_type=password",
            body: [
                "email": email.trimmingCharacters(in: .whitespacesAndNewlines),
                "password": password
            ]
        )
        guard let session = try session(from: response, fallbackEmail: email) else {
            throw YoryAuthError.invalidResponse
        }
        try save(session)
        return .signedIn(email: session.email)
    }

    static func restoreSession() async -> Bool {
        guard let current = loadSession() else { return false }
        if current.expiresAt > Date().timeIntervalSince1970 + 60 { return true }

        do {
            let response = try await post(
                path: "/auth/v1/token?grant_type=refresh_token",
                body: ["refresh_token": current.refreshToken]
            )
            guard let refreshed = try session(from: response, fallbackEmail: current.email) else {
                throw YoryAuthError.invalidResponse
            }
            try save(refreshed)
            return true
        } catch {
            clearSession()
            return false
        }
    }

    static func signOut() async {
        if let current = loadSession() {
            _ = try? await post(
                path: "/auth/v1/logout",
                body: [:],
                bearer: current.accessToken
            )
        }
        clearSession()
    }

    private static func post(
        path: String,
        body: [String: Any],
        bearer: String? = nil
    ) async throws -> [String: Any] {
        guard let config = configuration else { throw YoryAuthError.configurationMissing }
        guard let url = URL(string: config.url + path) else { throw YoryAuthError.configurationMissing }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = 20
        request.setValue(config.key, forHTTPHeaderField: "apikey")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let bearer, !bearer.isEmpty {
            request.setValue("Bearer " + bearer, forHTTPHeaderField: "Authorization")
        }
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse else { throw YoryAuthError.invalidResponse }
            let payload = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] ?? [:]
            guard (200...299).contains(http.statusCode) else {
                let message = (payload["msg"] as? String)
                    ?? (payload["message"] as? String)
                    ?? (payload["error_description"] as? String)
                    ?? (payload["error"] as? String)
                    ?? "The account service rejected the request (HTTP \(http.statusCode))."
                throw YoryAuthError.requestFailed(message)
            }
            return payload
        } catch let error as YoryAuthError {
            throw error
        } catch {
            throw YoryAuthError.requestFailed(error.localizedDescription)
        }
    }

    private static func session(from payload: [String: Any], fallbackEmail: String) throws -> YoryAuthSession? {
        guard
            let access = payload["access_token"] as? String,
            let refresh = payload["refresh_token"] as? String
        else { return nil }

        let expiresIn = payload["expires_in"] as? TimeInterval ?? 3600
        let user = payload["user"] as? [String: Any]
        let email = user?["email"] as? String ?? fallbackEmail
        return YoryAuthSession(
            accessToken: access,
            refreshToken: refresh,
            expiresAt: Date().timeIntervalSince1970 + expiresIn,
            email: email
        )
    }

    private static func save(_ session: YoryAuthSession) throws {
        let data = try JSONEncoder().encode(session)
        let baseQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: keychainAccount
        ]
        SecItemDelete(baseQuery as CFDictionary)

        var attributes = baseQuery
        attributes[kSecValueData as String] = data
        attributes[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        guard SecItemAdd(attributes as CFDictionary, nil) == errSecSuccess else {
            throw YoryAuthError.secureStorageFailed
        }
    }

    private static func loadSession() -> YoryAuthSession? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: keychainAccount,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data else { return nil }
        return try? JSONDecoder().decode(YoryAuthSession.self, from: data)
    }

    private static func clearSession() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: keychainService,
            kSecAttrAccount as String: keychainAccount
        ]
        SecItemDelete(query as CFDictionary)
    }
}
