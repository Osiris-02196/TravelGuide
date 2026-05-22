<template>
  <div class="route-editor-wrapper">
    <!-- Trigger: no route -->
    <a-button v-if="!routeData" type="dashed" block @click="openDialog">
      <template #icon><EnvironmentOutlined /></template>
      规划路线
    </a-button>

    <!-- Trigger: route exists -->
    <div v-else class="route-summary-bar" @click="openDialog">
      <div class="route-summary-left">
        <EnvironmentOutlined />
        <span class="route-badge">已规划路线</span>
        <span class="divider">|</span>
        <span>{{ formatDistance(routeData.distance) }}</span>
        <span class="divider">|</span>
        <span>{{ formatDuration(routeData.duration) }}</span>
      </div>
      <a-button size="small" type="text" danger @click.stop="clearRoute">
        <template #icon><DeleteOutlined /></template>
      </a-button>
    </div>

    <!-- Route planner modal -->
    <a-modal
      v-model:visible="dialogVisible"
      title="路线规划"
      width="880px"
      :footer="null"
      :destroy-on-close="true"
      :mask-closable="false"
    >
      <div class="planner">
        <!-- Left: controls -->
        <div class="planner-controls">
          <div class="ctrl-section">
            <label class="ctrl-label">出行方式</label>
            <a-radio-group v-model:value="localTransport" button-style="solid" size="small">
              <a-radio-button value="driving">驾车</a-radio-button>
              <a-radio-button value="walking">步行</a-radio-button>
              <a-radio-button value="riding">骑行</a-radio-button>
            </a-radio-group>
          </div>

          <!-- Origin -->
          <div class="ctrl-section">
            <label class="ctrl-label required">起点</label>
            <div class="search-wrap">
              <a-input
                v-model:value="originSearch"
                placeholder="搜索城市或景点名称"
                @input="onOriginInput"
              >
                <template #prefix><span class="dot dot-origin">起</span></template>
              </a-input>
              <ul v-if="originSuggestions.length" class="suggest-list">
                <li
                  v-for="item in originSuggestions"
                  :key="item.id"
                  @click="selectOrigin(item)"
                >
                  <span class="suggest-name">{{ item.name }}</span>
                  <span v-if="item.address" class="suggest-addr">{{ item.address }}</span>
                </li>
              </ul>
            </div>
            <div v-if="originPoint" class="selected-point origin">
              {{ originPoint.name }}
              <a-button type="text" size="small" @click="clearOrigin">✕</a-button>
            </div>
          </div>

          <!-- Waypoints -->
          <div class="ctrl-section">
            <label class="ctrl-label">途经点</label>
            <div v-for="(wp, i) in waypoints" :key="i" class="waypoint-group">
              <div class="search-wrap">
                <a-input
                  v-model:value="wp.search"
                  :placeholder="`途经点 ${i + 1}`"
                  @input="onWaypointInput(i)"
                >
                  <template #prefix><span class="dot dot-wp">{{ i + 1 }}</span></template>
                </a-input>
                <ul v-if="wp.suggestions.length" class="suggest-list">
                  <li
                    v-for="item in wp.suggestions"
                    :key="item.id"
                    @click="selectWaypoint(i, item)"
                  >
                    <span class="suggest-name">{{ item.name }}</span>
                    <span v-if="item.address" class="suggest-addr">{{ item.address }}</span>
                  </li>
                </ul>
              </div>
              <div v-if="wp.selected" class="selected-point waypoint">
                {{ wp.selected.name }}
                <a-button type="text" size="small" @click="removeWaypoint(i)">✕</a-button>
              </div>
            </div>
            <a-button
              v-if="waypoints.length < 5"
              type="link"
              size="small"
              @click="addWaypoint"
            >
              + 添加途经点
            </a-button>
          </div>

          <!-- Destination -->
          <div class="ctrl-section">
            <label class="ctrl-label required">终点</label>
            <div class="search-wrap">
              <a-input
                v-model:value="destSearch"
                placeholder="搜索城市或景点名称"
                @input="onDestInput"
              >
                <template #prefix><span class="dot dot-dest">终</span></template>
              </a-input>
              <ul v-if="destSuggestions.length" class="suggest-list">
                <li
                  v-for="item in destSuggestions"
                  :key="item.id"
                  @click="selectDest(item)"
                >
                  <span class="suggest-name">{{ item.name }}</span>
                  <span v-if="item.address" class="suggest-addr">{{ item.address }}</span>
                </li>
              </ul>
            </div>
            <div v-if="destPoint" class="selected-point dest">
              {{ destPoint.name }}
              <a-button type="text" size="small" @click="clearDest">✕</a-button>
            </div>
          </div>

          <!-- Calculate -->
          <a-button
            type="primary"
            block
            :disabled="!canCalc"
            :loading="calculating"
            @click="calcRoute"
          >
            计算路线
          </a-button>

          <!-- Result -->
          <div v-if="localResult" class="result-box">
            <CheckCircleFilled style="color: #52c41a" />
            <span class="result-text">
              距离 {{ formatDistance(localResult.distance) }}，约 {{ formatDuration(localResult.duration) }}
            </span>
          </div>

          <!-- Tip -->
          <div class="map-tip">
            提示：也可直接点击地图上的位置快速添加
          </div>
        </div>

        <!-- Right: map -->
        <div ref="mapContainer" class="planner-map"></div>
      </div>

      <!-- Footer -->
      <div class="planner-footer">
        <a-button @click="handleCancel">取消</a-button>
        <a-button type="primary" :disabled="!localResult" @click="handleConfirm">
          确认路线
        </a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { EnvironmentOutlined, DeleteOutlined, CheckCircleFilled } from '@ant-design/icons-vue'
import { getAMap } from '@/utils/amap'

export interface RouteResultData {
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

interface Suggestion {
  id: string
  name: string
  address: string
  location: [number, number]
}

interface PointInfo {
  name: string
  location: [number, number]
}

interface WaypointState {
  search: string
  selected: PointInfo | null
  suggestions: Suggestion[]
}

const emit = defineEmits<{
  (e: 'change', data: RouteResultData | null): void
}>()

// --- External state ---
const routeData = ref<RouteResultData | null>(null)

// --- Dialog state ---
const dialogVisible = ref(false)
const calculating = ref(false)
const localTransport = ref('driving')
const localResult = ref<RouteResultData | null>(null)

// --- Map ---
const mapContainer = ref<HTMLDivElement>()
let map: AMap.Map | null = null
let markers: AMap.Marker[] = []
let polyline: AMap.Polyline | null = null

// --- Origin ---
const originSearch = ref('')
const originSuggestions = ref<Suggestion[]>([])
const originPoint = ref<PointInfo | null>(null)
let originDebounce: ReturnType<typeof setTimeout> | null = null

// --- Destination ---
const destSearch = ref('')
const destSuggestions = ref<Suggestion[]>([])
const destPoint = ref<PointInfo | null>(null)
let destDebounce: ReturnType<typeof setTimeout> | null = null

// --- Waypoints ---
const waypoints = ref<WaypointState[]>([])
let waypointDebounces: (ReturnType<typeof setTimeout> | null)[] = []

const canCalc = computed(() => originPoint.value && destPoint.value)

// --- Open / Close ---
function openDialog() {
  dialogVisible.value = true
}

watch(dialogVisible, async (visible) => {
  if (visible) {
    await nextTick()
    await initMap()
  } else {
    destroyMap()
  }
})

// --- Map lifecycle ---
async function initMap() {
  if (!mapContainer.value) return
  const AMap = await getAMap()
  map = new AMap.Map(mapContainer.value, {
    zoom: 4,
    viewMode: '2D',
  })
  map.addControl(new (AMap as any).ToolBar())
  map.on('click', handleMapClick)

  // If editing existing route, show it on map
  if (routeData.value) {
    loadExistingRouteOntoMap(routeData.value)
  }
}

function destroyMap() {
  clearMapFeatures()
  if (map) {
    map.destroy()
    map = null
  }
}

function clearMapFeatures() {
  markers.forEach(m => m.setMap(null))
  markers = []
  if (polyline) {
    polyline.setMap(null)
    polyline = null
  }
}

// --- Map click ---
function handleMapClick(e: any) {
  const pos: [number, number] = [e.lnglat.getLng(), e.lnglat.getLat()]

  if (!originPoint.value) {
    // Reverse geocode to get a name
    reverseGeocode(pos).then(name => {
      originPoint.value = { name, location: pos }
      originSearch.value = name
      addMarkerToMap(pos, '起', '#1890ff')
    })
  } else if (!destPoint.value) {
    reverseGeocode(pos).then(name => {
      destPoint.value = { name, location: pos }
      destSearch.value = name
      addMarkerToMap(pos, '终', '#ff4d4f')
    })
  } else if (waypoints.value.length < 5) {
    reverseGeocode(pos).then(name => {
      const idx = waypoints.value.length
      waypoints.value.push({ search: name, selected: { name, location: pos }, suggestions: [] })
      addMarkerToMap(pos, String(idx + 1), '#52c41a')
    })
  }
}

async function reverseGeocode(pos: [number, number]): Promise<string> {
  try {
    const AMap = await getAMap()
    return new Promise(resolve => {
      AMap.plugin('AMap.Geocoder', () => {
        const geocoder = new AMap.Geocoder()
        geocoder.getAddress(pos, (status: string, result: any) => {
          if (status === 'complete' && result.regeocode) {
            resolve(result.regeocode.formattedAddress || `(${pos[0].toFixed(4)}, ${pos[1].toFixed(4)})`)
          } else {
            resolve(`(${pos[0].toFixed(4)}, ${pos[1].toFixed(4)})`)
          }
        })
      })
    })
  } catch {
    return `(${pos[0].toFixed(4)}, ${pos[1].toFixed(4)})`
  }
}

function addMarkerToMap(pos: [number, number], label: string, color: string) {
  if (!map) return
  const marker = new AMap.Marker({
    position: pos,
    label: {
      content: `<div style="background:${color};color:#fff;padding:2px 6px;border-radius:4px;font-size:12px;font-weight:bold">${label}</div>`,
      direction: 'top',
    },
  })
  marker.setMap(map)
  markers.push(marker)
}

function syncMarkersFromPoints() {
  clearMapFeatures()
  if (!map) return
  if (originPoint.value) addMarkerToMap(originPoint.value.location, '起', '#1890ff')
  waypoints.value.forEach((wp, i) => {
    if (wp.selected) addMarkerToMap(wp.selected.location, String(i + 1), '#52c41a')
  })
  if (destPoint.value) addMarkerToMap(destPoint.value.location, '终', '#ff4d4f')
  if (polyline && map) {
    polyline.setMap(map)
  }
}

// --- Load existing route for editing ---
function loadExistingRouteOntoMap(data: RouteResultData) {
  localTransport.value = data.transportType

  // Set origin
  originPoint.value = {
    name: data.originName || `起点 (${data.origin[0].toFixed(4)}, ${data.origin[1].toFixed(4)})`,
    location: data.origin,
  }
  originSearch.value = originPoint.value.name
  addMarkerToMap(data.origin, '起', '#1890ff')

  // Set destination
  destPoint.value = {
    name: data.destinationName || `终点 (${data.destination[0].toFixed(4)}, ${data.destination[1].toFixed(4)})`,
    location: data.destination,
  }
  destSearch.value = destPoint.value.name
  addMarkerToMap(data.destination, '终', '#ff4d4f')

  // Set waypoints
  waypoints.value = []
  data.waypoints.forEach((wp, i) => {
    const name = data.waypointNames?.[i] || `途经点 ${i + 1} (${wp[0].toFixed(4)}, ${wp[1].toFixed(4)})`
    waypoints.value.push({ search: name, selected: { name, location: wp }, suggestions: [] })
    addMarkerToMap(wp, String(i + 1), '#52c41a')
  })

  // Draw polyline
  if (data.polyline.length > 0) {
    polyline = new AMap.Polyline({
      path: data.polyline as any,
      strokeColor: '#1890ff',
      strokeWeight: 6,
      strokeOpacity: 0.8,
      showDir: true,
    })
    polyline.setMap(map!)
    map!.setFitView([polyline, ...markers])
  }

  // Show result info
  localResult.value = data
}

// --- PlaceSearch helpers ---
async function searchPlaces(keyword: string): Promise<Suggestion[]> {
  if (!keyword.trim()) return []
  const AMap = await getAMap()
  return new Promise(resolve => {
    AMap.plugin('AMap.PlaceSearch', () => {
      const ps = new AMap.PlaceSearch({ city: '', pageSize: 6 })
      ps.search(keyword, (status: string, result: any) => {
        if (status === 'complete' && result.poiList?.pois) {
          resolve(
            result.poiList.pois.map((p: any) => ({
              id: p.id,
              name: p.name,
              address: p.pname && p.cityname
                ? `${p.pname}${p.cityname !== p.pname ? ' · ' + p.cityname : ''}${p.address ? ' · ' + p.address : ''}`
                : p.address || '',
              location: [p.location.lng, p.location.lat] as [number, number],
            }))
          )
        } else {
          resolve([])
        }
      })
    })
  })
}

// --- Origin search ---
function onOriginInput() {
  if (originDebounce) clearTimeout(originDebounce)
  originDebounce = setTimeout(async () => {
    originSuggestions.value = await searchPlaces(originSearch.value)
  }, 300)
}

function selectOrigin(item: Suggestion) {
  originPoint.value = { name: item.name, location: item.location }
  originSearch.value = item.name
  originSuggestions.value = []
  syncMarkersFromPoints()
}

function clearOrigin() {
  originPoint.value = null
  originSearch.value = ''
  originSuggestions.value = []
  localResult.value = null
  syncMarkersFromPoints()
}

// --- Destination search ---
function onDestInput() {
  if (destDebounce) clearTimeout(destDebounce)
  destDebounce = setTimeout(async () => {
    destSuggestions.value = await searchPlaces(destSearch.value)
  }, 300)
}

function selectDest(item: Suggestion) {
  destPoint.value = { name: item.name, location: item.location }
  destSearch.value = item.name
  destSuggestions.value = []
  syncMarkersFromPoints()
}

function clearDest() {
  destPoint.value = null
  destSearch.value = ''
  destSuggestions.value = []
  localResult.value = null
  syncMarkersFromPoints()
}

// --- Waypoint management ---
function addWaypoint() {
  if (waypoints.value.length >= 5) return
  waypoints.value.push({ search: '', selected: null, suggestions: [] })
  waypointDebounces.push(null)
}

function removeWaypoint(index: number) {
  waypoints.value.splice(index, 1)
  waypointDebounces.splice(index, 1)
  localResult.value = null
  syncMarkersFromPoints()
}

function onWaypointInput(index: number) {
  if (waypointDebounces[index]) clearTimeout(waypointDebounces[index])
  waypointDebounces[index] = setTimeout(async () => {
    const wp = waypoints.value[index]
    if (!wp) return
    wp.suggestions = await searchPlaces(wp.search)
  }, 300)
}

function selectWaypoint(index: number, item: Suggestion) {
  const wp = waypoints.value[index]
  if (!wp) return
  wp.selected = { name: item.name, location: item.location }
  wp.search = item.name
  wp.suggestions = []
  syncMarkersFromPoints()
}

// --- Route search helper (single leg) ---
function searchLeg(AMap: any, from: [number, number], to: [number, number], type: string): Promise<any> {
  return new Promise((resolve, reject) => {
    if (type === 'walking') {
      AMap.plugin('AMap.Walking', () => {
        const s = new (AMap as any).Walking()
        s.search(from, to, (status: string, res: any) => {
          if (status === 'complete') resolve(res)
          else reject(new Error('步行段规划失败'))
        })
      })
    } else if (type === 'riding') {
      AMap.plugin('AMap.Riding', () => {
        const s = new (AMap as any).Riding()
        s.search(from, to, (status: string, res: any) => {
          if (status === 'complete') resolve(res)
          else reject(new Error('骑行段规划失败'))
        })
      })
    } else {
      AMap.plugin('AMap.Driving', () => {
        const s = new (AMap as any).Driving({
          policy: (AMap as any).DrivingPolicy.LEAST_TIME,
          extensions: 'all',
        })
        s.search(from, to, (status: string, res: any) => {
          if (status === 'complete') resolve(res)
          else reject(new Error('驾车段规划失败'))
        })
      })
    }
  })
}

// --- Calculate route (multi-leg to support waypoints for all transport types) ---
async function calcRoute() {
  if (!originPoint.value || !destPoint.value || !map) return
  calculating.value = true

  if (polyline) {
    polyline.setMap(null)
    polyline = null
  }

  const AMap = await getAMap()
  const allPoints: [number, number][] = [
    originPoint.value.location,
    ...waypoints.value.filter(w => w.selected).map(w => w.selected!.location),
    destPoint.value.location,
  ]

  const allPaths: [number, number][] = []
  let totalDistance = 0
  let totalDuration = 0

  try {
    // Calculate leg by leg — each leg is origin→wp1, wp1→wp2, ..., wpN→destination
    for (let i = 0; i < allPoints.length - 1; i++) {
      const result = await searchLeg(AMap, allPoints[i], allPoints[i + 1], localTransport.value)
      const route = result.routes[0]
      route.steps.forEach((step: any) => {
        step.path.forEach((p: AMap.LngLat) => {
          allPaths.push([p.getLng(), p.getLat()])
        })
      })
      totalDistance += route.distance
      totalDuration += route.time
    }
  } catch {
    message.error('路线规划失败，请检查途经点是否合理')
    calculating.value = false
    return
  }

  // Draw polyline
  polyline = new AMap.Polyline({
    path: allPaths as any,
    strokeColor: '#1890ff',
    strokeWeight: 6,
    strokeOpacity: 0.8,
    showDir: true,
  })
  polyline.setMap(map)
  map.setFitView([polyline, ...markers])

  const wpCoords = allPoints.slice(1, -1)
  localResult.value = {
    transportType: localTransport.value,
    origin: allPoints[0],
    destination: allPoints[allPoints.length - 1],
    waypoints: wpCoords,
    polyline: allPaths,
    distance: totalDistance,
    duration: totalDuration,
    originName: originPoint.value.name,
    destinationName: destPoint.value.name,
    waypointNames: waypoints.value.filter(w => w.selected).map(w => w.selected!.name),
  }
  calculating.value = false
}

// --- Confirm / Cancel ---
function handleConfirm() {
  if (!localResult.value) return
  routeData.value = localResult.value
  emit('change', localResult.value)
  dialogVisible.value = false
}

function handleCancel() {
  // Reset local state (don't save changes)
  localResult.value = routeData.value
  resetLocalState()
  dialogVisible.value = false
}

function clearRoute() {
  routeData.value = null
  emit('change', null)
}

function resetLocalState() {
  originPoint.value = null
  originSearch.value = ''
  originSuggestions.value = []
  destPoint.value = null
  destSearch.value = ''
  destSuggestions.value = []
  waypoints.value = []
  waypointDebounces = []
  localResult.value = null
  localTransport.value = 'driving'
  clearMapFeatures()
}

// --- Format helpers ---
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
.route-summary-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.2s;
}
.route-summary-bar:hover {
  background: #d9f7be;
}
.route-summary-left {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #333;
}
.route-badge {
  color: #52c41a;
  font-weight: 500;
}
.divider {
  color: #d9d9d9;
}

/* Planner layout */
.planner {
  display: flex;
  gap: 0;
  height: 520px;
}
.planner-controls {
  width: 280px;
  min-width: 280px;
  padding: 16px;
  overflow-y: auto;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.planner-map {
  flex: 1;
  height: 520px;
}

/* Control sections */
.ctrl-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.ctrl-label {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}
.ctrl-label.required::before {
  content: '* ';
  color: #ff4d4f;
}

/* Search */
.search-wrap {
  position: relative;
}
.suggest-list {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 100;
  list-style: none;
  margin: 2px 0;
  padding: 4px 0;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  max-height: 200px;
  overflow-y: auto;
}
.suggest-list li {
  padding: 6px 10px;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.suggest-list li:hover {
  background: #e6f7ff;
}
.suggest-name {
  color: #333;
}
.suggest-addr {
  color: #999;
  font-size: 11px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Selected point display */
.selected-point {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  margin-top: 2px;
}
.selected-point.origin {
  background: #e6f7ff;
  color: #1890ff;
}
.selected-point.dest {
  background: #fff2f0;
  color: #ff4d4f;
}
.selected-point.waypoint {
  background: #f6ffed;
  color: #52c41a;
}

/* Dot prefix */
.dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  font-size: 10px;
  font-weight: bold;
  color: #fff;
  line-height: 1;
}
.dot-origin { background: #1890ff; }
.dot-dest { background: #ff4d4f; }
.dot-wp { background: #52c41a; }

/* Waypoint group */
.waypoint-group + .waypoint-group {
  margin-top: 4px;
}

.waypoint-group .search-wrap {
  flex: 1;
}

/* Result */
.result-box {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 8px 10px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 4px;
  font-size: 13px;
}
.result-text {
  color: #333;
  line-height: 1.5;
}

/* Tip */
.map-tip {
  font-size: 11px;
  color: #bbb;
  text-align: center;
}

/* Footer */
.planner-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 0 0;
  border-top: 1px solid #f0f0f0;
  margin-top: 0;
}
</style>
