type LoadingStateProps = {
  text?: string;
};

/**
 * Small reusable loading component.
 *
 * React components should describe UI states explicitly:
 * - loading
 * - error
 * - empty
 * - data
 */
export function LoadingState({ text = "Loading..." }: LoadingStateProps) {
  return <div className="state-card">{text}</div>;
}