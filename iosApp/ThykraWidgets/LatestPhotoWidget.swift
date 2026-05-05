//
//  LatestPhotoWidget.swift
//  ThykraWidgets
//
//  Shows the most recently uploaded photo from the configured album (or the
//  user's most recent album when no configuration is supplied).
//
//  Tap target: deep-links into the iOS app at the underlying media via
//  `widgetURL(thykra://album/<albumId>/media/<mediaId>)`.
//

import SwiftUI
import WidgetKit

// MARK: - Timeline entry

struct LatestPhotoEntry: TimelineEntry {
    let date: Date
    let configuration: AlbumSelectionIntent
    let albumId: String?
    let albumTitle: String
    let mediaId: String?
    let imageData: Data?
    let takenAt: Date?
    /// Non-nil when we couldn't load anything — used to render the placeholder.
    let errorMessage: String?
}

// MARK: - Provider

struct LatestPhotoProvider: AppIntentTimelineProvider {
    typealias Entry = LatestPhotoEntry
    typealias Intent = AlbumSelectionIntent

    func placeholder(in context: Context) -> LatestPhotoEntry {
        LatestPhotoEntry(
            date: Date(),
            configuration: AlbumSelectionIntent(),
            albumId: nil,
            albumTitle: "Thykra",
            mediaId: nil,
            imageData: nil,
            takenAt: nil,
            errorMessage: nil
        )
    }

    func snapshot(for configuration: AlbumSelectionIntent, in context: Context) async -> LatestPhotoEntry {
        await fetch(configuration: configuration)
    }

    func timeline(for configuration: AlbumSelectionIntent, in context: Context) async -> Timeline<LatestPhotoEntry> {
        let entry = await fetch(configuration: configuration)
        // Refresh every 30 minutes — widgets are budget-limited; this is plenty
        // for "latest photo" semantics. iOS may extend the interval if the
        // widget isn't viewed often.
        let next = Date().addingTimeInterval(30 * 60)
        return Timeline(entries: [entry], policy: .after(next))
    }

    private func fetch(configuration: AlbumSelectionIntent) async -> LatestPhotoEntry {
        let api = WidgetApiClient()
        do {
            // Resolve target album: configured one wins, otherwise first album.
            let album: WidgetAlbum
            if let configured = configuration.album {
                let albums = try await api.listAlbums()
                guard let match = albums.first(where: { $0.id == configured.id }) else {
                    return errorEntry("Album not found", configuration: configuration)
                }
                album = match
            } else {
                let albums = try await api.listAlbums()
                guard let first = albums.first else {
                    return errorEntry("No albums yet", configuration: configuration)
                }
                album = first
            }

            let media = try await api.listMedia(albumId: album.id)
            let photos = media.filter { $0.type == "PHOTO" && $0.status == "ACTIVE" }
            guard let latest = photos.first else {
                return LatestPhotoEntry(
                    date: Date(),
                    configuration: configuration,
                    albumId: album.id,
                    albumTitle: album.title,
                    mediaId: nil,
                    imageData: nil,
                    takenAt: nil,
                    errorMessage: "No photos yet"
                )
            }

            // Prefer thumbnail to keep widget memory usage small.
            let imgUrl = latest.thumbnailUrl ?? latest.url
            let imageData = await api.downloadImage(imgUrl)
            let takenAt = WidgetDateParser.parse(latest.takenAt) ?? WidgetDateParser.parse(latest.uploadedAt)

            return LatestPhotoEntry(
                date: Date(),
                configuration: configuration,
                albumId: album.id,
                albumTitle: album.title,
                mediaId: latest.id,
                imageData: imageData,
                takenAt: takenAt,
                errorMessage: nil
            )
        } catch WidgetApiError.notAuthenticated {
            return errorEntry("Sign in to Thykra", configuration: configuration)
        } catch {
            return errorEntry("Couldn't load", configuration: configuration)
        }
    }

    private func errorEntry(_ message: String, configuration: AlbumSelectionIntent) -> LatestPhotoEntry {
        LatestPhotoEntry(
            date: Date(),
            configuration: configuration,
            albumId: configuration.album?.id,
            albumTitle: configuration.album?.title ?? "Thykra",
            mediaId: nil,
            imageData: nil,
            takenAt: nil,
            errorMessage: message
        )
    }
}

// MARK: - Entry view

struct LatestPhotoEntryView: View {
    var entry: LatestPhotoEntry

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            background
            // Bottom gradient scrim matches the web album-card vignette so
            // overlay text stays legible regardless of photo content.
            LinearGradient(
                colors: [.black.opacity(0.55), .clear],
                startPoint: .bottom,
                endPoint: .center
            )
            VStack(alignment: .leading, spacing: 2) {
                Text(entry.albumTitle)
                    .font(.headline)
                    .foregroundStyle(.white)
                    .lineLimit(1)
                if let taken = entry.takenAt {
                    Text(relativeDate(taken))
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.85))
                } else if let msg = entry.errorMessage {
                    Text(msg)
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.85))
                }
            }
            .padding(12)
        }
        .containerBackground(.thykraNavy.gradient, for: .widget)
        .widgetURL(deepLink)
    }

    @ViewBuilder
    private var background: some View {
        if let data = entry.imageData, let img = UIImage(data: data) {
            Image(uiImage: img)
                .resizable()
                .scaledToFill()
        } else {
            // Fallback gradient for empty/error states.
            LinearGradient(
                colors: [.thykraNavy, .thykraSky],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            VStack(spacing: 6) {
                Image(systemName: "photo.on.rectangle.angled")
                    .font(.system(size: 28, weight: .light))
                    .foregroundStyle(.white.opacity(0.85))
                if let msg = entry.errorMessage {
                    Text(msg)
                        .font(.caption)
                        .foregroundStyle(.white.opacity(0.9))
                }
            }
        }
    }

    private var deepLink: URL? {
        guard let albumId = entry.albumId else { return nil }
        if let mediaId = entry.mediaId {
            return URL(string: "thykra://album/\(albumId)/media/\(mediaId)")
        }
        return URL(string: "thykra://album/\(albumId)")
    }

    private func relativeDate(_ date: Date) -> String {
        let f = RelativeDateTimeFormatter()
        f.unitsStyle = .short
        return f.localizedString(for: date, relativeTo: Date())
    }
}

// MARK: - Theme colors (mirrors composeApp ThykraColors)

extension Color {
    static let thykraNavy = Color(red: 0x1A / 255.0, green: 0x1A / 255.0, blue: 0x2E / 255.0)
    static let thykraSky = Color(red: 0x1B / 255.0, green: 0x7F / 255.0, blue: 0xCC / 255.0)
    static let thykraSandy = Color(red: 0xF5 / 255.0, green: 0xE6 / 255.0, blue: 0xC8 / 255.0)
    static let thykraWarmWhite = Color(red: 0xFD / 255.0, green: 0xF8 / 255.0, blue: 0xEF / 255.0)
}

// MARK: - Widget

struct LatestPhotoWidget: Widget {
    let kind: String = "LatestPhotoWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: AlbumSelectionIntent.self,
            provider: LatestPhotoProvider()
        ) { entry in
            LatestPhotoEntryView(entry: entry)
        }
        .configurationDisplayName("Latest Photo")
        .description("Shows the most recent photo from your album.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

// MARK: - Previews

#Preview(as: .systemSmall) {
    LatestPhotoWidget()
} timeline: {
    LatestPhotoEntry(
        date: .now,
        configuration: AlbumSelectionIntent(),
        albumId: "album-1",
        albumTitle: "Iceland 2025",
        mediaId: "media-1",
        imageData: nil,
        takenAt: .now,
        errorMessage: nil
    )
}
