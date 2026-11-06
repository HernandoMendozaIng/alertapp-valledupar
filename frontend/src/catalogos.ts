import type { Arma, Elemento, EstadoReporte, Medio, TipoDelito } from './tipos'

export const TIPOS: Record<TipoDelito, string> = {
  ATRACO: 'Atraco',
  HURTO: 'Hurto sin violencia',
  SOSPECHOSO: 'Actividad sospechosa',
  OTRO: 'Otro hecho',
}

export const ARMAS: Record<Arma, string> = {
  ARMA_FUEGO: 'Arma de fuego',
  CORTOPUNZANTE: 'Cuchillo o navaja',
  SIN_ARMA: 'Sin arma',
  NO_IDENTIFICADA: 'No la vi',
}

export const MEDIOS: Record<Medio, string> = {
  A_PIE: 'A pie',
  BICICLETA: 'Bicicleta',
  MOTOCICLETA: 'Moto',
  VEHICULO: 'Carro',
  NO_IDENTIFICADO: 'No sé',
}

/** Frase corta del medio del agresor para las tarjetas (evita repetir "Moto · Moto"). */
export const MEDIO_FRASE: Record<Medio, string> = {
  A_PIE: 'Iba a pie',
  BICICLETA: 'Iba en bicicleta',
  MOTOCICLETA: 'Iba en moto',
  VEHICULO: 'Iba en carro',
  NO_IDENTIFICADO: 'Medio no identificado',
}

export const ELEMENTOS: Record<Elemento, string> = {
  CELULAR: 'Celular',
  DINERO: 'Dinero',
  JOYAS: 'Joyas',
  PRENDA: 'Ropa o bolso',
  BICICLETA: 'Bicicleta',
  MOTOCICLETA: 'Moto',
  VEHICULO: 'Carro',
  PARTE_VEHICULO: 'Partes del vehículo',
  NINGUNO: 'Nada',
  OTRO: 'Otra cosa',
}

export const ESTADOS: Record<EstadoReporte, string> = {
  PENDIENTE: 'Pendiente',
  VALIDADO: 'Validado por vecinos',
  EN_REVISION: 'En revisión',
  CERRADO: 'Cerrado',
  DESCARTADO: 'Descartado',
}

export const NIVELES = { ALTO: 'Alto', MEDIO: 'Medio', BAJO: 'Bajo', ALTA: 'Alta', MEDIA: 'Media', BAJA: 'Baja' } as const

/** Barrios citados en el planteamiento del problema (Fase 3). */
export const BARRIOS: Record<number, string[]> = {
  3: ['Primero de Mayo', 'San Martín'],
  4: ['Jorge Dangond', 'Cicerón Maestre', 'Los Fundadores'],
}

/** Centro aproximado del área de estudio (entre las Comunas 3 y 4). */
export const CENTRO: [number, number] = [10.4475, -73.262]

const fechaLarga = new Intl.DateTimeFormat('es-CO', {
  day: 'numeric',
  month: 'short',
  hour: 'numeric',
  minute: '2-digit',
  timeZone: 'America/Bogota',
})

export function fecha(iso: string) {
  return fechaLarga.format(new Date(iso))
}

const relativa = new Intl.RelativeTimeFormat('es-CO', { numeric: 'auto' })

export function hace(iso: string) {
  const minutos = Math.round((new Date(iso).getTime() - Date.now()) / 60000)
  if (Math.abs(minutos) < 60) return relativa.format(minutos, 'minute')
  const horas = Math.round(minutos / 60)
  if (Math.abs(horas) < 24) return relativa.format(horas, 'hour')
  return relativa.format(Math.round(horas / 24), 'day')
}

export function metros(m: number) {
  return m < 1000 ? `${Math.round(m)} m` : `${(m / 1000).toFixed(1)} km`
}

export function distancia(lat1: number, lon1: number, lat2: number, lon2: number) {
  const r = 6371000
  const dLat = ((lat2 - lat1) * Math.PI) / 180
  const dLon = ((lon2 - lon1) * Math.PI) / 180
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) ** 2
  return 2 * r * Math.asin(Math.sqrt(a))
}
