import {
  forwardRef,
  useId,
  useLayoutEffect,
  useRef,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from 'react';
import { useT } from '../i18n/LocaleProvider';
import styles from './Field.module.css';

interface FieldChrome {
  label: string;
  optionalTag?: boolean;
  help?: string;
  error?: string | null;
  /** Show "{n} left" once remaining ≤ 20% of maxLength. */
  showCounter?: boolean;
}

function useFieldIds(error?: string | null, help?: string) {
  const id = useId();
  const describedBy =
    [error ? `${id}-error` : null, help ? `${id}-help` : null].filter(Boolean).join(' ') ||
    undefined;
  return { id, describedBy };
}

function FieldShell({
  chrome,
  id,
  disabled,
  children,
  counter,
}: {
  chrome: FieldChrome;
  id: string;
  disabled?: boolean;
  children: ReactNode;
  counter?: ReactNode;
}) {
  const t = useT();
  const labelClass = [
    styles.label,
    chrome.error ? styles.labelError : '',
    disabled ? styles.labelDisabled : '',
  ]
    .filter(Boolean)
    .join(' ');
  return (
    <div className={styles.field}>
      <label className={labelClass} htmlFor={id}>
        {chrome.label}
        {chrome.optionalTag && <span className={styles.optional}> {t('common.optional')}</span>}
      </label>
      {children}
      {counter}
      {chrome.error ? (
        <div className={styles.error} id={`${id}-error`}>
          {chrome.error}
        </div>
      ) : chrome.help ? (
        <div className={styles.help} id={`${id}-help`}>
          {chrome.help}
        </div>
      ) : null}
    </div>
  );
}

export type InputProps = FieldChrome & InputHTMLAttributes<HTMLInputElement>;

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { label, optionalTag, help, error, showCounter, className, ...rest },
  ref,
) {
  const { id, describedBy } = useFieldIds(error, help);
  return (
    <FieldShell chrome={{ label, optionalTag, help, error }} id={id} disabled={rest.disabled}>
      <input
        ref={ref}
        id={id}
        className={[styles.control, error ? styles.controlError : '', className ?? '']
          .filter(Boolean)
          .join(' ')}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        {...rest}
      />
    </FieldShell>
  );
});

export type TextareaProps = FieldChrome & TextareaHTMLAttributes<HTMLTextAreaElement>;

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  { label, optionalTag, help, error, showCounter, className, value, maxLength, onChange, ...rest },
  ref,
) {
  const t = useT();
  const { id, describedBy } = useFieldIds(error, help);
  const innerRef = useRef<HTMLTextAreaElement | null>(null);

  // Auto-grow to 6 lines.
  useLayoutEffect(() => {
    const el = innerRef.current;
    if (!el) return;
    el.style.height = 'auto';
    const lineHeight = 21;
    el.style.height = `${Math.min(el.scrollHeight, lineHeight * 6 + 18)}px`;
  }, [value]);

  const remaining =
    typeof maxLength === 'number' && typeof value === 'string' ? maxLength - value.length : null;
  const counterVisible =
    showCounter && remaining !== null && maxLength != null && remaining <= maxLength * 0.2;

  return (
    <FieldShell
      chrome={{ label, optionalTag, help, error }}
      id={id}
      disabled={rest.disabled}
      counter={
        counterVisible ? (
          <div className={styles.counterRow} aria-live="polite">
            {t('common.charactersLeft', { n: remaining! })}
          </div>
        ) : undefined
      }
    >
      <textarea
        ref={(el) => {
          innerRef.current = el;
          if (typeof ref === 'function') ref(el);
          else if (ref) ref.current = el;
        }}
        id={id}
        className={[styles.control, error ? styles.controlError : '', className ?? '']
          .filter(Boolean)
          .join(' ')}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        value={value}
        maxLength={maxLength}
        onChange={onChange}
        {...rest}
      />
    </FieldShell>
  );
});

export type SelectProps = FieldChrome & SelectHTMLAttributes<HTMLSelectElement>;

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { label, optionalTag, help, error, showCounter, className, children, ...rest },
  ref,
) {
  const { id, describedBy } = useFieldIds(error, help);
  return (
    <FieldShell chrome={{ label, optionalTag, help, error }} id={id} disabled={rest.disabled}>
      <select
        ref={ref}
        id={id}
        className={[styles.control, error ? styles.controlError : '', className ?? '']
          .filter(Boolean)
          .join(' ')}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        {...rest}
      >
        {children}
      </select>
    </FieldShell>
  );
});
