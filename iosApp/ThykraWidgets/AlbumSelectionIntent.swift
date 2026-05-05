//
//  AlbumSelectionIntent.swift
//  ThykraWidgets
//
//  AppIntent (iOS 17+) used to configure widgets. Lets the user pick which
//  album to display. The dynamic options list is fetched from the server via
//  `WidgetApiClient`. We use AppIntent (not the older SiriKit IntentDefinition)
//  because it's the modern path and avoids needing an .intentdefinition file
//  in the Xcode project.
//

import AppIntents
import WidgetKit

// MARK: - AppEntity for an album

struct AlbumEntity: AppEntity, Identifiable, Hashable {
    let id: String
    let title: String

    static var typeDisplayRepresentation: TypeDisplayRepresentation {
        TypeDisplayRepresentation(name: "Album")
    }

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(title)")
    }

    static var defaultQuery = AlbumQuery()
}

// MARK: - Query that powers the picker

struct AlbumQuery: EntityQuery {
    func entities(for identifiers: [AlbumEntity.ID]) async throws -> [AlbumEntity] {
        // Fetch full list; map by id. For a small library this is fine.
        let albums = (try? await WidgetApiClient().listAlbums()) ?? []
        let byId = Dictionary(uniqueKeysWithValues: albums.map { ($0.id, AlbumEntity(id: $0.id, title: $0.title)) })
        return identifiers.compactMap { byId[$0] }
    }

    func suggestedEntities() async throws -> [AlbumEntity] {
        let albums = (try? await WidgetApiClient().listAlbums()) ?? []
        return albums.map { AlbumEntity(id: $0.id, title: $0.title) }
    }

    func defaultResult() async -> AlbumEntity? {
        // First album in the list is the default — matches the
        // "user's most recent album" behaviour described in the spec.
        let albums = (try? await WidgetApiClient().listAlbums()) ?? []
        return albums.first.map { AlbumEntity(id: $0.id, title: $0.title) }
    }
}

// MARK: - Configuration Intent

struct AlbumSelectionIntent: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "Choose Album"
    static var description = IntentDescription("Pick which album the widget should track.")

    @Parameter(title: "Album")
    var album: AlbumEntity?

    init() {}

    init(album: AlbumEntity?) {
        self.album = album
    }
}
