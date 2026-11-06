/** Detecta la comuna (3 o 4) de un punto con el algoritmo de ray casting sobre el GeoJSON de OSM. */

type Anillo = number[][]
interface Rasgo {
  properties: { comuna: number }
  geometry: { type: 'Polygon' | 'MultiPolygon'; coordinates: Anillo[] | Anillo[][] }
}

let cache: Promise<Rasgo[]> | null = null

function cargar(): Promise<Rasgo[]> {
  cache ??= fetch('/geo/comunas-3-4.json')
    .then((r) => r.json())
    .then((d) => d.features as Rasgo[])
    .catch(() => [])
  return cache
}

function dentro(lng: number, lat: number, anillo: Anillo) {
  let adentro = false
  for (let i = 0, j = anillo.length - 1; i < anillo.length; j = i++) {
    const [xi, yi] = anillo[i]
    const [xj, yj] = anillo[j]
    if (yi > lat !== yj > lat && lng < ((xj - xi) * (lat - yi)) / (yj - yi) + xi) adentro = !adentro
  }
  return adentro
}

export async function comunaDe(lat: number, lng: number): Promise<number | null> {
  const rasgos = await cargar()
  for (const r of rasgos) {
    const poligonos = (r.geometry.type === 'Polygon' ? [r.geometry.coordinates] : r.geometry.coordinates) as Anillo[][]
    for (const p of poligonos) {
      if (dentro(lng, lat, p[0]) && !p.slice(1).some((hueco) => dentro(lng, lat, hueco))) return r.properties.comuna
    }
  }
  return null
}
