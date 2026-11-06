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
}

export interface Sesion {
  token: string
  usuario: Usuario
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
