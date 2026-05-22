declare namespace AMap {
  class Map {
    constructor(container: string | HTMLElement, opts?: MapOptions)
    destroy(): void
    on(event: string, handler: (e: any) => void): void
    setFitView(objects?: any[]): void
    addControl(control: any): void
    getCenter(): LngLat
    getZoom(): number
  }

  interface MapOptions {
    center?: [number, number]
    zoom?: number
    viewMode?: '2D' | '3D'
  }

  class LngLat {
    getLng(): number
    getLat(): number
  }

  class Marker {
    constructor(opts: MarkerOptions)
    setMap(map: Map | null): void
    getPosition(): LngLat | null
  }

  interface MarkerOptions {
    position: [number, number]
    title?: string
    label?: MarkerLabel
    icon?: string
    offset?: Pixel
    map?: Map | null
  }

  interface MarkerLabel {
    content: string
    direction?: string
    offset?: Pixel
  }

  class Polyline {
    constructor(opts: PolylineOptions)
    setMap(map: Map | null): void
  }

  interface PolylineOptions {
    path: [number, number][]
    strokeColor?: string
    strokeWeight?: number
    strokeOpacity?: number
    showDir?: boolean
    strokeStyle?: string
    map?: Map | null
  }

  class Pixel {
    constructor(x: number, y: number)
  }

  class ToolBar {}
  class Scale {}

  /** Load plugin dynamically */
  function plugin(name: string, callback: () => void): void

  class Geocoder {
    constructor(opts?: GeocoderOptions)
    getLocation(address: string, callback: (status: string, result: any) => void): void
    getAddress(pos: [number, number] | LngLat, callback: (status: string, result: any) => void): void
  }

  interface GeocoderOptions {
    city?: string
    radius?: number
  }

  class PlaceSearch {
    constructor(opts?: PlaceSearchOptions)
    search(keyword: string, callback: (status: string, result: any) => void): void
  }

  interface PlaceSearchOptions {
    city?: string
    pageSize?: number
  }
}
