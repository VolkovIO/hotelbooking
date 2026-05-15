import { useState } from "react";
import { shortTechnicalId } from "../utils/formatters";

type TechnicalIdProps = {
  value: string;
  label?: string;
};

/**
 * Renders a long backend identifier in a UI-friendly way.
 *
 * Full UUIDs are important for debugging, but rendering them fully inside
 * every table cell makes the interface noisy and too wide.
 *
 * The full value is still available:
 * - in the title tooltip
 * - through the Copy button
 */
export function TechnicalId({ value, label }: TechnicalIdProps) {
  const [copied, setCopied] = useState(false);

  async function copyToClipboard() {
    await navigator.clipboard.writeText(value);
    setCopied(true);

    window.setTimeout(() => {
      setCopied(false);
    }, 1200);
  }

  return (
    <span className="technical-id-inline">
      {label !== undefined && <span className="technical-id-label">{label}</span>}

      <code title={value}>{shortTechnicalId(value)}</code>

      <button
        className="copy-button"
        type="button"
        title={`Copy ${value}`}
        onClick={copyToClipboard}
      >
        {copied ? "Copied" : "Copy"}
      </button>
    </span>
  );
}