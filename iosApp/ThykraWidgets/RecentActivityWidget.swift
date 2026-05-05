//
//  RecentActivityWidget.swift
//  ThykraWidgets
//
//  Small feed of recent reactions + comments across the user's albums.
//
//  Backed by the aggregated `GET /api/activity/recent` server endpoint —
//  one network call per refresh, regardless of how many albums or media
//  items the user has.
//

import SwiftUI
import WidgetKit

// MARK: - Timeline entry

struct RecentActivityEntry: TimelineEntry {
    let date: Date
    let configuration: AlbumSelectionIntent
    let items: [WidgetActivityItem]
    let errorMessage: String?
}

// MARK: - Provider

struct RecentActivityProvider: AppIntentTimelineProvider {
    typealias Entry = RecentActivityEntry
    typealias Intent = AlbumSelectionIntent

    /// Server clamps to 50; we ask for enough to fill the largest family.
    private let fetchLimit = 25
    private let maxItemsShown = 8

    func placeholder(in context: Context) -> RecentActivityEntry {
        RecentActivityEntry(
            date: Date(),
            configuration: AlbumSelectionIntent(),
            items: [],
            errorMessage: nil
        )
    }

    func snapshot(for configuration: AlbumSelectionIntent, in context: Context) async -> RecentActivityEntry {
        await fetch(configuration: configuration)
    }

    func timeline(for configuration: AlbumSelectionIntent, in context: Context) async -> Timeline<RecentActivityEntry> {
        let entry = await fetch(configuration: configuration)
        let next = Date().addingTimeInterval(20 * 60)
        return Timeline(entries: [entry], policy: .after(next))
    }

    private func fetch(configuration: AlbumSelectionIntent) async -> RecentActivityEntry {
        let api = WidgetApiClient()
        do {
            let feed = try await api.recentActivity(limit: fetchLimit)

            // If the user pinned an album in widget config, scope to that album;
            // otherwise show activity across every album they belong to.
            let scoped: [WidgetActivityItemDto]
            if let configuredId = configuration.album?.id {
                scoped = feed.items.filter { $0.albumId == configuredId }
            } else {
                scoped = feed.items
            }

            let mapped = scoped.compactMap { dto -> WidgetActivityItem? in
                guard let when = WidgetDateParser.parse(dto.createdAt) else { return nil }
                let kind: WidgetActivityKind = (dto.type == .REACTION) ? .reaction : .comment
                let summary: String = {
                    switch dto.type {
                    case .REACTION:
                        return humanReaction(dto.reactionType ?? "")
                    case .COMMENT:
                        return dto.commentBody ?? ""
                    }
                }()
                return WidgetActivityItem(
                    id: "\(kind == .reaction ? "r" : "c")-\(dto.albumId)-\(dto.mediaId)-\(dto.actorId)-\(dto.createdAt)",
                    kind: kind,
                    actorName: dto.actorDisplayName,
                    summary: summary,
                    albumId: dto.albumId,
                    mediaId: dto.mediaId,
                    createdAt: when
                )
            }

            let trimmed = Array(mapped.prefix(maxItemsShown))

            return RecentActivityEntry(
                date: Date(),
                configuration: configuration,
                items: trimmed,
                errorMessage: trimmed.isEmpty ? "No activity yet" : nil
            )
        } catch WidgetApiError.notAuthenticated {
            return RecentActivityEntry(
                date: Date(),
                configuration: configuration,
                items: [],
                errorMessage: "Sign in to Thykra"
            )
        } catch {
            return RecentActivityEntry(
                date: Date(),
                configuration: configuration,
                items: [],
                errorMessage: "Couldn't load"
            )
        }
    }

    private func humanReaction(_ type: String) -> String {
        switch type {
        case "LOVE": return "loved this"
        case "WOW": return "wow'd this"
        case "LAUGH": return "laughed"
        case "WISH_I_WAS_THERE": return "wishes they were there"
        case "WANDERLUST": return "wanderlusted"
        case "BEACH": return "beach mood"
        case "MOUNTAIN": return "mountain mood"
        case "FOOD": return "food mood"
        default: return "reacted"
        }
    }
}

// MARK: - Entry view

struct RecentActivityEntryView: View {
    @Environment(\.widgetFamily) var family
    var entry: RecentActivityEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text("Recent Activity")
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.thykraSandy)
                Spacer()
                Image(systemName: "bubble.left.and.bubble.right.fill")
                    .font(.caption)
                    .foregroundStyle(.thykraSandy.opacity(0.7))
            }

            if let msg = entry.errorMessage {
                Spacer()
                Text(msg)
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.85))
                Spacer()
            } else {
                ForEach(entry.items.prefix(rowLimit)) { item in
                    Link(destination: URL(string: "thykra://album/\(item.albumId)/media/\(item.mediaId)")!) {
                        ActivityRow(item: item)
                    }
                }
                Spacer(minLength: 0)
            }
        }
        .padding(12)
        .containerBackground(.thykraNavy.gradient, for: .widget)
        .widgetURL(URL(string: "thykra://albums"))
    }

    private var rowLimit: Int {
        switch family {
        case .systemSmall: return 2
        case .systemMedium: return 3
        case .systemLarge: return 6
        case .systemExtraLarge: return 8
        @unknown default: return 3
        }
    }
}

private struct ActivityRow: View {
    let item: WidgetActivityItem

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: item.kind == .reaction ? "heart.fill" : "text.bubble.fill")
                .font(.caption)
                .foregroundStyle(item.kind == .reaction ? .pink : .thykraSky)
                .frame(width: 14)
            VStack(alignment: .leading, spacing: 0) {
                Text(item.actorName)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                Text(item.summary)
                    .font(.caption2)
                    .foregroundStyle(.white.opacity(0.8))
                    .lineLimit(2)
            }
            Spacer(minLength: 0)
        }
    }
}

// MARK: - Widget

struct RecentActivityWidget: Widget {
    let kind: String = "RecentActivityWidget"

    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: kind,
            intent: AlbumSelectionIntent.self,
            provider: RecentActivityProvider()
        ) { entry in
            RecentActivityEntryView(entry: entry)
        }
        .configurationDisplayName("Recent Activity")
        .description("Latest reactions and comments across your albums.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

// MARK: - Previews

#Preview(as: .systemMedium) {
    RecentActivityWidget()
} timeline: {
    RecentActivityEntry(
        date: .now,
        configuration: AlbumSelectionIntent(),
        items: [
            WidgetActivityItem(id: "1", kind: .reaction, actorName: "Sara", summary: "loved this", albumId: "a", mediaId: "m", createdAt: .now),
            WidgetActivityItem(id: "2", kind: .comment, actorName: "Omar", summary: "Beautiful sunset!", albumId: "a", mediaId: "m", createdAt: .now.addingTimeInterval(-3600)),
        ],
        errorMessage: nil
    )
}
