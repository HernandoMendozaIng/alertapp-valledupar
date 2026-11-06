import { useEffect, useState, type ReactNode } from 'react'
import { GeoJSON, MapContainer, TileLayer, useMap } from 'react-leaflet'
import type { GeoJsonObject } from 'geojson'
import { CENTRO } from '../catalogos'

export const COLOR_NIVEL: Record<string, string> = {
  ALTO: '#D32F2F',
  ALTA: '#D32F2F',
  MEDIO: '#FFA000',
  MEDIA: '#FFA000',
  BAJO: '#388E3C',
  BAJA: '#388E3C',
}

/** Límites de las Comunas 3 y 4 tomados de OpenStreetMap (archivo en /public/geo). */
function CapaComunas() {
  const [datos, setDatos] = useState<GeoJsonObject | null>(null)
  useEffect(() => {
    fetch('/geo/comunas-3-4.json')
      .then((r) => r.json())
      .then(setDatos)
      .catch(() => setDatos(null))
  }, [])
  if (!datos) return null
  return (
    <GeoJSON
      data={datos}
      style={{ color: '#1d3b53', weight: 2, opacity: 0.7, fillOpacity: 0.03, dashArray: '6 6' }}
      onEachFeature={(f, capa) => capa.bindTooltip(f.properties?.nombre ?? '', { sticky: true })}
    />
  )
}

function Centrar({ centro, zoom }: { centro?: [number, number]; zoom?: number }) {
  const mapa = useMap()
  useEffect(() => {
    if (centro) mapa.setView(centro, zoom ?? mapa.getZoom(), { animate: true })
  }, [centro?.[0], centro?.[1]]) // eslint-disable-line react-hooks/exhaustive-deps
  return null
}

export default function MapaBase({
  children,
  centro,
  zoom = 15,
  alto,
  interactivo = true,
}: {
  children?: ReactNode
  centro?: [number, number]
  zoom?: number
  alto?: number
  interactivo?: boolean
}) {
  return (
    <MapContainer
      center={centro ?? CENTRO}
      zoom={zoom}
      zoomControl={false}
      scrollWheelZoom={interactivo}
      dragging={interactivo}
      style={alto ? { height: alto, borderRadius: 'var(--radio)' } : undefined}
      attributionControl
    >
      <TileLayer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        maxZoom={19}
      />
      <CapaComunas />
      <Centrar centro={centro} zoom={zoom} />
      {children}
    </MapContainer>
  )
}
