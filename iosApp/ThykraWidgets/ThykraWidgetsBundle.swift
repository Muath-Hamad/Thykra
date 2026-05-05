//
//  ThykraWidgetsBundle.swift
//  ThykraWidgets
//
//  Entry point for the WidgetKit extension. Each widget exposed to the home
//  screen must be returned from `body` here. WidgetBundle is what iOS uses
//  to enumerate widgets when the user picks one from the gallery.
//

import SwiftUI
import WidgetKit

@main
struct ThykraWidgetsBundle: WidgetBundle {
    var body: some Widget {
        LatestPhotoWidget()
        RecentActivityWidget()
    }
}
