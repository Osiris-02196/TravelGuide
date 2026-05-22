import AMapLoader from '@amap/amap-jsapi-loader'

declare global {
  interface Window {
    _AMapSecurityConfig?: {
      serviceHost: string
    }
  }
}

let amapLoaded: Promise<typeof AMap> | null = null

/**
 * 获取高德地图 AMap 对象（单例加载）
 * 自动配置 serviceHost 指向后端代理，安全密钥不暴露在前端
 */
export function getAMap(): Promise<typeof AMap> {
  if (amapLoaded) return amapLoaded

  // 配置安全代理：所有请求通过后端转发
  window._AMapSecurityConfig = {
    serviceHost: '/api/amap-proxy'
  }

  amapLoaded = AMapLoader.load({
    key: import.meta.env.VITE_AMAP_KEY,
    version: '2.0',
    plugins: ['AMap.Driving', 'AMap.Walking', 'AMap.Riding', 'AMap.Geocoder', 'AMap.ToolBar', 'AMap.Scale']
  })

  return amapLoaded
}

/**
 * 创建地图实例
 */
export async function createMap(
  container: string | HTMLElement,
  center: [number, number] = [116.397428, 39.90923],
  zoom: number = 13
): Promise<AMap.Map> {
  const AMap = await getAMap()
  return new AMap.Map(container, {
    center,
    zoom,
    viewMode: '2D',
  })
}
