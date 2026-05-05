package com.jameeli.thykra.permissions

import com.jameeli.thykra.model.AlbumVisibility
import com.jameeli.thykra.model.MemberRole

/**
 * Pure-function permission checks shared across server and clients. Mirrors the
 * authorisation rules enforced server-side so UIs can hide actions a user
 * cannot perform — not a substitute for the server checks.
 */
object AlbumPermissions {

    fun canEditAlbum(role: MemberRole?): Boolean = role == MemberRole.OWNER

    fun canDeleteAlbum(role: MemberRole?): Boolean = role == MemberRole.OWNER

    fun canManageMembers(role: MemberRole?): Boolean = role == MemberRole.OWNER

    fun canBlockMembers(role: MemberRole?): Boolean = role == MemberRole.OWNER

    fun canChangeVisibility(role: MemberRole?): Boolean = role == MemberRole.OWNER

    fun canCreateInviteLink(role: MemberRole?): Boolean =
        role == MemberRole.OWNER || role == MemberRole.CONTRIBUTOR

    fun canUploadMedia(role: MemberRole?): Boolean =
        role == MemberRole.OWNER || role == MemberRole.CONTRIBUTOR

    fun canDeleteMedia(role: MemberRole?, isUploader: Boolean): Boolean =
        role == MemberRole.OWNER || isUploader

    fun canReact(role: MemberRole?): Boolean = role != null

    fun canComment(role: MemberRole?): Boolean = role != null

    fun canEditComment(role: MemberRole?, isAuthor: Boolean): Boolean =
        role != null && isAuthor

    fun canDeleteComment(role: MemberRole?, isAuthor: Boolean): Boolean =
        role == MemberRole.OWNER || isAuthor

    fun canViewAlbum(role: MemberRole?, visibility: AlbumVisibility): Boolean =
        role != null || visibility == AlbumVisibility.LINK_SHARED
}
