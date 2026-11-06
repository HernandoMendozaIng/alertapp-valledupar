import { useEffect, useState } from 'react'
import { CENTRO } from './catalogos'

export interface Ubicacion {
  lat: number
  lng: number
  /** true si viene del GPS del dispositivo; false si es el centro del área de estudio. */
  real: boolean
  precision?: number
}

/**
 * Ubicación del usuario. Si el GPS no está disponible, está denegado o el usuario está fuera
 * de Valledupar, se usa el centro de las Comunas 3 y 4 para que el prototipo siga funcionando.
 */
export function useUbicacion(): Ubicacion {
  const [ubicacion, setUbicacion] = useState<Ubicacion>({ lat: CENTRO[0], lng: CENTRO[1], real: false })

  useEffect(() => {
    if (!('geolocation' in navigator)) return
    const id = navigator.geolocation.watchPosition(
      (p) => {
        const { latitude, longitude, accuracy } = p.coords
        const enValledupar = latitude > 10.4 && latitude < 10.52 && longitude > -73.32 && longitude < -73.2
        if (enValledupar) setUbicacion({ lat: latitude, lng: longitude, real: true, precision: accuracy })
      },
      () => {},
      { enableHighAccuracy: true, maximumAge: 30000, timeout: 15000 },
    )
    return () => navigator.geolocation.clearWatch(id)
  }, [])

  return ubicacion
}
