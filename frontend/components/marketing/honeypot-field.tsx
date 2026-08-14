interface HoneypotFieldProps {
  name: string
  value: string
  onChange: (value: string) => void
}

/**
 * Anti-spam trap: positioned off-screen (not display:none, which some bots
 * skip) and excluded from tab order / screen readers. Real visitors never
 * see or fill it; a filled value means the submission is automated.
 */
export function HoneypotField({ name, value, onChange }: HoneypotFieldProps) {
  return (
    <div style={{ position: "absolute", left: "-9999px", top: "-9999px" }} aria-hidden="true">
      <label htmlFor={name}>Website</label>
      <input
        id={name}
        name={name}
        type="text"
        tabIndex={-1}
        autoComplete="off"
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
    </div>
  )
}
