package com.jameeli.thykra.permissions

import com.jameeli.thykra.model.AlbumVisibility
import com.jameeli.thykra.model.MemberRole
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlbumPermissionsTest {

    @Test
    fun owner_can_edit_delete_manage_block_change_visibility() {
        val role = MemberRole.OWNER
        assertTrue(AlbumPermissions.canEditAlbum(role))
        assertTrue(AlbumPermissions.canDeleteAlbum(role))
        assertTrue(AlbumPermissions.canManageMembers(role))
        assertTrue(AlbumPermissions.canBlockMembers(role))
        assertTrue(AlbumPermissions.canChangeVisibility(role))
        assertTrue(AlbumPermissions.canCreateInviteLink(role))
        assertTrue(AlbumPermissions.canUploadMedia(role))
    }

    @Test
    fun contributor_can_invite_and_upload_but_not_manage() {
        val role = MemberRole.CONTRIBUTOR
        assertFalse(AlbumPermissions.canEditAlbum(role))
        assertFalse(AlbumPermissions.canDeleteAlbum(role))
        assertFalse(AlbumPermissions.canManageMembers(role))
        assertFalse(AlbumPermissions.canBlockMembers(role))
        assertFalse(AlbumPermissions.canChangeVisibility(role))
        assertTrue(AlbumPermissions.canCreateInviteLink(role))
        assertTrue(AlbumPermissions.canUploadMedia(role))
    }

    @Test
    fun viewer_can_react_and_comment_but_not_upload() {
        val role = MemberRole.VIEWER
        assertFalse(AlbumPermissions.canCreateInviteLink(role))
        assertFalse(AlbumPermissions.canUploadMedia(role))
        assertTrue(AlbumPermissions.canReact(role))
        assertTrue(AlbumPermissions.canComment(role))
    }

    @Test
    fun non_member_can_do_nothing_protected() {
        val role: MemberRole? = null
        assertFalse(AlbumPermissions.canEditAlbum(role))
        assertFalse(AlbumPermissions.canCreateInviteLink(role))
        assertFalse(AlbumPermissions.canUploadMedia(role))
        assertFalse(AlbumPermissions.canReact(role))
        assertFalse(AlbumPermissions.canComment(role))
    }

    @Test
    fun delete_media_owner_or_uploader() {
        assertTrue(AlbumPermissions.canDeleteMedia(MemberRole.OWNER, isUploader = false))
        assertTrue(AlbumPermissions.canDeleteMedia(MemberRole.CONTRIBUTOR, isUploader = true))
        assertFalse(AlbumPermissions.canDeleteMedia(MemberRole.CONTRIBUTOR, isUploader = false))
        assertFalse(AlbumPermissions.canDeleteMedia(MemberRole.VIEWER, isUploader = false))
    }

    @Test
    fun edit_comment_only_author() {
        assertTrue(AlbumPermissions.canEditComment(MemberRole.CONTRIBUTOR, isAuthor = true))
        assertFalse(AlbumPermissions.canEditComment(MemberRole.OWNER, isAuthor = false))
        assertFalse(AlbumPermissions.canEditComment(null, isAuthor = true))
    }

    @Test
    fun delete_comment_author_or_owner() {
        assertTrue(AlbumPermissions.canDeleteComment(MemberRole.OWNER, isAuthor = false))
        assertTrue(AlbumPermissions.canDeleteComment(MemberRole.CONTRIBUTOR, isAuthor = true))
        assertFalse(AlbumPermissions.canDeleteComment(MemberRole.CONTRIBUTOR, isAuthor = false))
        assertFalse(AlbumPermissions.canDeleteComment(null, isAuthor = false))
    }

    @Test
    fun view_album_member_or_link_shared() {
        assertTrue(AlbumPermissions.canViewAlbum(MemberRole.VIEWER, AlbumVisibility.PRIVATE))
        assertTrue(AlbumPermissions.canViewAlbum(null, AlbumVisibility.LINK_SHARED))
        assertFalse(AlbumPermissions.canViewAlbum(null, AlbumVisibility.PRIVATE))
    }
}
