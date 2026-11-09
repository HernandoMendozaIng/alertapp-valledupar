export type Rol = 'CIUDADANO' | 'AUTORIDAD'
export type TipoDelito = 'ATRACO' | 'HURTO' | 'SOSPECHOSO' | 'OTRO'
export type Arma = 'ARMA_FUEGO' | 'CORTOPUNZANTE' | 'SIN_ARMA' | 'NO_IDENTIFICADA'
export type Medio = 'A_PIE' | 'BICICLETA' | 'MOTOCICLETA' | 'VEHICULO' | 'NO_IDENTIFICADO'
export type Elemento =
  | 'CELULAR'
  | 'DINERO'
  | 'JOYAS'
  | 'PRENDA'
  | 'BICICLETA'
  | 'MOTOCICLETA'
  | 'VEHICULO'
  | 'PARTE_VEHICULO'
  | 'NINGUNO'
  | 'OTRO'
export type Severidad = 'BAJA' | 'MEDIA' | 'ALTA'
export type EstadoReporte = 'PENDIENTE' | 'VALIDADO' | 'EN_REVISION' | 'CERRADO' | 'DESCARTADO'
export type Nivel = 'BAJO' | 'MEDIO' | 'ALTO'

export interface Usuario {
  id: number
  nombre: string
  correo: string
  telefono: string | null
  rol: Rol
  comuna: number | null
  notificaciones: boolean
  radioAlertas: number
  dosFactores: boolean
}

/** Si la cuenta tiene 2FA, el ingreso devuelve un desafío en lugar del token. */
export interface Sesion {
  token: string
  usuario: Usuario
  requiereSegundoFactor?: boolean
  desafio?: string | null
}

export interface ReportePeticion {
  tipoDelito: TipoDelito
  arma: Arma
  medio: Medio
  elementoHurtado: Elemento
  descripcion: string
  fechaHecho: string
  latitud: number
  longitud: number
  comuna: number
  barrio: string
  anonimo: boolean
}

export interface Reporte extends Omit<ReportePeticion, 'descripcion' | 'barrio'> {
  id: number
  descripcion: string | null
  barrio: string | null
  autor: string | null
  esMio: boolean
  estado: EstadoReporte
  severidad: Severidad
  puntajeSeveridad: number
  confirmaciones: number
  falsos: number
  miVoto: 'CONFIRMA' | 'FALSO' | null
  tieneEvidencia: boolean
  creadoEn: string
}

export interface Comentario {
  id: number
  autor: string
  esAutoridad: boolean
  esMio: boolean
  texto: string
  creadoEn: string
}

export interface Hotspot {
  id: number
  lat: number
  lng: number
  radioMetros: number
  reportes: number
  severidadMedia: number
  nivel: Nivel
  puntaje: number
  franjaPico: string
}

export interface RespuestaHotspots {
  motor: string
  hotspots: Hotspot[]
  ruido: number
}

export interface Riesgo {
  nivel: Nivel
  indice: number
  franja: string
  explicacion: string
  motor: string
}

export interface Conteo {
  clave: string
  valor: number
}

export interface Metricas {
  total: number
  ultimos7Dias: number
  semanaAnterior: number
  validados: number
  pendientes: number
  descartados: number
  porDiaSemana: Conteo[]
  porHora: Conteo[]
  porTipo: Conteo[]
  porComuna: Conteo[]
  porSeveridad: Conteo[]
  porSemana: Conteo[]
}

export interface Alerta {
  id: number
  usuario: string
  telefono: string | null
  latitud: number
  longitud: number
  estado: 'ACTIVA' | 'ATENDIDA' | 'CANCELADA'
  nota: string | null
  creadoEn: string
  cerradaEn: string | null
}

// ---------- Sprint 2 ----------

export type Modo = 'A_PIE' | 'BICICLETA' | 'VEHICULO'

export type TipoNotificacion =
  | 'REPORTE_CERCANO'
  | 'PANICO_PROTEGIDO'
  | 'SEGUIMIENTO_PROTEGIDO'
  | 'GUARDIAN_AGREGADO'
  | 'ESTADO_REPORTE'

export interface Notificacion {
  id: number
  tipo: TipoNotificacion
  titulo: string
  cuerpo: string
  enlace: string | null
  latitud: number | null
  longitud: number | null
  leida: boolean
  creadoEn: string
}

export interface Bandeja {
  sinLeer: number
  notificaciones: Notificacion[]
}

export interface Ruta {
  puntos: [number, number][]
  metros: number
  minutos: number
  metrosEnRiesgo: number
  exposicion: number
  reportesCercanos: number
  calles: string[]
  nivel: Nivel
}

export interface ResultadoRuta {
  modo: Modo
  segura: Ruta
  corta: Ruta
  mismaRuta: boolean
  metrosExtra: number
  minutosExtra: number
  reduccionRiesgo: number
  explicacion: string
  reportesConsiderados: number
  motor: string
  calculadoEn: string
}

export interface RutaFrecuente {
  id: number
  nombre: string
  origenLatitud: number
  origenLongitud: number
  destinoLatitud: number
  destinoLongitud: number
  modo: Modo
}

export interface Seguimiento {
  id: number
  motivo: 'TRAYECTO' | 'PANICO'
  rutaId: number | null
  rutaNombre: string | null
  latitud: number
  longitud: number
  iniciadoEn: string
  actualizadoEn: string
  expiraEn: string
  activo: boolean
  guardianesAvisados: number
}

export interface Guardian {
  id: number
  nombre: string
  correo: string
  telefono: string | null
  desde: string
}

export interface Protegido {
  usuarioId: number
  nombre: string
  telefono: string | null
  estado: 'PANICO' | 'TRAYECTO' | 'TRANQUILO'
  alertaId: number | null
  latitud: number | null
  longitud: number | null
  ubicacionEn: string | null
  expiraEn: string | null
}
