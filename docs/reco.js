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

const STATE_BY_STATION_NAME = {
  "Abdullah Hukum": "Kuala Lumpur",
  "Angkasapuri": "Kuala Lumpur",
  "Bandar Tasek Selatan": "Kuala Lumpur",
  "Bank Negara": "Kuala Lumpur",
  "Batu Caves": "Selangor",
  "Batu Kentonmen": "Kuala Lumpur",
  "Batu Tiga": "Selangor",
  "Bangi": "Selangor",
  "Batang Benar": "Negeri Sembilan",
  "Batang Kali": "Selangor",
  "Bukit Badak": "Selangor",
  "Jalan Kastam": "Selangor",
  "Jalan Templer": "Selangor",
  "Kajang": "Selangor",
  "Kajang 2": "Selangor",
  "Kampung Batu": "Kuala Lumpur",
  "Kampung Dato Harun": "Selangor",
  "Kampung Raja Uda": "Selangor",
  "Kepong": "Kuala Lumpur",
  "Kepong Sentral": "Selangor",
  "Klang": "Selangor",
  "KL Sentral": "Kuala Lumpur",
  "Kuala Kubu Bharu": "Selangor",
  "Kuala Lumpur": "Kuala Lumpur",
  "Kuang": "Selangor",
  "Labu": "Negeri Sembilan",
  "Midvalley": "Kuala Lumpur",
  "Nilai": "Negeri Sembilan",
  "Padang Jawa": "Selangor",
  "Pantai Dalam": "Kuala Lumpur",
  "Pelabuhan Klang Selatan": "Selangor",
  "Petaling": "Selangor",
  "Pulau Sebang (Tampin)": "Melaka",
  "Putra": "Kuala Lumpur",
  "Rasa": "Selangor",
  "Rawang": "Selangor",
  "Rembau": "Negeri Sembilan",
  "Rengam": "Johor",
  "Salak Selatan": "Kuala Lumpur",
  "Segambut": "Kuala Lumpur",
  "Senawang": "Negeri Sembilan",
  "Sentul": "Kuala Lumpur",
  "Seputeh": "Kuala Lumpur",
  "Serdang": "Selangor",
  "Seremban": "Negeri Sembilan",
  "Serendah": "Selangor",
  "Seri Setia": "Selangor",
  "Setia Jaya": "Selangor",
  "Shah Alam": "Selangor",
  "Subang Jaya": "Selangor",
  "Sungai Buloh": "Selangor",
  "Sungai Gadut": "Negeri Sembilan",
  "Taman Wahyu": "Kuala Lumpur",
  "Tanjong Malim": "Perak",
  "Telok Gadong": "Selangor",
  "Telok Pulai": "Selangor",
  "Tiroi": "Negeri Sembilan",
  "UKM": "Selangor",
  "Alor Setar": "Kedah",
  "Anak Bukit": "Kedah",
  "Arau": "Perlis",
  "Bagan Serai": "Perak",
  "Bukit Ketri": "Perlis",
  "Bukit Mertajam": "Pulau Pinang",
  "Bukit Tengah": "Pulau Pinang",
  "Butterworth": "Pulau Pinang",
  "Gurun": "Kedah",
  "Ipoh": "Perak",
  "Kamunting": "Perak",
  "Kobah": "Kedah",
  "Kodiang": "Kedah",
  "Kuala Kangsar": "Perak",
  "Nibong Tebal": "Pulau Pinang",
  "Padang Besar": "Perlis",
  "Padang Rengas": "Perak",
  "Parit Buntar": "Perak",
  "Simpang Ampat": "Pulau Pinang",
  "Sungai Petani": "Kedah",
  "Sungai Siput": "Perak",
  "Taiping": "Perak",
  "Tasek Gelugor": "Pulau Pinang"
};

function dataRoot(){
  return currentService === "komuter" ? "./data" : `./data/${currentService}`;
}

async function fetchJson(rel){
  const res = await fetch(`${dataRoot()}/${rel}`, { cache: "no-store" });
  if(!res.ok) throw new Error(`Fetch failed: ${rel} (${res.status})`);
  return res.json();
}

function stationState(name){
  return STATE_BY_STATION_NAME[name] || "Other";
}

function renderStationOptions(selectEl, names, placeholderLabel){
  selectEl.innerHTML = "";
  if (placeholderLabel) {
    const placeholder = document.createElement("option");
    placeholder.value = "";
    placeholder.textContent = placeholderLabel;
    selectEl.appendChild(placeholder);
  }
  const grouped = new Map();

  for(const name of names){
    const state = stationState(name);
    if(!grouped.has(state)) grouped.set(state, []);
    grouped.get(state).push(name);
  }

  const states = Array.from(grouped.keys()).sort((a,b)=>a.localeCompare(b));
  for(const state of states){
    const optgroup = document.createElement("optgroup");
    optgroup.label = state;
    const entries = grouped.get(state).slice().sort((a,b)=>a.localeCompare(b));
    for(const name of entries){
      const option = document.createElement("option");
      option.value = name;
      option.textContent = `${name} (${state})`;
      optgroup.appendChild(option);
    }
    selectEl.appendChild(optgroup);
  }
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
  renderStationOptions(originEl, stations.map(s=>s.name), "Origin");
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
  renderStationOptions(destEl, destNames, "Destination");
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
