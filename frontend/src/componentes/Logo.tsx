/** Marca de AlertApp: un pin de ubicación con el punto de riesgo en el centro. */
export default function Logo({ tam = 32, invertido = false }: { tam?: number; invertido?: boolean }) {
  return (
    <svg width={tam} height={tam} viewBox="0 0 32 32" aria-hidden>
      <rect width="32" height="32" rx="7" fill={invertido ? '#ffffff' : 'var(--accion)'} />
      <path
        d="M16 6c-4.4 0-8 3.4-8 7.7C8 19.5 16 26 16 26s8-6.5 8-12.3C24 9.4 20.4 6 16 6z"
        fill={invertido ? 'var(--accion)' : 'var(--sobre-accion)'}
      />
      <circle cx="16" cy="13.6" r="3.4" fill="#D32F2F" />
    </svg>
  )
}
