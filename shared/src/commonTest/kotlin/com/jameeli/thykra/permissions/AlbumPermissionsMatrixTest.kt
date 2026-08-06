package com.jameeli.thykra.permissions

import com.jameeli.thykra.model.AlbumVisibility
import com.jameeli.thykra.model.MemberRole
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Exhaustive truth table over every role (including non-member) for each
 * permission check — a spec in test form. AlbumPermissionsTest covers the
 * per-role stories; this sweep guarantees no combination is left unpinned.
 */
class AlbumPermissionsMatrixTest {

    private val roles = listOf(MemberRole.OWNER, MemberRole.CONTRIBUTOR, MemberRole.VIEWER, null)

    private fun sweep(name: String, expectedByRole: Map<MemberRole?, Boolean>, check: (MemberRole?) -> Boolean) {
        roles.forEach { role ->
            assertEquals(expectedByRole.getValue(role), check(role), "$name(role=$role)")
        }
    }

    @Test
    fun owner_only_permissions() {
        val ownerOnly = mapOf<MemberRole?, Boolean>(
            MemberRole.OWNER to true,
            MemberRole.CONTRIBUTOR to false,
            MemberRole.VIEWER to false,
            null to false
        )
        sweep("canEditAlbum", ownerOnly, AlbumPermissions::canEditAlbum)
        sweep("canDeleteAlbum", ownerOnly, AlbumPermissions::canDeleteAlbum)
        sweep("canManageMembers", ownerOnly, AlbumPermissions::canManageMembers)
        sweep("canBlockMembers", ownerOnly, AlbumPermissions::canBlockMembers)
        sweep("canChangeVisibility", ownerOnly, AlbumPermissions::canChangeVisibility)
    }

    @Test
    fun contributor_level_permissions() {
        val ownerOrContributor = mapOf<MemberRole?, Boolean>(
            MemberRole.OWNER to true,
            MemberRole.CONTRIBUTOR to true,
            MemberRole.VIEWER to false,
            null to false
        )
        sweep("canCreateInviteLink", ownerOrContributor, AlbumPermissions::canCreateInviteLink)
        sweep("canUploadMedia", ownerOrContributor, AlbumPermissions::canUploadMedia)
    }

    @Test
    fun any_member_permissions() {
        val anyMember = mapOf<MemberRole?, Boolean>(
            MemberRole.OWNER to true,
            MemberRole.CONTRIBUTOR to true,
            MemberRole.VIEWER to true,
            null to false
        )
        sweep("canReact", anyMember, AlbumPermissions::canReact)
        sweep("canComment", anyMember, AlbumPermissions::canComment)
    }

    @Test
    fun delete_media_matrix() {
        // (role, isUploader) -> expected: owner always, otherwise only the uploader.
        val cases = listOf(
            Triple(MemberRole.OWNER, true, true),
            Triple(MemberRole.OWNER, false, true),
            Triple(MemberRole.CONTRIBUTOR, true, true),
            Triple(MemberRole.CONTRIBUTOR, false, false),
            Triple(MemberRole.VIEWER, true, true),
            Triple(MemberRole.VIEWER, false, false),
            Triple(null, true, true),
            Triple(null, false, false)
        )
        cases.forEach { (role, isUploader, expected) ->
            assertEquals(expected, AlbumPermissions.canDeleteMedia(role, isUploader), "canDeleteMedia(role=$role, isUploader=$isUploader)")
        }
    }

    @Test
    fun edit_comment_matrix() {
        // Only the author may edit, and only while still a member.
        val cases = listOf(
            Triple(MemberRole.OWNER, true, true),
            Triple(MemberRole.OWNER, false, false),
            Triple(MemberRole.CONTRIBUTOR, true, true),
            Triple(MemberRole.CONTRIBUTOR, false, false),
            Triple(MemberRole.VIEWER, true, true),
            Triple(MemberRole.VIEWER, false, false),
            Triple(null, true, false),
            Triple(null, false, false)
        )
        cases.forEach { (role, isAuthor, expected) ->
            assertEquals(expected, AlbumPermissions.canEditComment(role, isAuthor), "canEditComment(role=$role, isAuthor=$isAuthor)")
        }
    }

    @Test
    fun delete_comment_matrix() {
        // The owner moderates everything; authors can remove their own.
        val cases = listOf(
            Triple(MemberRole.OWNER, true, true),
            Triple(MemberRole.OWNER, false, true),
            Triple(MemberRole.CONTRIBUTOR, true, true),
            Triple(MemberRole.CONTRIBUTOR, false, false),
            Triple(MemberRole.VIEWER, true, true),
            Triple(MemberRole.VIEWER, false, false),
            Triple(null, true, true),
            Triple(null, false, false)
        )
        cases.forEach { (role, isAuthor, expected) ->
            assertEquals(expected, AlbumPermissions.canDeleteComment(role, isAuthor), "canDeleteComment(role=$role, isAuthor=$isAuthor)")
        }
    }

    @Test
    fun view_album_matrix() {
        val cases = listOf(
            Triple(MemberRole.OWNER, AlbumVisibility.PRIVATE, true),
            Triple(MemberRole.CONTRIBUTOR, AlbumVisibility.PRIVATE, true),
            Triple(MemberRole.VIEWER, AlbumVisibility.PRIVATE, true),
            Triple(null, AlbumVisibility.PRIVATE, false),
            Triple(MemberRole.OWNER, AlbumVisibility.LINK_SHARED, true),
            Triple(MemberRole.CONTRIBUTOR, AlbumVisibility.LINK_SHARED, true),
            Triple(MemberRole.VIEWER, AlbumVisibility.LINK_SHARED, true),
            Triple(null, AlbumVisibility.LINK_SHARED, true)
        )
        cases.forEach { (role, visibility, expected) ->
            assertEquals(expected, AlbumPermissions.canViewAlbum(role, visibility), "canViewAlbum(role=$role, visibility=$visibility)")
        }
    }
}
