import {
  forwardRef,
  useLayoutEffect,
  useRef,
  useState,
  type AnchorHTMLAttributes,
  type ButtonHTMLAttributes,
  type ReactNode,
} from 'react';
import styles from './Button.module.css';

export type ButtonVariant =
  | 'primary'
  | 'secondary'
  | 'warm'
  | 'ghost'
  | 'ghost-link'
  | 'danger'
  | 'danger-quiet';
export type ButtonSize = 'sm' | 'md' | 'lg';

interface CommonProps {
  variant?: ButtonVariant;
  size?: ButtonSize;
  /** Replaces the label with a spinner + loadingLabel; locks width first. */
  loading?: boolean;
  loadingLabel?: string;
  icon?: ReactNode;
  iconOnly?: boolean;
  children?: ReactNode;
}

type ButtonAsButton = CommonProps &
  ButtonHTMLAttributes<HTMLButtonElement> & { as?: 'button' };
type ButtonAsAnchor = CommonProps &
  AnchorHTMLAttributes<HTMLAnchorElement> & { as: 'a' };

export type ButtonProps = ButtonAsButton | ButtonAsAnchor;

function classFor(variant: ButtonVariant, size: ButtonSize, loading: boolean, iconOnly: boolean) {
  const variantClass =
    variant === 'ghost-link'
      ? `${styles.ghost} ${styles.link}`
      : variant === 'danger-quiet'
        ? styles.dangerQuiet
        : styles[variant];
  return [
    styles.btn,
    variantClass,
    styles[size],
    loading ? styles.loading : '',
    iconOnly ? styles.iconOnly : '',
  ]
    .filter(Boolean)
    .join(' ');
}

export const Button = forwardRef<HTMLButtonElement | HTMLAnchorElement, ButtonProps>(
  function Button(props, ref) {
    const {
      variant = 'primary',
      size = 'md',
      loading = false,
      loadingLabel,
      icon,
      iconOnly = false,
      children,
      ...rest
    } = props as CommonProps & Record<string, unknown>;

    const innerRef = useRef<HTMLButtonElement | HTMLAnchorElement | null>(null);
    const [lockedWidth, setLockedWidth] = useState<number | null>(null);

    // Lock width before the label→spinner swap so the button never reflows.
    useLayoutEffect(() => {
      if (loading && innerRef.current && lockedWidth === null) {
        setLockedWidth(innerRef.current.getBoundingClientRect().width);
      } else if (!loading && lockedWidth !== null) {
        setLockedWidth(null);
      }
    }, [loading, lockedWidth]);

    const setRefs = (el: HTMLButtonElement | HTMLAnchorElement | null) => {
      innerRef.current = el;
      if (typeof ref === 'function') ref(el);
      else if (ref) (ref as React.MutableRefObject<typeof el>).current = el;
    };

    const className = classFor(variant, size, loading, iconOnly);
    const style = lockedWidth !== null ? { minWidth: lockedWidth } : undefined;

    const content = loading ? (
      <>
        <span className={styles.spinner} aria-hidden="true" />
        {loadingLabel ?? children}
      </>
    ) : (
      <>
        {icon}
        {iconOnly ? null : children}
      </>
    );

    if ((props as ButtonAsAnchor).as === 'a') {
      const { as: _as, ...anchorRest } = rest as { as?: 'a' } & Record<string, unknown>;
      return (
        <a
          ref={setRefs as never}
          className={className}
          style={style}
          {...(anchorRest as AnchorHTMLAttributes<HTMLAnchorElement>)}
        >
          {content}
        </a>
      );
    }

    const { as: _as, disabled, ...buttonRest } = rest as {
      as?: 'button';
      disabled?: boolean;
    } & Record<string, unknown>;
    return (
      <button
        ref={setRefs as never}
        className={className}
        style={style}
        disabled={disabled || loading}
        aria-busy={loading || undefined}
        {...(buttonRest as ButtonHTMLAttributes<HTMLButtonElement>)}
      >
        {content}
      </button>
    );
  },
);
