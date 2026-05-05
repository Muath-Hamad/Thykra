import { useEffect, useRef, useState } from 'react';
import {
  CommentDto,
  MAX_COMMENT_LENGTH,
  addComment,
  deleteComment,
  getComments,
  updateComment,
} from '../api/comments';

interface CommentThreadProps {
  albumId: string;
  mediaId: string;
  currentUserId: string | null;
  albumOwnerId: string | null;
}

function formatRelative(iso: string): string {
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return '';
  const diffMs = Date.now() - then;
  const sec = Math.max(0, Math.floor(diffMs / 1000));
  if (sec < 60) return 'just now';
  const min = Math.floor(sec / 60);
  if (min < 60) return `${min}m ago`;
  const hr = Math.floor(min / 60);
  if (hr < 24) return `${hr}h ago`;
  const day = Math.floor(hr / 24);
  if (day < 7) return `${day}d ago`;
  return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

function initials(name: string): string {
  const parts = name.trim().split(/\s+/);
  if (parts.length === 0 || !parts[0]) return '?';
  if (parts.length === 1) return parts[0].slice(0, 1).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export function CommentThread({
  albumId,
  mediaId,
  currentUserId,
  albumOwnerId,
}: CommentThreadProps) {
  const [comments, setComments] = useState<CommentDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [draft, setDraft] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editingDraft, setEditingDraft] = useState('');
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setComments([]);
    setError(null);
    getComments(albumId, mediaId)
      .then((resp) => {
        if (cancelled) return;
        if (resp?.success && resp.data) {
          setComments(resp.data as CommentDto[]);
        }
      })
      .catch((err) => {
        if (cancelled) return;
        console.error('Failed to load comments:', err);
        setError('Could not load comments');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [albumId, mediaId]);

  async function handleSubmit() {
    const body = draft.trim();
    if (!body || submitting) return;
    setSubmitting(true);
    setError(null);
    try {
      const resp = await addComment(albumId, mediaId, body);
      if (resp?.success && resp.data) {
        setComments((prev) => [...prev, resp.data as CommentDto]);
        setDraft('');
      } else {
        setError(resp?.error ?? 'Failed to post comment');
      }
    } catch (err) {
      console.error('Failed to add comment:', err);
      setError('Failed to post comment');
    } finally {
      setSubmitting(false);
    }
  }

  function startEdit(c: CommentDto) {
    setEditingId(c.id);
    setEditingDraft(c.body);
  }

  function cancelEdit() {
    setEditingId(null);
    setEditingDraft('');
  }

  async function handleSaveEdit(commentId: string) {
    const body = editingDraft.trim();
    if (!body) return;
    setSubmitting(true);
    setError(null);
    try {
      const resp = await updateComment(albumId, mediaId, commentId, body);
      if (resp?.success && resp.data) {
        const updated = resp.data as CommentDto;
        setComments((prev) => prev.map((c) => (c.id === commentId ? updated : c)));
        cancelEdit();
      } else {
        setError(resp?.error ?? 'Failed to update comment');
      }
    } catch (err) {
      console.error('Failed to update comment:', err);
      setError('Failed to update comment');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(commentId: string) {
    if (!window.confirm('Delete this comment?')) return;
    setError(null);
    const previous = comments;
    setComments((prev) => prev.filter((c) => c.id !== commentId));
    try {
      const resp = await deleteComment(albumId, mediaId, commentId);
      if (!resp?.success) {
        setComments(previous);
        setError(resp?.error ?? 'Failed to delete comment');
      }
    } catch (err) {
      console.error('Failed to delete comment:', err);
      setComments(previous);
      setError('Failed to delete comment');
    }
  }

  function handleInputKey(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  }

  const canPost = currentUserId !== null;
  const remaining = MAX_COMMENT_LENGTH - draft.length;

  return (
    <>
      <style>{`
        .ct-root {
          display: flex;
          flex-direction: column;
          gap: 0.6rem;
          color: var(--color-warm-white);
          font-family: var(--font-body);
        }

        .ct-heading {
          font-family: var(--font-display);
          font-weight: 700;
          font-size: 0.95rem;
          letter-spacing: 0.01em;
          color: var(--color-warm-white);
          margin: 0;
          opacity: 0.9;
        }

        .ct-list {
          display: flex;
          flex-direction: column;
          gap: 0.6rem;
          max-height: 32vh;
          overflow-y: auto;
          padding-right: 0.4rem;
        }
        .ct-list::-webkit-scrollbar { width: 6px; }
        .ct-list::-webkit-scrollbar-thumb {
          background: rgba(253, 248, 239, 0.2);
          border-radius: 999px;
        }

        .ct-empty,
        .ct-loading {
          font-size: 0.82rem;
          opacity: 0.7;
          padding: 0.5rem 0;
        }

        .ct-error {
          font-size: 0.78rem;
          color: var(--color-soft-red);
          padding: 0.25rem 0;
        }

        .ct-item {
          display: flex;
          gap: 0.6rem;
          align-items: flex-start;
        }

        .ct-avatar {
          flex-shrink: 0;
          width: 32px;
          height: 32px;
          border-radius: 50%;
          background: var(--color-ocean-blue);
          color: var(--color-warm-white);
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: 700;
          font-size: 0.78rem;
          font-family: var(--font-display);
          overflow: hidden;
        }
        .ct-avatar img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }

        .ct-bubble {
          flex: 1;
          min-width: 0;
          background: rgba(253, 248, 239, 0.08);
          border: 1px solid rgba(253, 248, 239, 0.1);
          border-radius: var(--radius-md);
          padding: 0.5rem 0.7rem;
        }

        .ct-meta {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          font-size: 0.74rem;
          margin-bottom: 0.18rem;
        }
        .ct-author {
          font-weight: 600;
          color: var(--color-warm-white);
        }
        .ct-time {
          opacity: 0.6;
          font-size: 0.7rem;
        }
        .ct-edited {
          opacity: 0.5;
          font-size: 0.68rem;
          font-style: italic;
        }

        .ct-body {
          font-size: 0.85rem;
          line-height: 1.45;
          color: var(--color-warm-white);
          white-space: pre-wrap;
          word-break: break-word;
        }

        .ct-actions {
          display: flex;
          gap: 0.4rem;
          margin-top: 0.3rem;
        }
        .ct-action {
          background: transparent;
          border: none;
          color: var(--color-ocean-blue);
          font-family: var(--font-body);
          font-size: 0.72rem;
          font-weight: 600;
          cursor: pointer;
          padding: 0.05rem 0;
          letter-spacing: 0.02em;
        }
        .ct-action:hover { text-decoration: underline; }
        .ct-action.danger { color: var(--color-soft-red); }

        .ct-edit-textarea,
        .ct-input {
          width: 100%;
          background: rgba(253, 248, 239, 0.08);
          border: 1px solid rgba(253, 248, 239, 0.18);
          color: var(--color-warm-white);
          border-radius: var(--radius-sm);
          padding: 0.45rem 0.6rem;
          font-family: var(--font-body);
          font-size: 0.85rem;
          line-height: 1.4;
          resize: vertical;
          min-height: 36px;
          max-height: 140px;
          outline: none;
          transition: border-color 0.15s, background 0.15s;
        }
        .ct-edit-textarea:focus,
        .ct-input:focus {
          border-color: var(--color-ocean-blue);
          background: rgba(253, 248, 239, 0.12);
        }

        .ct-input-row {
          display: flex;
          gap: 0.5rem;
          align-items: flex-end;
        }
        .ct-input-wrapper {
          flex: 1;
          display: flex;
          flex-direction: column;
          gap: 0.2rem;
        }
        .ct-input-meta {
          font-size: 0.68rem;
          opacity: 0.55;
          align-self: flex-end;
        }
        .ct-input-meta.warn { color: var(--color-soft-red); opacity: 0.9; }

        .ct-submit {
          background: var(--color-sky-blue);
          border: none;
          color: #fff;
          font-family: var(--font-body);
          font-weight: 600;
          font-size: 0.8rem;
          padding: 0.5rem 1rem;
          border-radius: var(--radius-sm);
          cursor: pointer;
          transition: background 0.15s, opacity 0.15s;
          align-self: stretch;
        }
        .ct-submit:hover { background: var(--color-ocean-blue); }
        .ct-submit:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }
      `}</style>

      <div className="ct-root">
        <h3 className="ct-heading">Comments</h3>

        <div className="ct-list">
          {loading ? (
            <div className="ct-loading">Loading comments...</div>
          ) : comments.length === 0 ? (
            <div className="ct-empty">No comments yet — say something nice.</div>
          ) : (
            comments.map((c) => {
              const isAuthor = currentUserId !== null && currentUserId === c.authorId;
              const isOwner = albumOwnerId !== null && currentUserId === albumOwnerId;
              const canDelete = isAuthor || isOwner;
              const canEdit = isAuthor;
              const wasEdited = c.updatedAt !== c.createdAt;
              const isEditing = editingId === c.id;
              return (
                <div key={c.id} className="ct-item">
                  <div className="ct-avatar" aria-hidden="true">
                    {c.authorAvatarUrl ? (
                      <img src={c.authorAvatarUrl} alt="" />
                    ) : (
                      initials(c.authorDisplayName)
                    )}
                  </div>
                  <div className="ct-bubble">
                    <div className="ct-meta">
                      <span className="ct-author">{c.authorDisplayName}</span>
                      <span className="ct-time">{formatRelative(c.createdAt)}</span>
                      {wasEdited && <span className="ct-edited">edited</span>}
                    </div>
                    {isEditing ? (
                      <>
                        <textarea
                          className="ct-edit-textarea"
                          value={editingDraft}
                          onChange={(e) => setEditingDraft(e.target.value.slice(0, MAX_COMMENT_LENGTH))}
                          rows={2}
                          autoFocus
                        />
                        <div className="ct-actions">
                          <button
                            className="ct-action"
                            onClick={() => handleSaveEdit(c.id)}
                            disabled={submitting || editingDraft.trim().length === 0}
                          >
                            Save
                          </button>
                          <button className="ct-action" onClick={cancelEdit}>Cancel</button>
                        </div>
                      </>
                    ) : (
                      <>
                        <div className="ct-body">{c.body}</div>
                        {(canEdit || canDelete) && (
                          <div className="ct-actions">
                            {canEdit && (
                              <button className="ct-action" onClick={() => startEdit(c)}>Edit</button>
                            )}
                            {canDelete && (
                              <button
                                className="ct-action danger"
                                onClick={() => handleDelete(c.id)}
                              >
                                Delete
                              </button>
                            )}
                          </div>
                        )}
                      </>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>

        {error && <div className="ct-error">{error}</div>}

        {canPost && (
          <div className="ct-input-row">
            <div className="ct-input-wrapper">
              <textarea
                ref={inputRef}
                className="ct-input"
                placeholder="Add a comment..."
                value={draft}
                onChange={(e) => setDraft(e.target.value.slice(0, MAX_COMMENT_LENGTH))}
                onKeyDown={handleInputKey}
                rows={1}
                aria-label="Add a comment"
              />
              <span className={`ct-input-meta ${remaining < 50 ? 'warn' : ''}`}>
                {draft.length > 0 ? `${remaining} left` : 'Press Enter to post, Shift+Enter for newline'}
              </span>
            </div>
            <button
              className="ct-submit"
              onClick={handleSubmit}
              disabled={submitting || draft.trim().length === 0}
            >
              Post
            </button>
          </div>
        )}
      </div>
    </>
  );
}
