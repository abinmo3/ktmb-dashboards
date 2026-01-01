// =====================
// DOM
// =====================
const serviceEl = document.getElementById("service");
const originEl = document.getElementById("origin");
const destEl = document.getElementById("destination");
const metaEl = document.getElementById("meta");
const picksEl = document.getElementById("picks");
const liveEl = document.getElementById("live");

const mapDetails = document.getElementById("liveMapDetails");
const canvas = document.getElementById("heatmap");
const ctx = canvas.getContext("2d");

let FeedMessageType = null;

// =====================
// State
// =====================
let stations = [];
let byOrigin = null;
let meta = null;

let currentService = serviceEl?.value || "komuter";

// Map state
let map = null;
let routeLayer = null;
let trainLayer = null;
let bbox = null;

// =====================
// Helpers
// =====================
function slugify(s){
  return s.toLowerCase().replace(/[^a-z0-9]+/g,"-").replace(/(^-|-$)/g,"");
}
function hourLabel(h){ return String(h).padStart(2,"0")+":00"; }

function dataRoot(){
  // komuter uses /data, komuter_utara uses /data/komuter_utara
  return currentService === "komuter" ? "./data" : `./data/${currentService}`;
}

async function fetchJson(rel){
  const res = await fetch(`${dataRoot()}/${rel}`, { cache: "no-store" });
  if(!res.ok) throw new Error(`Fetch failed: ${rel} (${res.status})`);
  return res.json();
}

// =====================
// Heatmap
// =====================
function quantile(sorted, q){
  const pos = (sorted.length - 1) * q;
  const base = Math.floor(pos);
  const rest = pos - base;
  if (sorted[base+1] === undefined) return sorted[base];
  return sorted[base] + rest * (sorted[base+1] - sorted[base]);
}

function bucketize(values){
  const v = values.filter(x => Number.isFinite(x)).slice().sort((a,b)=>a-b);
  if (v.length < 5) return values.map(_ => 1);
  const q25 = quantile(v, 0.25);
  const q50 = quantile(v, 0.50);
  const q75 = quantile(v, 0.75);
  return values.map(x => {
    if (!Number.isFinite(x)) return 1;
    if (x <= q25) return 0;
    if (x <= q50) return 1;
    if (x <= q75) return 2;
    return 3;
  });
}

function colorForBucket(b){
  if (b === 0) return "#d9fbe5";
  if (b === 1) return "#fff7cc";
  if (b === 2) return "#ffe0cc";
  return "#ffd1d1";
}

function drawHeatmap(hourValues){
  ctx.clearRect(0,0,canvas.width,canvas.height);

  const buckets = bucketize(hourValues);
  const cellW = canvas.width / 24;
  const cellH = 80;

  for(let h=0; h<24; h++){
    ctx.fillStyle = colorForBucket(buckets[h]);
    ctx.fillRect(h*cellW, 20, cellW-2, cellH);
  }

  ctx.fillStyle = "#666";
  ctx.font = "12px system-ui";
  for(let h=0; h<24; h+=2){
    ctx.fillText(hourLabel(h), h*cellW+2, 14);
  }
}

function best3Windows(hourValues){
  const pairs = hourValues.map((v,h)=>({h,v})).filter(o=>Number.isFinite(o.v));
  pairs.sort((a,b)=>a.v-b.v);
  return pairs.slice(0,3).map(o=>o.h);
}
function avoidWindows(hourValues){
  const pairs = hourValues.map((v,h)=>({h,v})).filter(o=>Number.isFinite(o.v));
  pairs.sort((a,b)=>b.v-a.v);
  return pairs.slice(0,2).map(o=>o.h);
}
function renderPicks(hourValues){
  const best = best3Windows(hourValues);
  const avoid = avoidWindows(hourValues);
  const bestText = best.map(h=>`${hourLabel(h)}–${hourLabel(h+1)}`).join(", ");
  const avoidText = avoid.map(h=>`${hourLabel(h)}–${hourLabel(h+1)}`).join(", ");

  picksEl.innerHTML = `
    <b>Best boarding windows (latest data):</b> ${bestText}<br/>
    <b>Avoid if possible:</b> ${avoidText}
  `;
}

// =====================
// Data loaders
// =====================
async function loadStations(){
  stations = await fetchJson("stations.json");
  originEl.innerHTML =
    `<option value="">Origin</option>` +
    stations.map(s=>`<option value="${s.name}">${s.name}</option>`).join("");
}

async function loadMeta(){
  meta = await fetchJson("meta.json");
  metaEl.textContent = `Latest ridership date: ${meta.latest_date} · Last refresh: ${meta.generated_at}`;
}

async function onOriginChange(){
  const origin = originEl.value;
  destEl.disabled = true;
  destEl.innerHTML = `<option value="">Destination</option>`;
  picksEl.innerHTML = "";
  drawHeatmap(new Array(24).fill(NaN));

  if(!origin) return;

  const slug = slugify(origin);
  byOrigin = await fetchJson(`by_origin/${slug}.json`);

  const destNames = Object.keys(byOrigin.destinations).sort();
  destEl.innerHTML = `<option value="">Destination</option>` +
    destNames.map(d=>`<option value="${d}">${d}</option>`).join("");
  destEl.disabled = false;
}

async function onDestChange(){
  const dest = destEl.value;
  if(!dest || !byOrigin) return;

  const rec = byOrigin.destinations[dest];
  const values = (rec.today && rec.today.length === 24) ? rec.today : rec.baseline;

  drawHeatmap(values);
  renderPicks(values);

  metaEl.textContent = `Latest ridership date: ${byOrigin.latest_date} · Origin: ${byOrigin.origin} → Destination: ${dest}`;
}

async function onServiceChange(){
  currentService = serviceEl.value;

  // reset UI
  byOrigin = null;
  originEl.value = "";
  destEl.disabled = true;
  destEl.innerHTML = `<option value="">Destination</option>`;
  picksEl.innerHTML = "";
  drawHeatmap(new Array(24).fill(NaN));

  // reload data
  try{
    await loadStations();
    await loadMeta();
    await renderRoutesOnMap(); // redraw if map open
  }catch(e){
    console.error(e);
    metaEl.textContent = `No data yet for service: ${currentService} (check /data folders)`;
  }
}

// =====================
// Leaflet map
// =====================
function ensureMap(){
  if(map) return;

  map = L.map("map", { zoomControl: true });

  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 18,
    attribution: "© OpenStreetMap"
  }).addTo(map);

  routeLayer = L.layerGroup().addTo(map);
  trainLayer = L.layerGroup().addTo(map);

  // so user never sees a blank empty box
  map.setView([3.1390, 101.6869], 10);
}

function inBbox(lat, lon){
  if(!bbox) return true;
  return lat>=bbox.min_lat && lat<=bbox.max_lat && lon>=bbox.min_lon && lon<=bbox.max_lon;
}

async function renderRoutesOnMap(){
  if(!mapDetails || !mapDetails.open) return;

  ensureMap();

  // Leaflet needs resize after <details> opens
  setTimeout(() => map.invalidateSize(), 150);

  routeLayer.clearLayers();

  try{
    const routes = await fetchJson("gtfs_routes.json");
    bbox = await fetchJson("bbox.json");

    for(const r of routes){
      L.polyline(r.coords, { weight: 4, opacity: 0.35 }).addTo(routeLayer);
    }

    map.fitBounds([
      [bbox.min_lat, bbox.min_lon],
      [bbox.max_lat, bbox.max_lon]
    ], { padding: [10,10] });
  }catch(e){
    // If you haven’t generated gtfs_routes.json yet, map will still show tiles + train dots
    console.log("Route files not found yet for", currentService);
  }
}

function updateTrainsOnMap(vehicles){
  if(!map || !trainLayer) return;

  trainLayer.clearLayers();

  const filtered = vehicles.filter(v => inBbox(v.lat, v.lon));

  for(const v of filtered){
    const title = v.routeId ? `Route: ${v.routeId}` : "KTMB train";
    const timeText = v.timestamp ? new Date(v.timestamp * 1000).toLocaleTimeString() : "-";

    const popupHtml = `
      <b>${title}</b><br/>
      Trip ID: ${v.tripId || "-"}<br/>
      Vehicle ID: ${v.vehicleId || "-"}<br/>
      Updated: ${timeText}<br/>
      Speed: ${Number.isFinite(v.speed) ? v.speed.toFixed(1) + " m/s" : "-"}<br/>
      Bearing: ${Number.isFinite(v.bearing) ? v.bearing.toFixed(0) + "°" : "-"}<br/>
      Stop ID: ${v.stopId || "-"}<br/>
      Stop seq: ${v.currentStopSequence ?? "-"}
    `;

    const marker = L.circleMarker([v.lat, v.lon], { radius: 6, fillOpacity: 0.85 });
    marker.bindPopup(popupHtml);
    marker.addTo(trainLayer);
  }
}


// =====================
// Live trains (protobuf)
// =====================
async function initLiveLayer() {
  try {
    const root = await protobuf.load("./vendor/gtfs-realtime.proto");
    FeedMessageType = root.lookupType("transit_realtime.FeedMessage");

    await refreshLiveLayer();
    setInterval(refreshLiveLayer, 30000);
  } catch (e) {
    console.error(e);
    if (liveEl) liveEl.textContent = "Live trains: unavailable";
  }
}

async function refreshLiveLayer() {
  try {
    const url = "https://ktmb-gtfs-proxy.abinmo3.workers.dev/";
    const res = await fetch(url, { cache: "no-store" });
    const buf = new Uint8Array(await res.arrayBuffer());
    const feed = FeedMessageType.decode(buf);

   const vehicles = (feed.entity || [])
  .map(e => e.vehicle)
  .filter(v => v && v.position && Number.isFinite(v.position.latitude) && Number.isFinite(v.position.longitude))
  .map(v => {
    const trip = v.trip || {};
    const veh = v.vehicle || {};
    return {
      lat: v.position.latitude,
      lon: v.position.longitude,
      speed: v.position.speed,
      bearing: v.position.bearing,
      timestamp: v.timestamp,

      tripId: trip.tripId || trip.trip_id,
      routeId: trip.routeId || trip.route_id,
      startDate: trip.startDate || trip.start_date,

      vehicleId: veh.id || veh.label,
      stopId: v.stopId || v.stop_id,
      currentStopSequence: v.currentStopSequence || v.current_stop_sequence,
    };
  });


    // update map dots
    updateTrainsOnMap(vehicles);

    if (liveEl) {
      liveEl.textContent = `Live trains: ${vehicles.length} active · updated ${new Date().toLocaleTimeString()}`;
    }
  } catch (e) {
    console.error(e);
    if (liveEl) liveEl.textContent = "Live trains: unavailable";
  }
}

// =====================
// Init
// =====================
(async function init(){
  // listeners
  if(serviceEl) serviceEl.addEventListener("change", onServiceChange);
  originEl.addEventListener("change", onOriginChange);
  destEl.addEventListener("change", onDestChange);

  if(mapDetails) mapDetails.addEventListener("toggle", renderRoutesOnMap);

  // first load
  try{
    await loadStations();
    await loadMeta();
  }catch(e){
    console.error(e);
    metaEl.textContent = "Failed to load /data/*.json (check Cloudflare Pages deploy)";
  }

  drawHeatmap(new Array(24).fill(NaN));

// render map immediately if it's open by default (recommended)
if (mapDetails && mapDetails.open) {
  await renderRoutesOnMap();
}

initLiveLayer();

})();
