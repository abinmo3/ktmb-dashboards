const PROXY_URL = "https://ktmb-gtfs-proxy.abinmo3.workers.dev/";
const REPORT_KEY = "ktmb-android-preview-reports";
const SERVICE_META = {
  komuter: {
    label: "Komuter Klang Valley",
    title: "Komuter",
    defaultRoute: { origin: "KL Sentral", destination: "Bandar Tasek Selatan" },
    popularStations: ["KL Sentral", "Bandar Tasek Selatan", "Kajang", "Subang Jaya", "Seremban", "Sungai Buloh"],
  },
  komuter_utara: {
    label: "Komuter Utara",
    title: "Komuter Utara",
    defaultRoute: { origin: "Butterworth", destination: "Alor Setar" },
    popularStations: ["Butterworth", "Alor Setar", "Sungai Petani", "Ipoh", "Padang Besar", "Arau"],
  },
};

const ICONS = {
  home: iconSvg("M3 10.5 12 3l9 7.5V21a1 1 0 0 1-1 1h-5.5v-6h-5V22H4a1 1 0 0 1-1-1z"),
  forecast: iconSvg("M4 19h16M5 15l4-5 4 3 6-8"),
  live: iconSvg("M4 12h3l2 4 3-8 2 4h6"),
  stations: iconSvg("M7 4h10l3 5-8 12L4 9z"),
  more: iconSvg("M6 7h12M6 12h12M6 17h12"),
  report: iconSvg("M8 4h8l4 4v12H8zM10 12h4M10 16h6"),
  search: iconSvg("M11 4.5a6.5 6.5 0 1 1 0 13 6.5 6.5 0 0 1 0-13zm8.5 15-4.2-4.2"),
  swap: iconSvg("M8 6 4 10l4 4M20 18l-4-4-4 4M4 10h16M20 18H4"),
};

const app = {
  service: "komuter",
  screen: "home",
  forecastMode: "latest",
  stations: [],
  stateMap: {},
  guides: [],
  reports: loadReports(),
  meta: null,
  byOrigin: null,
  origin: "",
  destination: "",
  selectedStation: "",
  live: {
    count: null,
    coverage: "Loading",
    updated: "Loading...",
    status: "loading",
    vehicles: [],
  },
};

const els = {};
let feedType = null;
let liveTimer = null;

function qs(id) {
  return document.getElementById(id);
}

function iconSvg(path) {
  return `
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="${path}"></path>
    </svg>
  `;
}

function slugify(text) {
  return String(text || "")
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/(^-|-$)/g, "");
}

function loadReports() {
  try {
    return JSON.parse(localStorage.getItem(REPORT_KEY) || "[]");
  } catch {
    return [];
  }
}

function saveReports(reports) {
  localStorage.setItem(REPORT_KEY, JSON.stringify(reports));
}

function dataRoot() {
  return app.service === "komuter" ? "./data" : `./data/${app.service}`;
}

async function fetchJson(rel) {
  const res = await fetch(`${dataRoot()}/${rel}`, { cache: "no-store" });
  if (!res.ok) {
    throw new Error(`Fetch failed: ${rel} (${res.status})`);
  }
  return res.json();
}

function hourLabel(hour) {
  return `${String(hour).padStart(2, "0")}:00`;
}

function formatWindow(hour) {
  return `${hourLabel(hour)}–${hourLabel((hour + 1) % 24)}`;
}

function windowsFromValues(values, count, direction) {
  const pairs = values
    .map((value, hour) => ({ value, hour }))
    .filter((item) => Number.isFinite(item.value))
    .sort((a, b) => (direction === "asc" ? a.value - b.value : b.value - a.value));

  return pairs.slice(0, count).map((item) => item.hour);
}

function windowsText(values, count, direction) {
  const hours = windowsFromValues(values, count, direction);
  return hours.length ? hours.map(formatWindow).join(", ") : "—";
}

function windowSummary(values) {
  const finite = values.filter(Number.isFinite);
  if (!finite.length) {
    return { low: 0, mid: 0, high: 0, packed: 0, max: 0 };
  }
  const max = Math.max(...finite);
  const thresholds = {
    low: max * 0.25,
    mid: max * 0.5,
    high: max * 0.75,
  };
  const summary = { low: 0, mid: 0, high: 0, packed: 0, max };
  values.forEach((value) => {
    if (!Number.isFinite(value)) return;
    if (value <= thresholds.low) summary.low += 1;
    else if (value <= thresholds.mid) summary.mid += 1;
    else if (value <= thresholds.high) summary.high += 1;
    else summary.packed += 1;
  });
  return summary;
}

function routeValue(rec, mode) {
  if (!rec) return new Array(24).fill(null);
  if (mode === "typical" && Array.isArray(rec.baseline_730) && rec.baseline_730.length === 24) {
    return rec.baseline_730;
  }
  if (mode === "latest" && Array.isArray(rec.today) && rec.today.length === 24) {
    return rec.today;
  }
  if (Array.isArray(rec.baseline) && rec.baseline.length === 24) {
    return rec.baseline;
  }
  return new Array(24).fill(null);
}

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value));
}

function prettyState(state) {
  return state || "Other";
}

function serviceLabel() {
  return SERVICE_META[app.service]?.label || app.service;
}

function defaultRoute() {
  return SERVICE_META[app.service]?.defaultRoute || { origin: "", destination: "" };
}

function popularStations() {
  const preferred = SERVICE_META[app.service]?.popularStations || [];
  const known = new Set(app.stations.map((station) => station.name));
  const items = preferred.filter((name) => known.has(name));
  return items.length ? items : app.stations.slice(0, 6).map((station) => station.name);
}

function stationState(name) {
  return prettyState(app.stateMap[name]);
}

function groupStations(stations) {
  const grouped = new Map();
  const order = ["Kuala Lumpur", "Selangor", "Negeri Sembilan", "Perak", "Kedah", "Perlis", "Johor", "Pulau Pinang", "Melaka", "Other"];
  stations.forEach((station) => {
    const state = stationState(station.name);
    if (!grouped.has(state)) grouped.set(state, []);
    grouped.get(state).push(station);
  });
  return order
    .filter((state) => grouped.has(state))
    .map((state) => ({ state, stations: grouped.get(state) }));
}

function nearestStationInRoute() {
  if (!app.byOrigin || !app.origin) return "";
  const rec = app.byOrigin.destinations?.[app.destination];
  if (rec) return app.destination;
  const next = Object.keys(app.byOrigin.destinations || {}).find(Boolean);
  return next || "";
}

function coverageLabel(count) {
  if (!Number.isFinite(count)) return "Unavailable";
  if (count === 0) return "Unavailable";
  if (count < 6) return "Low";
  if (count < 14) return "Medium";
  if (count < 24) return "High";
  return "Very high";
}

function currentRouteText() {
  const route = defaultRoute();
  const origin = app.origin || route.origin;
  const destination = app.destination || route.destination;
  return origin && destination ? `${origin} → ${destination}` : "Select a route";
}

function setScreen(screen) {
  app.screen = screen;
  document.querySelectorAll(".screen").forEach((section) => {
    section.classList.toggle("is-active", section.dataset.screen === screen);
  });
  document.querySelectorAll(".nav-item").forEach((item) => {
    item.classList.toggle("is-active", item.dataset.screenTarget === screen);
  });
  const titles = {
    home: "Home",
    forecast: "Forecast",
    live: "Live",
    stations: "Stations",
    more: "More",
  };
  const subtitles = {
    home: "Mobile concept preview",
    forecast: `${serviceLabel()} · ${currentRouteText()}`,
    live: `${serviceLabel()} · live vehicle feed`,
    stations: `${serviceLabel()} · browse station directory`,
    more: `${serviceLabel()} · reports, guides, and notes`,
  };
  els.screenTitle.textContent = titles[screen] || "KTMB Crowd & Trend";
  els.screenSubtitle.textContent = subtitles[screen] || "Android concept preview";
}

function setService(service) {
  app.service = service;
  document.querySelectorAll("[data-service]").forEach((button) => {
    button.classList.toggle("is-active", button.dataset.service === service);
  });
  els.homeServiceBadge.textContent = serviceLabel();
  loadServiceData(service).catch((error) => {
    console.error(error);
    renderFallbackState();
  });
}

function selectLevel(level) {
  app.reportLevel = level;
  els.reportLevel.value = level;
  document.querySelectorAll("[data-level]").forEach((button) => {
    button.classList.toggle("is-active", button.dataset.level === level);
  });
}

function selectForecastMode(mode) {
  app.forecastMode = mode;
  document.querySelectorAll("[data-forecast-mode]").forEach((button) => {
    button.classList.toggle("is-active", button.dataset.forecastMode === mode);
  });
  renderForecast();
}

function setForecastSelection(origin, destination, silent = false) {
  const originKnown = app.stations.some((station) => station.name === origin);
  const destinationKnown = app.byOrigin?.destinations && Object.prototype.hasOwnProperty.call(app.byOrigin.destinations, destination);
  if (originKnown) app.origin = origin;
  if (destinationKnown) app.destination = destination;
  if (els.forecastOrigin) els.forecastOrigin.value = app.origin;
  if (els.forecastDestination) els.forecastDestination.value = app.destination;
  if (!silent) {
    renderForecast();
    renderHome();
    renderStationsDetail();
  }
}

async function loadServiceData(service) {
  const [stations, stateMap, meta, guides] = await Promise.all([
    fetchJson("stations.json"),
    fetchJson("state_map.json"),
    fetchJson("meta.json"),
    fetchJson("guides.json").catch(() => []),
  ]);

  app.stations = stations.filter((station) => {
    const name = station.name;
    return name && name !== "Penalty" && name !== "Unknown";
  });
  app.stateMap = stateMap;
  app.meta = meta;
  app.guides = guides;

  const defaults = defaultRoute();
  const origin = app.stations.some((station) => station.name === defaults.origin)
    ? defaults.origin
    : app.stations[0]?.name || "";

  await loadOrigin(origin);

  const preferredDestination = app.byOrigin?.destinations && Object.prototype.hasOwnProperty.call(app.byOrigin.destinations, defaults.destination)
    ? defaults.destination
    : pickBestDestination(app.byOrigin);

  app.origin = origin;
  app.destination = preferredDestination;
  app.selectedStation = app.origin || app.stations[0]?.name || "";

  renderStaticContent();
  populateForecastControls();
  renderHome();
  renderForecast();
  renderStations();
  renderStationsDetail();
  renderMore();
  setScreen(app.screen);
  startOrRefreshLiveFeed();
}

async function loadOrigin(origin) {
  if (!origin) {
    app.byOrigin = null;
    return;
  }
  const file = `${slugify(origin)}.json`;
  try {
    app.byOrigin = await fetchJson(`by_origin/${file}`);
  } catch {
    app.byOrigin = null;
  }
}

function pickBestDestination(byOrigin) {
  if (!byOrigin?.destinations) return "";
  const entries = Object.entries(byOrigin.destinations);
  if (!entries.length) return "";
  const scored = entries
    .map(([name, rec]) => {
      const values = routeValue(rec, "latest").filter(Number.isFinite);
      const peak = values.length ? Math.max(...values) : 0;
      return { name, peak };
    })
    .sort((a, b) => b.peak - a.peak);
  return scored[0]?.name || entries[0][0];
}

function populateForecastControls() {
  const serviceOptions = [
    { value: "komuter", label: "Komuter (Klang Valley)" },
    { value: "komuter_utara", label: "Komuter Utara (North)" },
  ];

  els.forecastService.innerHTML = serviceOptions
    .map((option) => `<option value="${option.value}">${option.label}</option>`)
    .join("");
  els.forecastService.value = app.service;

  populateOriginOptions();
}

function populateOriginOptions() {
  const options = app.stations
    .slice()
    .sort((a, b) => a.name.localeCompare(b.name))
    .map((station) => `<option value="${station.name}">${station.name} (${stationState(station.name)})</option>`)
    .join("");
  els.forecastOrigin.innerHTML = options;
  els.forecastOrigin.value = app.origin;
  populateDestinationOptions();
}

function populateDestinationOptions() {
  const destinations = Object.keys(app.byOrigin?.destinations || {})
    .sort((a, b) => a.localeCompare(b));
  els.forecastDestination.innerHTML = destinations
    .map((name) => `<option value="${name}">${name}</option>`)
    .join("");
  els.forecastDestination.value = app.destination || destinations[0] || "";
}

function renderStaticContent() {
  els.statusTime.textContent = new Intl.DateTimeFormat("en-MY", {
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date());
  els.homeLatestDate.textContent = app.meta?.latest_date || "Loading...";
  els.homeServiceBadge.textContent = serviceLabel();
  els.moreMeta.textContent = `Data source: data.gov.my + GTFS. Latest ridership date: ${app.meta?.latest_date || "n/a"}. Use for planning only, not as an official KTM service.`;
  els.reportCountBadge.textContent = `${app.reports.length} saved`;
  els.stationCountBadge.textContent = `${app.stations.length} stations`;
  els.forecastService.value = app.service;
  els.reportLevel.value = app.reportLevel || "Low";
}

function renderHome() {
  els.homeLatestDate.textContent = app.meta?.latest_date || "n/a";
  els.homeLiveCount.textContent = Number.isFinite(app.live.count) ? String(app.live.count) : "--";
  els.homeLiveNote.textContent = app.live.updated || "Feed refresh in progress";
  const latestRec = app.byOrigin?.destinations?.[app.destination];
  const latestValues = routeValue(latestRec, "latest");
  els.homeBestWindow.textContent = windowsText(latestValues, 2, "asc");
  els.homeRouteLabel.textContent = currentRouteText();
  els.homeServiceBadge.textContent = serviceLabel();

  const stations = popularStations();
  els.homeStationStrip.innerHTML = stations
    .map((name) => {
      const state = stationState(name);
      const selected = name === app.origin || name === app.destination ? " is-active" : "";
      return `
        <button class="station-mini${selected}" type="button" data-station="${name}">
          <span class="tag">${state}</span>
          <strong>${name}</strong>
          <span class="station-meta">Tap to jump into forecast</span>
        </button>
      `;
    })
    .join("");

  els.homeStationStrip.querySelectorAll("[data-station]").forEach((button) => {
    button.addEventListener("click", async () => {
      const station = button.dataset.station;
      if (!station) return;
      app.selectedStation = station;
      await loadOrigin(station);
      app.origin = station;
      app.destination = pickBestDestination(app.byOrigin) || app.destination;
      populateForecastControls();
      renderForecast();
      renderStationsDetail();
      setScreen("forecast");
    });
  });
}

function renderForecast() {
  const byOrigin = app.byOrigin;
  const rec = byOrigin?.destinations?.[app.destination];
  const values = routeValue(rec, app.forecastMode);
  const latestValues = routeValue(rec, "latest");
  const typicalValues = routeValue(rec, "typical");
  const activeValues = app.forecastMode === "typical" ? typicalValues : latestValues;

  const finite = activeValues.filter(Number.isFinite);
  const max = finite.length ? Math.max(...finite) : 1;

  els.forecastRoute.textContent = currentRouteText();
  els.forecastMeta.textContent = app.meta
    ? `Latest ridership date: ${app.meta.latest_date} · ${app.byOrigin?.origin || app.origin} → ${app.destination || "—"}`
    : "Latest ridership date pending";

  els.forecastHeatmap.innerHTML = activeValues
    .map((value, hour) => {
      const normalized = Number.isFinite(value) && max > 0 ? clamp((value / max) * 100, 8, 100) : 8;
      const levelClass = (() => {
        if (!Number.isFinite(value)) return "is-low";
        if (value <= max * 0.25) return "is-low";
        if (value <= max * 0.5) return "is-mid";
        if (value <= max * 0.75) return "is-high";
        return "is-packed";
      })();
      return `
        <div class="hour ${levelClass}">
          <div class="bar" style="--h:${normalized}"></div>
          <span class="hour-label">${hour % 3 === 0 ? String(hour).padStart(2, "0") : ""}</span>
        </div>
      `;
    })
    .join("");

  els.forecastBest.textContent = windowsText(activeValues, 3, "asc");
  els.forecastAvoid.textContent = windowsText(activeValues, 2, "desc");

  const latestBest = windowsText(latestValues, 3, "asc");
  const latestAvoid = windowsText(latestValues, 1, "desc");
  const typicalBest = windowsText(typicalValues, 3, "asc");
  const typicalAvoid = windowsText(typicalValues, 2, "desc");

  els.forecastNote.textContent = `Latest-day best windows: ${latestBest} · avoid: ${latestAvoid}. Typical 24m best windows: ${typicalBest} · avoid: ${typicalAvoid}.`;
}

function renderLiveMap() {
  const count = Number.isFinite(app.live.count) ? app.live.count : 0;
  const coverage = coverageLabel(count);

  els.liveCount.textContent = Number.isFinite(app.live.count) ? String(app.live.count) : "--";
  els.liveCoverage.textContent = coverage;
  els.liveUpdated.textContent = app.live.updated || "—";
  els.liveRefreshBadge.textContent = app.live.status === "ok" ? "Refreshing every 30s" : "Feed pending";

  const vehicles = app.live.vehicles.slice(0, 4);
  els.liveTrainList.innerHTML = vehicles.length
    ? vehicles
        .map((vehicle, index) => {
          const route = vehicle.routeId || vehicle.tripId || "KTMB";
          const vehicleLabel = vehicle.vehicleId || vehicle.tripId || `Train ${index + 1}`;
          const speed = Number.isFinite(vehicle.speed) ? `${vehicle.speed.toFixed(1)} m/s` : "Speed n/a";
          const updated = vehicle.timestamp ? new Date(vehicle.timestamp * 1000).toLocaleTimeString([], { hour: "numeric", minute: "2-digit" }) : "just now";
          return `
            <article class="train-row">
              <div class="train-chip">${String(index + 1)}</div>
              <div>
                <strong>${vehicleLabel}</strong>
                <span>${route} · ${speed}</span>
              </div>
              <span>${updated}</span>
            </article>
          `;
        })
        .join("")
    : `
        <article class="train-row">
          <div class="train-chip">--</div>
          <div>
            <strong>Waiting for live feed</strong>
            <span>The summary will fill in when the GTFS-realtime feed arrives.</span>
          </div>
          <span>--</span>
        </article>
      `;

  const markers = [
    { x: 25, y: 72, delay: "0s", label: "KL" },
    { x: 44, y: 47, delay: "0.8s", label: "Batu" },
    { x: 60, y: 78, delay: "1.5s", label: "Sentral" },
    { x: 78, y: 38, delay: "0.4s", label: "Jaya" },
    { x: 83, y: 70, delay: "1.1s", label: "BTS" },
  ];

  els.liveMapArt.innerHTML = `
    <svg viewBox="0 0 640 320" preserveAspectRatio="none" aria-hidden="true">
      <defs>
        <linearGradient id="railGlow" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="rgba(255,255,255,0.9)"/>
          <stop offset="100%" stop-color="rgba(255,255,255,0.5)"/>
        </linearGradient>
      </defs>
      <path d="M70 250 C160 220, 195 120, 290 120 S420 210, 515 160 S585 110, 600 78" stroke="url(#railGlow)" stroke-width="5" fill="none" stroke-linecap="round" opacity="0.88"/>
      <path d="M58 86 C150 58, 205 82, 286 145 S432 226, 564 238" stroke="rgba(255,255,255,0.65)" stroke-width="4" fill="none" stroke-linecap="round"/>
      <path d="M126 44 C190 96, 228 126, 296 154 S428 208, 540 260" stroke="rgba(255,255,255,0.32)" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-dasharray="9 9"/>
      ${markers
        .map(
          (marker, index) => `
            <g transform="translate(${(marker.x / 100) * 640}, ${(marker.y / 100) * 320})">
              <circle class="route-dot ${index === 1 ? "alt" : index === 3 ? "alt-2" : ""}" cx="0" cy="0" r="10" style="animation-delay:${marker.delay};"></circle>
            </g>
          `,
        )
        .join("")}
    </svg>
  `;
}

function renderStations() {
  const query = (els.stationSearch.value || "").toLowerCase().trim();
  const filtered = app.stations.filter((station) => {
    const name = station.name.toLowerCase();
    const state = stationState(station.name).toLowerCase();
    return !query || name.includes(query) || state.includes(query);
  });

  els.stationCountBadge.textContent = `${filtered.length} stations`;

  const grouped = groupStations(filtered);
  if (!filtered.length) {
    els.stationGroups.innerHTML = `
      <article class="inline-note">
        No stations match “${els.stationSearch.value.trim()}”. Try another station or state.
      </article>
    `;
    return;
  }

  els.stationGroups.innerHTML = grouped
    .map(
      ({ state, stations }) => `
        <section class="state-group">
          <div class="section-head tight">
            <div>
              <span class="section-kicker">${state}</span>
              <h3>${stations.length} station${stations.length === 1 ? "" : "s"}</h3>
            </div>
          </div>
          ${stations
            .map(
              (station) => `
                <button class="station-card" type="button" data-station-card="${station.name}">
                  <div>
                    <strong>${station.name}</strong>
                    <div class="state">${state}</div>
                  </div>
                  <span class="caret">›</span>
                </button>
              `,
            )
            .join("")}
        </section>
      `,
    )
    .join("");

  els.stationGroups.querySelectorAll("[data-station-card]").forEach((button) => {
    button.addEventListener("click", async () => {
      const station = button.dataset.stationCard;
      if (!station) return;
      app.selectedStation = station;
      await loadOrigin(station);
      app.origin = station;
      app.destination = pickBestDestination(app.byOrigin) || app.destination;
      populateForecastControls();
      renderForecast();
      renderStationsDetail();
      setScreen("forecast");
    });
  });
}

function renderStationsDetail() {
  const station = app.stations.find((entry) => entry.name === app.selectedStation) || app.stations.find((entry) => entry.name === app.origin) || app.stations[0];
  if (!station) {
    els.stationDetail.innerHTML = `<p class="metric-note">No station data available yet.</p>`;
    return;
  }

  const currentRoute = app.byOrigin?.destinations?.[app.destination];
  const latestValues = routeValue(currentRoute, "latest");
  const typicalValues = routeValue(currentRoute, "typical");

  els.stationDetail.innerHTML = `
    <span class="surface-chip">${stationState(station.name)}</span>
    <h4>${station.name}</h4>
    <p>${currentRoute ? "Crowd windows shown for the currently selected route." : "Use this station as a launchpad for forecast and live views."}</p>
    <div class="route-summary" style="margin-top:12px;">
      <div>
        <span class="route-label">Default route</span>
        <strong>${currentRouteText()}</strong>
      </div>
      <div class="route-subline">${app.meta?.latest_date || "n/a"}</div>
    </div>
    <div class="detail-actions">
      <button class="detail-button" type="button" data-detail-action="forecast">Open forecast</button>
      <button class="detail-button" type="button" data-detail-action="live">Live map</button>
      <button class="detail-button" type="button" data-detail-action="report">Report crowd</button>
    </div>
    <div class="dual-grid" style="margin-top:12px;">
      <article class="metric-card">
        <span class="metric-label">Latest best</span>
        <strong>${windowsText(latestValues, 3, "asc")}</strong>
        <span class="metric-note">Latest-day crowd windows</span>
      </article>
      <article class="metric-card">
        <span class="metric-label">Typical best</span>
        <strong>${windowsText(typicalValues, 3, "asc")}</strong>
        <span class="metric-note">24-month normal pattern</span>
      </article>
    </div>
  `;

  els.stationDetail.querySelectorAll("[data-detail-action]").forEach((button) => {
    button.addEventListener("click", async () => {
      const action = button.dataset.detailAction;
      if (action === "forecast") {
        await loadOrigin(station.name);
        app.origin = station.name;
        app.destination = pickBestDestination(app.byOrigin) || app.destination;
        populateForecastControls();
        renderForecast();
        setScreen("forecast");
      }
      if (action === "live") {
        setScreen("live");
      }
      if (action === "report") {
        setScreen("more");
        els.reportStation.value = station.name;
      }
    });
  });
}

function renderReports() {
  els.reportCountBadge.textContent = `${app.reports.length} saved`;
  if (!app.reports.length) {
    els.reportList.innerHTML = `
      <article class="report-card">
        <strong>No reports yet</strong>
        <span class="report-meta">Share the first crowd update for your station.</span>
      </article>
    `;
    return;
  }

  els.reportList.innerHTML = app.reports
    .slice(0, 6)
    .map(
      (report) => `
        <article class="report-card">
          <strong>${report.station} · ${report.level}</strong>
          <span class="report-meta">${report.time}</span>
          ${report.notes ? `<span class="report-meta">${report.notes}</span>` : ""}
        </article>
      `,
    )
    .join("");
}

function renderGuides() {
  if (!app.guides.length) {
    els.guideList.innerHTML = `
      <article class="guide-card">
        <strong>Guides unavailable</strong>
        <span class="guide-meta">The guide list will appear when the data file loads.</span>
      </article>
    `;
    return;
  }

  els.guideList.innerHTML = app.guides
    .map(
      (guide) => `
        <article class="guide-card">
          <span class="surface-chip">${guide.readTime}</span>
          <h4>${guide.title}</h4>
          <span class="guide-meta">${guide.summary}</span>
          <span class="guide-meta">Updated ${guide.updated}</span>
        </article>
      `,
    )
    .join("");
}

function renderMore() {
  renderReports();
  renderGuides();
}

function renderFallbackState() {
  app.live.count = null;
  app.live.coverage = "Unavailable";
  app.live.updated = "Feed unavailable";
  app.live.vehicles = [];
  renderHome();
  renderForecast();
  renderLiveMap();
}

async function initLiveFeed() {
  try {
    const root = await protobuf.load("./vendor/gtfs-realtime.proto");
    feedType = root.lookupType("transit_realtime.FeedMessage");
    await refreshLiveFeed();
    if (liveTimer) clearInterval(liveTimer);
    liveTimer = setInterval(refreshLiveFeed, 30000);
  } catch (error) {
    console.error(error);
    app.live.count = null;
    app.live.coverage = "Unavailable";
    app.live.updated = "Feed unavailable";
    app.live.status = "error";
    app.live.vehicles = [];
    renderLiveMap();
    renderHome();
  }
}

async function refreshLiveFeed() {
  try {
    const res = await fetch(PROXY_URL, { cache: "no-store" });
    const bytes = new Uint8Array(await res.arrayBuffer());
    const feed = feedType.decode(bytes);
    const vehicles = (feed.entity || [])
      .map((entity) => entity.vehicle)
      .filter((vehicle) => vehicle && vehicle.position && Number.isFinite(vehicle.position.latitude) && Number.isFinite(vehicle.position.longitude))
      .map((vehicle) => ({
        lat: vehicle.position.latitude,
        lon: vehicle.position.longitude,
        speed: vehicle.position.speed,
        timestamp: vehicle.timestamp,
        routeId: vehicle.trip?.routeId || vehicle.trip?.route_id || "",
        tripId: vehicle.trip?.tripId || vehicle.trip?.trip_id || "",
        vehicleId: vehicle.vehicle?.id || vehicle.vehicle?.label || "",
      }));

    app.live.vehicles = vehicles;
    app.live.count = vehicles.length;
    app.live.coverage = coverageLabel(vehicles.length);
    app.live.updated = `Updated ${new Date().toLocaleTimeString([], { hour: "numeric", minute: "2-digit" })}`;
    app.live.status = "ok";
  } catch (error) {
    console.error(error);
    app.live.count = null;
    app.live.coverage = "Unavailable";
    app.live.updated = "Feed unavailable";
    app.live.status = "error";
    app.live.vehicles = [];
  }
  renderLiveMap();
  renderHome();
  if (app.screen === "live") setScreen("live");
}

function startOrRefreshLiveFeed() {
  if (!feedType) {
    renderLiveMap();
    renderHome();
    initLiveFeed();
    return;
  }
  refreshLiveFeed();
}

function renderIcons() {
  document.querySelectorAll("[data-icon]").forEach((slot) => {
    const icon = ICONS[slot.dataset.icon];
    if (icon) slot.innerHTML = icon;
  });
}

function renderNavIcons() {
  renderIcons();
}

function attachHandlers() {
  document.querySelectorAll("[data-service]").forEach((button) => {
    button.addEventListener("click", () => setService(button.dataset.service));
  });

  document.querySelectorAll("[data-screen-target]").forEach((button) => {
    button.addEventListener("click", () => setScreen(button.dataset.screenTarget));
  });

  document.querySelectorAll("[data-jump]").forEach((button) => {
    button.addEventListener("click", () => setScreen(button.dataset.jump));
  });

  els.forecastService.addEventListener("change", async () => {
    app.service = els.forecastService.value;
    document.querySelectorAll("[data-service]").forEach((button) => {
      button.classList.toggle("is-active", button.dataset.service === app.service);
    });
    await loadServiceData(app.service);
    setScreen("forecast");
  });

  els.forecastOrigin.addEventListener("change", async () => {
    app.origin = els.forecastOrigin.value;
    await loadOrigin(app.origin);
    app.destination = pickBestDestination(app.byOrigin) || app.destination;
    populateForecastControls();
    renderForecast();
    renderHome();
    renderStationsDetail();
  });

  els.forecastDestination.addEventListener("change", () => {
    app.destination = els.forecastDestination.value;
    renderForecast();
    renderHome();
    renderStationsDetail();
  });

  els.swapRoute.addEventListener("click", async () => {
    const origin = app.destination;
    const destination = app.origin;
    if (!origin) return;
    await loadOrigin(origin);
    app.origin = origin;
    app.destination = destination && app.byOrigin?.destinations?.[destination] ? destination : pickBestDestination(app.byOrigin);
    populateForecastControls();
    renderForecast();
    renderHome();
    renderStationsDetail();
  });

  document.querySelectorAll("[data-forecast-mode]").forEach((button) => {
    button.addEventListener("click", () => selectForecastMode(button.dataset.forecastMode));
  });

  document.querySelectorAll("[data-level]").forEach((button) => {
    button.addEventListener("click", () => selectLevel(button.dataset.level));
  });

  els.stationSearch.addEventListener("input", () => renderStations());

  els.reportForm.addEventListener("submit", (event) => {
    event.preventDefault();
    const station = els.reportStation.value.trim();
    const level = els.reportLevel.value;
    const notes = els.reportNotes.value.trim();
    if (!station) return;

    const entry = {
      station,
      level,
      notes,
      time: new Date().toLocaleString(),
    };

    app.reports = [entry, ...app.reports];
    saveReports(app.reports);
    renderReports();
    els.reportForm.reset();
    els.reportStation.value = station;
    selectLevel("Low");
  });
}

function renderHeroArt() {
  els.heroArt.innerHTML = `
    <svg viewBox="0 0 260 220" width="100%" height="100%" aria-hidden="true">
      <defs>
        <linearGradient id="heroRail" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="rgba(15,138,141,0.95)"/>
          <stop offset="100%" stop-color="rgba(15,138,141,0.22)"/>
        </linearGradient>
      </defs>
      <rect x="18" y="18" width="224" height="184" rx="28" fill="rgba(255,255,255,0.68)" stroke="rgba(15,138,141,0.12)"/>
      <path d="M54 160 C82 138, 108 120, 136 112 S176 88, 208 56" stroke="url(#heroRail)" stroke-width="7" fill="none" stroke-linecap="round"/>
      <path d="M42 74 C74 62, 104 60, 132 70 S176 102, 216 126" stroke="rgba(15,138,141,0.2)" stroke-width="4" fill="none" stroke-linecap="round" stroke-dasharray="8 9"/>
      <circle cx="54" cy="160" r="9" fill="white" stroke="rgba(15,138,141,0.9)" stroke-width="3"/>
      <circle cx="92" cy="138" r="6" fill="white" stroke="rgba(15,138,141,0.76)" stroke-width="2.5"/>
      <circle cx="130" cy="116" r="8" fill="white" stroke="rgba(15,138,141,0.9)" stroke-width="3"/>
      <circle cx="170" cy="90" r="6" fill="white" stroke="rgba(15,138,141,0.76)" stroke-width="2.5"/>
      <circle cx="208" cy="56" r="9" fill="white" stroke="rgba(15,138,141,0.9)" stroke-width="3"/>
      <circle cx="84" cy="70" r="5" fill="rgba(17,167,166,0.85)"/>
      <circle cx="162" cy="66" r="5" fill="rgba(17,167,166,0.55)"/>
    </svg>
  `;
}

async function bootstrap() {
  els.statusTime = qs("statusTime");
  els.screenTitle = qs("screenTitle");
  els.screenSubtitle = qs("screenSubtitle");
  els.homeLatestDate = qs("homeLatestDate");
  els.homeLiveCount = qs("homeLiveCount");
  els.homeLiveNote = qs("homeLiveNote");
  els.homeBestWindow = qs("homeBestWindow");
  els.homeRouteLabel = qs("homeRouteLabel");
  els.homeStationStrip = qs("homeStationStrip");
  els.homeServiceBadge = qs("homeServiceBadge");
  els.heroArt = qs("heroArt");
  els.forecastService = qs("forecastService");
  els.forecastOrigin = qs("forecastOrigin");
  els.forecastDestination = qs("forecastDestination");
  els.swapRoute = qs("swapRoute");
  els.forecastRoute = qs("forecastRoute");
  els.forecastMeta = qs("forecastMeta");
  els.forecastHeatmap = qs("forecastHeatmap");
  els.forecastBest = qs("forecastBest");
  els.forecastAvoid = qs("forecastAvoid");
  els.forecastNote = qs("forecastNote");
  els.liveMapArt = qs("liveMapArt");
  els.liveCount = qs("liveCount");
  els.liveCoverage = qs("liveCoverage");
  els.liveUpdated = qs("liveUpdated");
  els.liveRefreshBadge = qs("liveRefreshBadge");
  els.liveTrainList = qs("liveTrainList");
  els.stationSearch = qs("stationSearch");
  els.stationGroups = qs("stationGroups");
  els.stationCountBadge = qs("stationCountBadge");
  els.stationDetail = qs("stationDetail");
  els.reportForm = qs("reportForm");
  els.reportStation = qs("reportStation");
  els.reportLevel = qs("reportLevel");
  els.reportNotes = qs("reportNotes");
  els.reportList = qs("reportList");
  els.reportCountBadge = qs("reportCountBadge");
  els.guideList = qs("guideList");
  els.moreMeta = qs("moreMeta");

  renderIcons();
  renderNavIcons();
  renderHeroArt();
  attachHandlers();
  selectLevel("Low");
  selectForecastMode("latest");
  setScreen("home");
  setInterval(() => {
    if (els.statusTime) {
      els.statusTime.textContent = new Intl.DateTimeFormat("en-MY", {
        hour: "numeric",
        minute: "2-digit",
      }).format(new Date());
    }
  }, 60000);

  try {
    await loadServiceData(app.service);
  } catch (error) {
    console.error(error);
    renderFallbackState();
  }
}

bootstrap();
