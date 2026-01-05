const DATA_ROOT = "./data";

async function fetchJson(path) {
  const res = await fetch(path, { cache: "no-store" });
  if (!res.ok) {
    throw new Error(`Fetch failed: ${path} (${res.status})`);
  }
  return res.json();
}

async function loadStateMap() {
  try {
    return await fetchJson(`${DATA_ROOT}/state_map.json`);
  } catch (error) {
    console.warn("State map unavailable", error);
    return {};
  }
}

function normalize(text) {
  return text ? text.toLowerCase().trim() : "";
}

function buildStationGroups(stations, stateMap) {
  const grouped = new Map();
  stations.forEach((station) => {
    const name = station.name;
    const state = stateMap[name] || "Other";
    if (!grouped.has(state)) grouped.set(state, []);
    grouped.get(state).push({ name, state });
  });
  return grouped;
}

function renderStationList(listEl, stations, stateMap, query) {
  listEl.innerHTML = "";
  const filtered = stations.filter((station) => {
    if (!query) return true;
    return (
      normalize(station.name).includes(query) ||
      normalize(stateMap[station.name] || "other").includes(query)
    );
  });

  const grouped = buildStationGroups(filtered, stateMap);
  const states = Array.from(grouped.keys()).sort((a, b) => a.localeCompare(b));

  states.forEach((state) => {
    const section = document.createElement("div");
    section.className = "list-card";

    const heading = document.createElement("strong");
    heading.textContent = state;
    section.appendChild(heading);

    const list = document.createElement("div");
    list.className = "list-grid";

    grouped.get(state)
      .sort((a, b) => a.name.localeCompare(b.name))
      .forEach((station) => {
        const card = document.createElement("div");
        card.className = "list-card";

        const name = document.createElement("div");
        name.textContent = station.name;
        card.appendChild(name);

        const link = document.createElement("a");
        link.href = `./station.html?id=${encodeURIComponent(station.name)}`;
        link.textContent = "View station";
        card.appendChild(link);

        list.appendChild(card);
      });

    section.appendChild(list);
    listEl.appendChild(section);
  });
}

async function initStationsDirectory() {
  const listEl = document.getElementById("stationList");
  const searchEl = document.getElementById("stationSearch");
  if (!listEl || !searchEl) return;

  const [stations, stateMap] = await Promise.all([
    fetchJson(`${DATA_ROOT}/stations.json`),
    loadStateMap(),
  ]);

  const render = () => {
    const query = normalize(searchEl.value);
    renderStationList(listEl, stations, stateMap, query);
  };

  searchEl.addEventListener("input", render);
  render();
}

async function initStationDetail() {
  const nameEl = document.getElementById("stationName");
  const stateEl = document.getElementById("stationState");
  const linksEl = document.getElementById("stationLinks");
  if (!nameEl || !stateEl || !linksEl) return;

  const params = new URLSearchParams(window.location.search);
  const id = params.get("id");
  if (!id) {
    nameEl.textContent = "Station not found";
    stateEl.textContent = "Add ?id=Station Name to the URL.";
    return;
  }

  const [stations, stateMap] = await Promise.all([
    fetchJson(`${DATA_ROOT}/stations.json`),
    loadStateMap(),
  ]);

  const station = stations.find((entry) => entry.name.toLowerCase() === id.toLowerCase());
  if (!station) {
    nameEl.textContent = "Station not found";
    stateEl.textContent = "Check the station name in the directory.";
    return;
  }

  const stationState = stateMap[station.name] || "Other";
  nameEl.textContent = station.name;
  stateEl.textContent = stationState;

  linksEl.innerHTML = "";
  const forecastLink = document.createElement("a");
  forecastLink.href = "./reco.html";
  forecastLink.textContent = "Open crowd forecast";
  forecastLink.className = "btn btn-soft";

  const liveLink = document.createElement("a");
  liveLink.href = "./live-map.html";
  liveLink.textContent = "View live map";
  liveLink.className = "btn btn-outline";

  linksEl.appendChild(forecastLink);
  linksEl.appendChild(liveLink);
}

async function initGuidesIndex() {
  const listEl = document.getElementById("guidesList");
  if (!listEl) return;

  const guides = await fetchJson(`${DATA_ROOT}/guides.json`);
  listEl.innerHTML = "";

  guides.forEach((guide) => {
    const card = document.createElement("div");
    card.className = "list-card";

    const title = document.createElement("strong");
    title.textContent = guide.title;
    card.appendChild(title);

    const meta = document.createElement("span");
    meta.className = "muted";
    meta.textContent = `${guide.readTime} · Updated ${guide.updated}`;
    card.appendChild(meta);

    const desc = document.createElement("span");
    desc.textContent = guide.summary;
    card.appendChild(desc);

    const link = document.createElement("a");
    link.href = `./guide.html?slug=${encodeURIComponent(guide.slug)}`;
    link.textContent = "Read guide";
    card.appendChild(link);

    listEl.appendChild(card);
  });
}

async function initGuideDetail() {
  const titleEl = document.getElementById("guideTitle");
  const metaEl = document.getElementById("guideMeta");
  const contentEl = document.getElementById("guideContent");
  if (!titleEl || !metaEl || !contentEl) return;

  const params = new URLSearchParams(window.location.search);
  const slug = params.get("slug");
  if (!slug) {
    titleEl.textContent = "Guide not found";
    metaEl.textContent = "Add ?slug=... to the URL.";
    return;
  }

  const guides = await fetchJson(`${DATA_ROOT}/guides.json`);
  const guide = guides.find((entry) => entry.slug === slug);
  if (!guide) {
    titleEl.textContent = "Guide not found";
    metaEl.textContent = "Return to the guides index.";
    return;
  }

  titleEl.textContent = guide.title;
  metaEl.textContent = `${guide.readTime} · Updated ${guide.updated}`;
  contentEl.innerHTML = "";

  guide.sections.forEach((section) => {
    const block = document.createElement("div");
    block.className = "box";

    const heading = document.createElement("strong");
    heading.textContent = section.heading;
    block.appendChild(heading);

    const body = document.createElement("p");
    body.className = "muted";
    body.textContent = section.body;
    block.appendChild(body);

    contentEl.appendChild(block);
  });
}

function loadReports() {
  try {
    return JSON.parse(localStorage.getItem("ktmbReports") || "[]");
  } catch {
    return [];
  }
}

function saveReports(reports) {
  localStorage.setItem("ktmbReports", JSON.stringify(reports));
}

function renderReports(listEl, reports) {
  listEl.innerHTML = "";
  if (!reports.length) {
    const empty = document.createElement("div");
    empty.className = "muted";
    empty.textContent = "No reports yet. Share the first update for your station.";
    listEl.appendChild(empty);
    return;
  }

  reports.slice(0, 6).forEach((report) => {
    const card = document.createElement("div");
    card.className = "list-card";

    const heading = document.createElement("strong");
    heading.textContent = `${report.station} · ${report.level}`;
    card.appendChild(heading);

    const time = document.createElement("span");
    time.className = "muted";
    time.textContent = report.time;
    card.appendChild(time);

    if (report.notes) {
      const notes = document.createElement("span");
      notes.textContent = report.notes;
      card.appendChild(notes);
    }

    listEl.appendChild(card);
  });
}

function initSubmitForm() {
  const form = document.getElementById("reportForm");
  const listEl = document.getElementById("reportList");
  if (!form || !listEl) return;

  const reports = loadReports();
  renderReports(listEl, reports);

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    const formData = new FormData(form);
    const station = formData.get("station");
    const level = formData.get("level");
    const notes = formData.get("notes");

    const entry = {
      station,
      level,
      notes,
      time: new Date().toLocaleString(),
    };

    const updated = [entry, ...reports];
    saveReports(updated);
    renderReports(listEl, updated);
    form.reset();
  });
}

async function patchRecoStationGrouping() {
  const originEl = document.getElementById("origin");
  const destEl = document.getElementById("destination");
  if (!originEl || !destEl) return;
  if (typeof window.renderStationOptions !== "function") return;

  const stateMap = await loadStateMap();
  if (!Object.keys(stateMap).length) return;

  window.renderStationOptions = function renderStationOptions(selectEl, names, placeholderLabel) {
    selectEl.innerHTML = "";
    if (placeholderLabel) {
      const placeholder = document.createElement("option");
      placeholder.value = "";
      placeholder.textContent = placeholderLabel;
      selectEl.appendChild(placeholder);
    }

    const grouped = new Map();
    names.forEach((name) => {
      const state = stateMap[name] || "Other";
      if (!grouped.has(state)) grouped.set(state, []);
      grouped.get(state).push(name);
    });

    const states = Array.from(grouped.keys()).sort((a, b) => a.localeCompare(b));
    states.forEach((state) => {
      const optgroup = document.createElement("optgroup");
      optgroup.label = state;
      grouped
        .get(state)
        .slice()
        .sort((a, b) => a.localeCompare(b))
        .forEach((name) => {
          const option = document.createElement("option");
          option.value = name;
          option.textContent = `${name} (${state})`;
          optgroup.appendChild(option);
        });
      selectEl.appendChild(optgroup);
    });
  };

  if (typeof window.loadStations === "function") {
    await window.loadStations();
  }
}

initStationsDirectory();
initStationDetail();
initGuidesIndex();
initGuideDetail();
initSubmitForm();
patchRecoStationGrouping();
