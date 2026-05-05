//
//  WidgetApiClient.swift
//  ThykraWidgets
//
//  Lightweight network client used from the WidgetKit extension. Widgets run
//  in a tightly-memory-constrained, short-lived process — pulling in the
//  full ComposeApp/Kotlin framework just to make a couple of GETs is overkill,
//  so we duplicate the minimum DTO shapes here as plain `Codable` structs that
//  mirror the server's `ApiResponse<T>` envelope.
//
//  JWT comes from the App Group's shared UserDefaults, written by
//  `IosTokenStorage` in the main app (see TokenStorage.ios.kt).
//

import Foundation

// MARK: - DTOs (mirror shared/.../model/*.kt)

struct ApiEnvelope<T: Decodable>: Decodable {
    let success: Bool
    let data: T?
    let error: String?
}

struct WidgetAlbumMemberSummary: Decodable, Hashable {
    let userId: String
    let displayName: String
    let avatarUrl: String?
}

struct WidgetAlbum: Decodable, Hashable, Identifiable {
    let id: String
    let ownerId: String
    let title: String
    let description: String?
    let coverUrl: String?
    let memberCount: Int
    let previewMembers: [WidgetAlbumMemberSummary]?
    let createdAt: String
}

struct WidgetMedia: Decodable, Hashable, Identifiable {
    let id: String
    let albumId: String
    let uploaderId: String
    let type: String        // "PHOTO" | "VIDEO"
    let status: String      // "PENDING" | "ACTIVE"
    let url: String
    let thumbnailUrl: String?
    let filename: String
    let takenAt: String?
    let uploadedAt: String
}

struct WidgetReaction: Decodable, Hashable, Identifiable {
    let id: String
    let mediaId: String
    let userId: String
    let userDisplayName: String
    let userAvatarUrl: String?
    let type: String
    let createdAt: String
}

struct WidgetMediaReactions: Decodable {
    let summary: [WidgetReactionSummary]?
    let reactions: [WidgetReaction]
}

struct WidgetReactionSummary: Decodable, Hashable {
    let type: String
    let count: Int
    let reactedByMe: Bool
}

struct WidgetComment: Decodable, Hashable, Identifiable {
    let id: String
    let mediaId: String
    let authorId: String
    let authorDisplayName: String
    let authorAvatarUrl: String?
    let body: String
    let createdAt: String
    let updatedAt: String
}

// MARK: - Activity feed item (computed locally from reactions + comments)

enum WidgetActivityKind: String {
    case reaction
    case comment
}

struct WidgetActivityItem: Hashable, Identifiable {
    let id: String
    let kind: WidgetActivityKind
    let actorName: String
    let summary: String
    let albumId: String
    let mediaId: String
    let createdAt: Date
}

// MARK: - Client

enum WidgetApiError: Error {
    case notAuthenticated
    case http(Int)
    case decoding
}

struct WidgetApiClient {
    /// Must match `Constants.kt` SERVER_PORT and Platform.ios.kt API_HOST.
    /// iOS simulator/device can both reach `localhost` when the server is
    /// running on the same machine; for real-device usage the host needs
    /// a LAN IP — that is a deployment concern, out of scope here.
    static let baseURL = "http://localhost:8081"
    static let appGroup = "group.com.jameeli.thykra"
    static let tokenKey = "auth_token"

    private let session: URLSession
    private let decoder: JSONDecoder

    init() {
        let config = URLSessionConfiguration.ephemeral
        config.timeoutIntervalForRequest = 10
        config.timeoutIntervalForResource = 15
        self.session = URLSession(configuration: config)
        self.decoder = JSONDecoder()
    }

    private func token() -> String? {
        UserDefaults(suiteName: Self.appGroup)?.string(forKey: Self.tokenKey)
    }

    private func authedRequest(path: String) throws -> URLRequest {
        guard let token = token(), !token.isEmpty else {
            throw WidgetApiError.notAuthenticated
        }
        guard let url = URL(string: Self.baseURL + path) else {
            throw WidgetApiError.http(-1)
        }
        var req = URLRequest(url: url)
        req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        req.setValue("application/json", forHTTPHeaderField: "Accept")
        return req
    }

    private func get<T: Decodable>(_ path: String) async throws -> T {
        let req = try authedRequest(path: path)
        let (data, resp) = try await session.data(for: req)
        guard let http = resp as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw WidgetApiError.http((resp as? HTTPURLResponse)?.statusCode ?? -1)
        }
        let env = try decoder.decode(ApiEnvelope<T>.self, from: data)
        guard let payload = env.data, env.success else {
            throw WidgetApiError.decoding
        }
        return payload
    }

    // MARK: - Endpoints

    func listAlbums() async throws -> [WidgetAlbum] {
        try await get("/api/albums")
    }

    func listMedia(albumId: String) async throws -> [WidgetMedia] {
        try await get("/api/albums/\(albumId)/media")
    }

    func reactions(albumId: String, mediaId: String) async throws -> WidgetMediaReactions {
        try await get("/api/albums/\(albumId)/media/\(mediaId)/reactions")
    }

    func comments(albumId: String, mediaId: String) async throws -> [WidgetComment] {
        try await get("/api/albums/\(albumId)/media/\(mediaId)/comments")
    }

    // MARK: - Image fetch (for the latest-photo widget)

    /// Local dev URLs from the server come back relative-ish (e.g. starting
    /// with `/api/media/files/...`) when storage.baseUrl isn't absolute.
    /// Prefix with `baseURL` if needed before downloading.
    func absolutize(_ url: String) -> URL? {
        if url.hasPrefix("http://") || url.hasPrefix("https://") {
            return URL(string: url)
        }
        let prefix = url.hasPrefix("/") ? "" : "/"
        return URL(string: Self.baseURL + prefix + url)
    }

    func downloadImage(_ url: String) async -> Data? {
        guard let u = absolutize(url) else { return nil }
        var req = URLRequest(url: u)
        if let token = token() { req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization") }
        do {
            let (data, resp) = try await session.data(for: req)
            guard let http = resp as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
                return nil
            }
            return data
        } catch {
            return nil
        }
    }
}

// MARK: - ISO8601 helper

enum WidgetDateParser {
    static let formatter: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()
    static let formatterNoFraction: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime]
        return f
    }()

    static func parse(_ s: String?) -> Date? {
        guard let s = s else { return nil }
        return formatter.date(from: s) ?? formatterNoFraction.date(from: s)
    }
}
