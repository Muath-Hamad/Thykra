package com.jameeli.thykra

/**
 * True in a debug binary. Gates the kit gallery route, which is a development surface and
 * must never ship: the design's acceptance for build step 02 is that the gallery matches
 * part 2 board for board, not that anyone can reach it from the app.
 */
expect val KitGalleryEnabled: Boolean
