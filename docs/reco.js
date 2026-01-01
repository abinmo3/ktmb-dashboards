const serviceEl = document.getElementById("service");
const originEl = document.getElementById("origin");
const destEl = document.getElementById("destination");

const metaEl = document.getElementById("meta");
const liveEl = document.getElementById("live");

const latestBestEl = document.getElementById("latestBest");
const latestAvoidEl = document.getElementById("latestAvoid");
const histBestEl = document.getElementById("histBest");
const histAvoidEl = document.getElementById("histAvoid");
const histHintEl = document.getElementById("histHint");

let currentService = serviceEl?.value || "komuter";
let byOrigin = null;
let bbox = null;

let FeedMessageType = null;

function dataRoot(){
  return currentService === "komuter" ? "./data" : `./data/${currentService}`;
}

async function fetchJson(rel){
  const res = await fetch(`${dataRoot()}/${rel}`, { cache: "no-store" });
  if(!res.ok) throw new Error(`Fetch failed: ${rel} (${res.status})`);
  return res.json();
}

function slugify(s){
  return s.toLowerCase().replace(/[^a-z0-9]+/g,"-").replace(/(^-|-$)/g,"");
}
function hourLabel(h){ return String(h).padStart(2,"0")+":00"; }

function best3Windows(values){
  const pairs = values.map((v,h)=>({h,v})).filter(o=>Number.isFinite(o.v));
  pairs.sort((a,b)=>a.v-b.v);
  return pairs.slice(0,3).map(o=>o.h);
}
function avoidWindows(values){
  const pairs = values.map((v,h)=>({h,v})).filter(o=>Number.isFinite(o.v));
  pairs.sort((a,b)=>b.v-a.v);
  return pairs.slice(0,1).map(o=>o.h); // top 1 busiest hour block
}
function fmtWindows(hours){
  return hours.map(h => `${hourLabel(h)}–${hourLabel(h+1)}`).join(", ");
}

function inBbox(lat, lon){
  if(!bbox) return true;
  return lat>=bbox.min_lat && lat<=bbox.max_lat && lon>=bbox.min_lon && lon<=bbox.max_lon;
}

async function loadStations(){
  const stations = await fetchJson("stations.json");
  originEl.innerHTML =
    `<option value="">Origin</option>` +
    stations.map(s=>`<option value="${s.name}">${s.name}</option>`).join("");
}

async function loadMeta(){
  const meta = await fetchJson("meta.json");
  metaEl.textContent = `Latest ridership date: ${meta.latest_date}`;
}

async function loadBBox(){
  try {
    bbox = await fetchJson("bbox.json");
  } catch {
    bbox = null;
  }
}

async function onServiceChange(){
  currentService = serviceEl.value;

  byOrigin = null;
  originEl.value = "";
  destEl.disabled = true;
  destEl.innerHTML = `<option value="">Destination</option>`;

  latestBestEl.textContent = "Best: –";
  latestAvoidEl.textContent = "Avoid: –";
  histBestEl.textContent = "Best: –";
  histAvoidEl.textContent = "Avoid: –";

  await loadStations();
  await loadMeta();
  await loadBBox();
}

async function onOriginChange(){
  const origin = originEl.value;

  destEl.disabled = true;
  destEl.innerHTML = `<option value="">Destination</option>`;

  latestBestEl.textContent = "Best: –";
  latestAvoidEl.textContent = "Avoid: –";
  histBestEl.textContent = "Best: –";
  histAvoidEl.textContent = "Avoid: –";

  if(!origin) return;

  const slug = slugify(origin);
  byOrigin = await fetchJson(`by_origin/${slug}.json`);

  const destNames = Object.keys(byOrigin.destinations).sort();
  destEl.innerHTML = `<option value="">Destination</option>` +
    destNames.map(d=>`<option value="${d}">${d}</option>`).join("");
  destEl.disabled = false;
}

function renderReco(titlePrefix, values, bestEl, avoidEl){
  const best = best3Windows(values);
  const avoid = avoidWindows(values);

  bestEl.textContent = `Best: ${fmtWindows(best) || "–"}`;
  avoidEl.textContent = `Avoid: ${fmtWindows(avoid) || "–"}`;

  // (titlePrefix kept for future use)
}

async function onDestChange(){
  const dest = destEl.value;
  if(!dest || !byOrigin) return;

  const rec = byOrigin.destinations[dest];

  // “Latest day” (not real-time crowd)
  const latestValues = (rec.today && rec.today.length === 24) ? rec.today : rec.baseline;
  renderReco("Latest", latestValues, latestBestEl, latestAvoidEl);

  // “24 months typical”
  if (rec.baseline_730 && rec.baseline_730.length === 24){
    renderReco("24m", rec.baseline_730, histBestEl, histAvoidEl);
    histHintEl.textContent = "Based on last 24 months hourly pattern.";
  } else {
    // fallback
    renderReco("Fallback", rec.baseline, histBestEl, histAvoidEl);
    histHintEl.textContent = "baseline_730 not found yet — using baseline fallback.";
  }

  metaEl.textContent = `Ridership date: ${byOrigin.latest_date} · ${byOrigin.origin} → ${dest}`;
}

// Live trains count (real-time)
async function initLiveCount(){
  try{
    const root = await protobuf.load("./vendor/gtfs-realtime.proto");
    FeedMessageType = root.lookupType("transit_realtime.FeedMessage");
    await refreshLive();
    setInterval(refreshLive, 30000);
  }catch(e){
    console.error(e);
    liveEl.textContent = "Live trains: unavailable";
  }
}

async function refreshLive(){
  try{
    const url = "https://ktmb-gtfs-proxy.abinmo3.workers.dev/";
    const res = await fetch(url, { cache: "no-store" });
    const buf = new Uint8Array(await res.arrayBuffer());
    const feed = FeedMessageType.decode(buf);

    const vehicles = (feed.entity || [])
      .map(e => e.vehicle)
      .filter(v => v && v.position && Number.isFinite(v.position.latitude) && Number.isFinite(v.position.longitude))
      .map(v => ({ lat: v.position.latitude, lon: v.position.longitude }));

    const filtered = vehicles.filter(v => inBbox(v.lat, v.lon));
    liveEl.textContent = `Live trains: ${filtered.length} active · updated ${new Date().toLocaleTimeString()}`;
  }catch(e){
    console.error(e);
    liveEl.textContent = "Live trains: unavailable";
  }
}

// init
(async function init(){
  serviceEl.addEventListener("change", onServiceChange);
  originEl.addEventListener("change", onOriginChange);
  destEl.addEventListener("change", onDestChange);

  await loadStations();
  await loadMeta();
  await loadBBox();
  initLiveCount();
})();
