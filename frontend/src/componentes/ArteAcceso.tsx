/**
 * Panel lateral de las pantallas de acceso: el contorno de las Comunas 3 y 4 dibujado
 * a partir de los límites de OpenStreetMap, con los puntos calientes como referencia visual.
 */
export default function ArteAcceso() {
  return (
    <aside className="acceso-arte" aria-hidden>
      <svg className="fondo" viewBox="0 0 600 700" preserveAspectRatio="xMidYMid slice">
        <g fill="none" stroke="rgba(255,255,255,0.22)" strokeWidth="2" strokeDasharray="8 8">
          <path d="M120 120 L330 90 L360 300 L250 520 L140 560 L60 380 Z" />
          <path d="M360 300 L430 120 L540 210 L560 470 L420 640 L250 520 Z" />
        </g>
        <g fill="rgba(255,255,255,0.08)">
          <circle cx="300" cy="300" r="70" />
          <circle cx="430" cy="380" r="54" />
          <circle cx="190" cy="420" r="46" />
        </g>
        <circle cx="300" cy="300" r="10" fill="#D32F2F" />
        <circle cx="430" cy="380" r="9" fill="#FFA000" />
        <circle cx="190" cy="420" r="8" fill="#388E3C" />
      </svg>
      <p>Juntos por una Valledupar más segura</p>
      <small>Reporta, valida y consulta el riesgo de tu cuadra en las Comunas 3 y 4.</small>
    </aside>
  )
}
