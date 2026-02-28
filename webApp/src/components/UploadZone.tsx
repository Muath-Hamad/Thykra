import { useState, useRef, DragEvent } from 'react';

interface UploadZoneProps {
  onFilesSelected: (files: File[]) => void;
}

export function UploadZone({ onFilesSelected }: UploadZoneProps) {
  const [dragOver, setDragOver] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  function handleDragOver(e: DragEvent) {
    e.preventDefault();
    setDragOver(true);
  }

  function handleDragLeave(e: DragEvent) {
    e.preventDefault();
    setDragOver(false);
  }

  function handleDrop(e: DragEvent) {
    e.preventDefault();
    setDragOver(false);
    const files = Array.from(e.dataTransfer.files).filter(
      (f) => f.type.startsWith('image/') || f.type.startsWith('video/')
    );
    if (files.length > 0) {
      onFilesSelected(files);
    }
  }

  function handleClick() {
    inputRef.current?.click();
  }

  function handleInputChange(e: React.ChangeEvent<HTMLInputElement>) {
    const files = e.target.files ? Array.from(e.target.files) : [];
    if (files.length > 0) {
      onFilesSelected(files);
    }
    // Reset input so the same file can be selected again
    e.target.value = '';
  }

  return (
    <>
      <style>{`
        .upload-zone {
          border: 2px dashed #D4C5A9;
          border-radius: var(--radius-md);
          padding: 2.5rem 2rem;
          text-align: center;
          cursor: pointer;
          background: transparent;
          transition: border-color 0.2s, background-color 0.2s;
          margin-bottom: 1.5rem;
        }
        .upload-zone:hover {
          border-color: rgba(27,127,204,0.4);
        }
        .upload-zone.drag-over {
          border-color: var(--color-sky-blue);
          background-color: rgba(27,127,204,0.04);
        }

        .upload-zone-icon {
          margin-bottom: 0.75rem;
          color: var(--color-muted-slate);
          opacity: 0.5;
        }
        .upload-zone.drag-over .upload-zone-icon {
          color: var(--color-sky-blue);
          opacity: 1;
        }

        .upload-zone-text {
          margin: 0;
          color: var(--color-muted-slate);
          font-family: var(--font-body);
          font-size: 0.85rem;
          font-weight: 400;
        }
      `}</style>

      <div
        className={`upload-zone${dragOver ? ' drag-over' : ''}`}
        onClick={handleClick}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        <input
          ref={inputRef}
          type="file"
          multiple
          accept="image/*,video/*"
          onChange={handleInputChange}
          style={{ display: 'none' }}
        />
        <div className="upload-zone-icon">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 16V4" />
            <path d="M8 8l4-4 4 4" />
            <path d="M20 16.7A4 4 0 0017.5 9H16.2A7 7 0 104 14.3" />
          </svg>
        </div>
        <p className="upload-zone-text">
          {dragOver ? 'Drop files here' : 'Click or drag photos here to upload'}
        </p>
      </div>
    </>
  );
}
