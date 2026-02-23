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
    <div
      onClick={handleClick}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      style={{
        border: `2px dashed ${dragOver ? '#4A90D9' : '#ccc'}`,
        borderRadius: '8px',
        padding: '2rem',
        textAlign: 'center',
        cursor: 'pointer',
        backgroundColor: dragOver ? '#f0f7ff' : 'transparent',
        transition: 'border-color 0.2s, background-color 0.2s',
        marginBottom: '1rem',
      }}
    >
      <input
        ref={inputRef}
        type="file"
        multiple
        accept="image/*,video/*"
        onChange={handleInputChange}
        style={{ display: 'none' }}
      />
      <p style={{ margin: 0, color: '#666' }}>
        {dragOver ? 'Drop files here' : 'Click or drag files here to upload'}
      </p>
    </div>
  );
}
