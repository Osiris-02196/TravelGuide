<template>
  <div class="route-viewer" v-if="routeData">
    <div class="route-info-bar">
      <span>{{ transportLabel }}</span>
      <span class="info-sep">|</span>
      <span>{{ formatDistance(routeData.distance) }}</span>
      <span class="info-sep">|</span>
      <span>约 {{ formatDuration(routeData.duration) }}</span>
    </div>

    <!-- Point names -->
    <div class="route-points-bar">
      <span class="point-item origin">
        <span class="point-dot orig"></span>
        {{ routeData.originName || `(${routeData.origin[0].toFixed(4)}, ${routeData.origin[1].toFixed(4)})` }}
      </span>
      <template v-if="routeData.waypoints.length && routeData.waypointNames">
        <ArrowRightOutlined class="point-arrow" />
        <span class="point-item" v-for="(name, i) in routeData.waypointNames" :key="i">
          <span class="point-dot wp"></span>
          {{ name }}
        </span>
      </template>
      <ArrowRightOutlined class="point-arrow" />
      <span class="point-item dest">
        <span class="point-dot dst"></span>
        {{ routeData.destinationName || `(${routeData.destination[0].toFixed(4)}, ${routeData.destination[1].toFixed(4)})` }}
      </span>
    </div>

    <div ref="mapContainer" class="map-container"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ArrowRightOutlined } from '@ant-design/icons-vue'
import { getAMap } from '@/utils/amap'

interface RouteData {
  transportType: string
  origin: [number, number]
  destination: [number, number]
  waypoints: [number, number][]
  polyline: [number, number][]
  distance: number
  duration: number
  originName?: string
  destinationName?: string
  waypointNames?: string[]
}

const props = defineProps<{
  routeData: RouteData | null
}>()

const mapContainer = ref<HTMLDivElement>()
let map: AMap.Map | null = null

const transportLabelMap: Record<string, string> = {
  driving: '驾车',
  walking: '步行',
  riding: '骑行',
}
const transportLabel = ref('')

onMounted(() => {
  if (!mapContainer.value || !props.routeData) return
  renderRoute()
})

onUnmounted(() => {
  if (map) map.destroy()
})

async function renderRoute() {
  if (!mapContainer.value || !props.routeData) return
  const data = props.routeData
  transportLabel.value = transportLabelMap[data.transportType] || data.transportType

  const AMap = await getAMap()
  map = new AMap.Map(mapContainer.value, {
    center: data.origin,
    zoom: 14,
    viewMode: '2D',
  })

  // Draw route polyline
  new AMap.Polyline({
    path: data.polyline as any,
    strokeColor: '#1890ff',
    strokeWeight: 6,
    strokeOpacity: 0.8,
    showDir: true,
    map,
  })

  // Start marker
  new AMap.Marker({
    position: data.origin,
    label: {
      content: '<div style="background:#1890ff;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px;font-weight:bold">起点</div>',
      direction: 'top',
    },
    map,
  })

  // Waypoint markers
  data.waypoints.forEach((wp, i) => {
    new AMap.Marker({
      position: wp,
      label: {
        content: `<div style="background:#52c41a;color:#fff;padding:2px 6px;border-radius:4px;font-size:12px;font-weight:bold">${i + 1}</div>`,
        direction: 'top',
      },
      map,
    })
  })

  // End marker
  new AMap.Marker({
    position: data.destination,
    label: {
      content: '<div style="background:#ff4d4f;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px;font-weight:bold">终点</div>',
      direction: 'top',
    },
    map,
  })

  map.setFitView()
}

function formatDistance(m: number): string {
  return m >= 1000 ? `${(m / 1000).toFixed(1)}公里` : `${m}米`
}
function formatDuration(s: number): string {
  return s >= 3600
    ? `${Math.floor(s / 3600)}小时${Math.round((s % 3600) / 60)}分钟`
    : `${Math.round(s / 60)}分钟`
}
</script>

<style scoped>
.route-viewer {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
}
.route-info-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f6ffed;
  font-size: 13px;
  color: #333;
  font-weight: 500;
}
.info-sep {
  color: #d9d9d9;
  font-weight: 300;
}
.route-points-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  padding: 6px 12px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  font-size: 12px;
}
.point-item {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: #555;
}
.point-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.point-dot.orig { background: #1890ff; }
.point-dot.dst { background: #ff4d4f; }
.point-dot.wp { background: #52c41a; }
.point-arrow {
  font-size: 11px;
  color: #bfbfbf;
}
.map-container {
  width: 100%;
  height: 350px;
}
</style>
