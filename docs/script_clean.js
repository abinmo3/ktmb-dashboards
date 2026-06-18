var activeFilter = 'all';

function switchScreen(name) {
  document.querySelectorAll('.screen').forEach(function(s){ s.classList.remove('active'); });
  var target = document.querySelector('[data-screen="'+name+'"]');
  if(target) target.classList.add('active');
  document.querySelectorAll('.nav-item').forEach(function(n){ n.classList.toggle('active', n.dataset.screen === name); });
  var titles = {network:'Network overview',forecast:'Crowd forecast',live:'Live feed',stations:'Station directory',info:'Data & info'};
  document.getElementById('appSubtitle').textContent = titles[name] || '';
  if(name === 'network' && typeof drawNetwork === 'function') setTimeout(drawNetwork, 50);
}

document.querySelectorAll('.service-switch button').forEach(function(btn){
  btn.addEventListener('click', function(){
    document.querySelectorAll('.service-switch button').forEach(function(b){ b.classList.remove('active'); });
    this.classList.add('active');
    activeFilter = this.dataset.service;
    if(typeof drawNetwork === 'function') drawNetwork();
    if(typeof filterStations === 'function') filterStations();
  });
});

var forecastData = {
  latest: [0.2,0.1,0.1,0.1,0.2,1.0,3.2,6.8,9.5,8.2,7.1,6.5,5.8,6.2,6.0,5.5,7.8,10.5,12.3,11.2,8.0,4.5,2.1,0.8],
  typical:[0.1,0.1,0.1,0.1,0.3,1.2,3.5,7.0,9.8,8.0,6.8,6.2,5.5,6.0,5.8,5.2,7.5,10.0,12.0,10.8,7.5,4.2,2.0,0.7]
};
var currentMode = 'latest';

function renderHeatmap() {
  var data = forecastData[currentMode], max = Math.max.apply(null,data);
  document.getElementById('heatmap').innerHTML = data.map(function(v,i){
    var h = Math.max(6,(v/max)*72), level = v/max<0.25?'low':v/max<0.5?'mid':v/max<0.75?'busy':'packed';
    var colors = {low:'#10B981',mid:'#F59E0B',busy:'#F97316',packed:'#E11D48'};
    return '<div class="bar" style="height:'+h+'px;background:'+colors[level]+';opacity:'+(0.5+(v/max)*0.5)+'"></div>';
  }).join('');
  var pairs = data.map(function(v,i){ return {v:v,i:i}; }).filter(function(p){ return p.v>0; }).sort(function(a,b){ return a.v-b.v; });
  document.getElementById('bestWindows').textContent = pairs.slice(0,3).map(function(p){ return ('0'+p.i).slice(-2)+':00'; }).join(', ');
  document.getElementById('avoidWindows').textContent = pairs.slice(-3).reverse().map(function(p){ return ('0'+p.i).slice(-2)+':00'; }).join(', ');
}
function setMode(m){ currentMode=m; document.querySelectorAll('[data-mode]').forEach(function(c){ c.classList.toggle('active',c.dataset.mode===m); }); renderHeatmap(); }
function updateForecast(){ var o=document.getElementById('fcOrigin').value,d=document.getElementById('fcDest').value; document.getElementById('fcRouteLabel').textContent=o+' → '+d; renderHeatmap(); }
function swapRoute(){ var o=document.getElementById('fcOrigin'),d=document.getElementById('fcDest'),t=o.value; o.value=d.value; d.value=t; updateForecast(); }

var stationData = [
  {name:'KL Sentral',state:'Kuala Lumpur',service:'Komuter/ETS'},
  {name:'Kuala Lumpur',state:'Kuala Lumpur',service:'Komuter/ETS'},
  {name:'Bank Negara',state:'Kuala Lumpur',service:'Komuter'},
  {name:'Midvalley',state:'Kuala Lumpur',service:'Komuter'},
  {name:'Abdullah Hukum',state:'Kuala Lumpur',service:'Komuter'},
  {name:'Seremban',state:'Negeri Sembilan',service:'Komuter/ETS/Intercity'},
  {name:'Kajang',state:'Selangor',service:'Komuter/ETS'},
  {name:'Subang Jaya',state:'Selangor',service:'Komuter'},
  {name:'Shah Alam',state:'Selangor',service:'Komuter'},
  {name:'Bandar Tasek Selatan',state:'Kuala Lumpur',service:'Komuter/ETS'},
  {name:'Ipoh',state:'Perak',service:'ETS/Utara'},
  {name:'Butterworth',state:'Pulau Pinang',service:'ETS/Utara'},
  {name:'Alor Setar',state:'Kedah',service:'ETS/Utara'},
  {name:'Padang Besar',state:'Perlis',service:'ETS/Utara'},
  {name:'Gemas',state:'Negeri Sembilan',service:'ETS/Intercity'},
  {name:'JB Sentral',state:'Johor',service:'ETS/Intercity/Shuttle Tebrau'},
  {name:'Gua Musang',state:'Kelantan',service:'Intercity'},
  {name:'Kuala Lipis',state:'Pahang',service:'Intercity'},
  {name:'Dabong',state:'Kelantan',service:'Intercity'},
  {name:'Tumpat',state:'Kelantan',service:'Intercity'},
  {name:'Sungai Buloh',state:'Selangor',service:'Komuter/ETS'},
  {name:'Klang',state:'Selangor',service:'Komuter'},
  {name:'Rawang',state:'Selangor',service:'Komuter/ETS'},
  {name:'Taiping',state:'Perak',service:'ETS/Utara'},
  {name:'Segamat',state:'Johor',service:'ETS/Intercity'},
  {name:'Woodlands CIQ',state:'Singapore',service:'Shuttle Tebrau'},
  {name:'Sunway-Setia Jaya',state:'Selangor',service:'BRT Sunway'},
  {name:'Sunway Lagoon',state:'Selangor',service:'BRT Sunway'},
  {name:'SunMed',state:'Selangor',service:'BRT Sunway'},
  {name:'SunU-Monash',state:'Selangor',service:'BRT Sunway'},
  {name:'South Quay-USJ1',state:'Selangor',service:'BRT Sunway'},
  {name:'USJ7',state:'Selangor',service:'BRT Sunway'},
];
stationData.sort(function(a,b){ return a.name.localeCompare(b.name); });

function filterStations(){
  var q = document.getElementById('stationSearch').value.toLowerCase();
  var groups = {};
  var filtered = stationData.filter(function(s){
    var mq = !q || s.name.toLowerCase().indexOf(q)>=0 || s.state.toLowerCase().indexOf(q)>=0;
    var ms = activeFilter==='all' || (activeFilter==='selatan' && s.service.indexOf('Komuter')>=0) || (activeFilter==='utara' && s.service.indexOf('Utara')>=0);
    return mq && ms;
  });
  filtered.forEach(function(s){
    if(!groups[s.state]) groups[s.state]=[];
    groups[s.state].push(s);
  });
  var order = ['Kuala Lumpur','Selangor','Negeri Sembilan','Perak','Kedah','Perlis','Pulau Pinang','Kelantan','Pahang','Johor','Singapore'];
  var html = '';
  order.forEach(function(state){
    if(!groups[state]) return;
    html += '<div class="station-group-header">'+state+'</div>';
    groups[state].forEach(function(s){
      html += '<div class="station-item" onclick="showStationDetail(\''+s.name+'\',\''+s.state+'\',\''+s.service.replace(/'/g,"&#39;")+'\')"><span><span class="name">'+s.name+'</span><span class="state-tag">'+s.service+'</span></span><span style="color:var(--text-tertiary)">→</span></div>';
    });
  });
  document.getElementById('stationGroups').innerHTML = html;
  document.getElementById('stationCount').textContent = filtered.length+' of '+stationData.length+' stations';
}

function showStationDetail(name, state, service){
  var detail = document.getElementById('stationDetail');
  if(!detail){
    detail = document.createElement('div');
    detail.id = 'stationDetail';
    detail.style.marginTop = '8px';
    var parent = document.getElementById('stationGroups');
    parent.parentNode.insertBefore(detail, parent);
  }
  detail.innerHTML = '<div class="card" style="background:linear-gradient(135deg,rgba(13,148,136,0.08),rgba(13,148,136,0.02));border-color:rgba(13,148,136,0.2)"><div style="display:flex;justify-content:space-between;align-items:flex-start"><div><div style="font-size:17px;font-weight:700">'+name+'</div><div style="font-size:12px;color:var(--text-tertiary);margin-top:2px">'+state+'</div><div style="margin-top:4px"><span class="badge">'+service+'</span></div></div><button onclick="document.getElementById(\'stationDetail\').innerHTML=\'\'" style="background:none;border:none;font-size:18px;cursor:pointer;color:var(--text-tertiary)">&times;</button></div><div style="display:flex;gap:8px;margin-top:12px"><button class="btn-primary" style="flex:1;font-size:12px;padding:8px" onclick="switchScreen(\'forecast\');document.getElementById(\'fcOrigin\').value=\''+name+'\';updateForecast()">Use as origin</button><button class="btn-outline" style="flex:1;font-size:12px;padding:8px" onclick="switchScreen(\'forecast\');document.getElementById(\'fcDest\').value=\''+name+'\';updateForecast()">Use as destination</button></div></div>';
  detail.scrollIntoView({behavior:'smooth'});
}

function drawNetwork() {
  var canvas = document.getElementById('networkMap');
  if(!canvas) return;
  var w = canvas.offsetWidth, h = canvas.offsetHeight;
  var S = [
    {x:18,y:12,n:'Padang Besar',hub:true,svc:'ets'},{x:25,y:34,n:'Alor Setar',svc:'ets'},{x:36,y:66,n:'Bukit Mertajam',svc:'utara'},
    {x:25,y:64,n:'Butterworth',hub:true,svc:'utara'},{x:96,y:88,n:'Kuala Kangsar',svc:'utara'},{x:70,y:86,n:'Taiping',svc:'utara'},
    {x:113,y:96,n:'Ipoh',hub:true,svc:'ets'},{x:155,y:168,n:'Klang',svc:'komuter'},{x:173,y:165,n:'Subang Jaya',svc:'komuter'},
    {x:171,y:159,n:'Sungai Buloh',svc:'komuter'},{x:185,y:157,n:'Batu Caves',svc:'komuter'},{x:171,y:153,n:'Rawang',svc:'komuter'},
    {x:187,y:162,n:'Kuala Lumpur',svc:'komuter'},{x:185,y:163,n:'KL Sentral',hub:true,svc:'komuter'},{x:188,y:166,n:'BTS',svc:'komuter'},
    {x:198,y:169,n:'Kajang',svc:'komuter'},{x:218,y:182,n:'Seremban',hub:true,svc:'komuter'},{x:302,y:188,n:'Gemas',hub:true,svc:'intercity'},
    {x:328,y:192,n:'Segamat',svc:'intercity'},{x:365,y:213,n:'Kluang',svc:'intercity'},{x:387,y:214,n:'Kempas Baru',svc:'intercity'},
    {x:391,y:216,n:'JB Sentral',hub:true,svc:'intercity'},{x:268,y:146,n:'Mentakab',svc:'intercity'},{x:270,y:126,n:'Jerantut',svc:'intercity'},
    {x:232,y:115,n:'Kuala Lipis',hub:true,svc:'intercity'},{x:228,y:64,n:'Dabong',svc:'intercity'},{x:221,y:85,n:'Gua Musang',hub:true,svc:'intercity'},
    {x:250,y:58,n:'Krai',svc:'intercity'},{x:250,y:32,n:'Wakaf Bharu',svc:'intercity'},{x:246,y:29,n:'Tumpat',svc:'intercity'},
  ];
  var E = [[0,1],[1,2],[2,3],[3,4],[4,5],[5,6],[7,8],[8,9],[9,10],[10,11],[11,12],[12,13],[13,14],[14,15],[15,16],[16,17],[13,6],[13,17],[6,3],[6,0],[17,18],[17,22],[22,23],[23,24],[24,25],[25,26],[26,27],[27,28],[28,29],[18,19],[19,20],[20,21]];
  var C = {komuter:{s:'#0D9488',g:'rgba(13,148,136,0.25)'},utara:{s:'#F59E0B',g:'rgba(245,158,11,0.25)'},ets:{s:'#6366F1',g:'rgba(99,102,241,0.25)'},intercity:{s:'#8B5CF6',g:'rgba(139,92,246,0.25)'}};
  var MY = 'M19.8,15.5 L35.4,17.2 L50.9,20.8 L62.6,22.5 L66.5,24.2 L60.3,29.5 L65.0,36.5 L70.4,43.5 L70.4,52.2 L62.6,61.0 L70.4,68.0 L86.0,80.3 L93.8,89.0 L99.2,101.2 L101.6,108.2 L113.2,115.2 L124.9,122.2 L132.7,131.0 L136.6,141.5 L152.2,150.2 L163.9,157.3 L171.6,164.2 L181.8,171.2 L187.2,177.2 L210.6,185.2 L233.9,194.0 L284.6,201.0 L319.6,204.5 L339.1,206.2 L343.0,202.8 L358.5,195.8 L362.4,187.0 L346.9,174.8 L331.3,162.5 L315.7,152.0 L304.0,138.0 L300.1,125.8 L296.2,113.5 L288.4,101.2 L284.6,90.8 L276.8,80.3 L272.9,68.0 L265.1,57.5 L257.3,47.0 L249.5,36.5 L233.9,27.8 L210.6,20.8 L187.2,15.5 L160.0,13.7 L121.0,14.8 L82.1,15.5 L19.8,15.5 Z';

  // Filter by active service
  var vs = S, ve = E;
  if(activeFilter === 'selatan'){ vs = []; S.forEach(function(n){ if(n.svc==='komuter'||n.svc==='ets') vs.push(n); }); }
  if(activeFilter === 'utara'){ vs = []; S.forEach(function(n){ if(n.svc==='utara'||n.svc==='ets') vs.push(n); }); }
  var nameSet = {}; vs.forEach(function(n){ nameSet[n.n]=true; });
  ve = E.filter(function(e){ return nameSet[S[e[0]].n] && nameSet[S[e[1]].n]; });

  var svg = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 '+w+' '+h+'" style="width:100%;height:100%;font-family:Inter,sans-serif">';
  svg += '<defs>';
  for(var k in C){ svg += '<radialGradient id="gd-'+k+'"><stop offset="0%" stop-color="'+C[k].s+'" stop-opacity="1"/><stop offset="100%" stop-color="'+C[k].s+'" stop-opacity="0.6"/></radialGradient>'; }
  svg += '<filter id="glow"><feGaussianBlur stdDeviation="1.5"/><feMerge><feMergeNode/><feMergeNode in="SourceGraphic"/></feMerge></filter>';
  svg += '<filter id="glowH"><feGaussianBlur stdDeviation="2.5"/><feMerge><feMergeNode/><feMergeNode in="SourceGraphic"/></feMerge></filter></defs>';
  for(var i=0;i<w;i+=40){ svg += '<line x1="'+i+'" y1="0" x2="'+i+'" y2="'+h+'" stroke="rgba(0,0,0,0.03)" stroke-width="0.5"/>'; }
  for(var j=0;j<h;j+=40){ svg += '<line x1="0" y1="'+j+'" x2="'+w+'" y2="'+j+'" stroke="rgba(0,0,0,0.03)" stroke-width="0.5"/>'; }
  svg += '<path d="'+MY+'" fill="rgba(13,148,136,0.04)" stroke="rgba(13,148,136,0.1)" stroke-width="1" transform="scale(0.90) translate(22,12)"/>';

  ve.forEach(function(e){
    var a=e[0],b=e[1],ax=S[a].x,ay=S[a].y,bx=S[b].x,by=S[b].y,mx=(ax+bx)/2,my=(ay+by)/2-8;
    svg += '<path d="M'+ax+','+ay+' Q'+mx+','+my+' '+bx+','+by+'" fill="none" stroke="'+C[S[a].svc].s+'" stroke-width="1" opacity="0.35" stroke-linecap="round"/>';
  });

  vs.filter(function(n){ return n.hub; }).forEach(function(n){
    svg += '<circle cx="'+n.x+'" cy="'+n.y+'" r="10" fill="'+C[n.svc].g+'" filter="url(#glowH)"><animate attributeName="r" values="10;15;10" dur="2.5s" repeatCount="indefinite"/><animate attributeName="opacity" values="0.6;0.15;0.6" dur="2.5s" repeatCount="indefinite"/></circle>';
  });

  vs.forEach(function(n){
    svg += '<circle cx="'+n.x+'" cy="'+n.y+'" r="'+(n.hub?5.5:3)+'" fill="url(#gd-'+n.svc+')" filter="url(#glow)" stroke="white" stroke-width="0.8"/>';
  });

  vs.filter(function(n){ return n.hub; }).forEach(function(n){
    svg += '<rect x="'+(n.x-n.n.length*3.5-4)+'" y="'+(n.y-17)+'" width="'+(n.n.length*7+8)+'" height="14" rx="4" fill="white" fill-opacity="0.9" stroke="'+C[n.svc].s+'" stroke-width="0.5"/>';
    svg += '<text x="'+n.x+'" y="'+(n.y-7)+'" text-anchor="middle" font-size="7.5" font-weight="700" fill="'+C[n.svc].s+'">'+n.n+'</text>';
  });

  var lx=8,ly=h-46;
  svg += '<rect x="'+lx+'" y="'+ly+'" width="170" height="38" rx="6" fill="white" fill-opacity="0.85" stroke="rgba(0,0,0,0.06)" stroke-width="0.5"/>';
  var li=0;
  for(var k in C){ var cl=C[k]; svg+='<circle cx="'+(lx+8+li*42)+'" cy="'+(ly+8)+'" r="4" fill="'+cl.s+'"/>'; svg+='<text x="'+(lx+15+li*42)+'" y="'+(ly+12)+'" font-size="8" fill="#475569">'+k.charAt(0).toUpperCase()+k.slice(1)+'</text>'; li++; }

  svg += '</svg>';
  canvas.innerHTML = svg;
}
setTimeout(function(){ drawNetwork(); }, 100);

function refreshLive(){
  var count = Math.floor(Math.random()*20)+3;
  var covs = ['Low activity','Moderate activity','High activity','Very high activity'];
  document.getElementById('liveCount').textContent = count;
  document.getElementById('liveCoverage').textContent = count<6?covs[0]:count<14?covs[1]:count<24?covs[2]:covs[3];
  document.getElementById('liveUpdated').textContent = new Date().toLocaleTimeString();
  document.getElementById('liveRefreshBadge').textContent = 'Just now';
  var routes = ['Klang Valley','Seremban Line','Port Klang Line','ETS North','ETS South','Jungle Line','Shuttle Tebrau','BRT Sunway'];
  var html = '';
  for(var i=0;i<Math.min(count,8);i++){
    html += '<div class="station-item" style="cursor:default"><span><strong>VH-'+('00'+(i+1)).slice(-3)+'</strong><br><span style="font-size:10px;color:var(--text-tertiary)">'+routes[i%routes.length]+'</span></span><span style="text-align:right"><strong style="color:var(--brand-teal)">'+Math.floor(Math.random()*80+10)+' km/h</strong><br><span style="font-size:10px;color:var(--text-tertiary)">'+Math.floor(Math.random()*360)+'&deg;</span></span></div>';
  }
  document.getElementById('trainList').innerHTML = html;
}
refreshLive(); setInterval(refreshLive, 30000);
renderHeatmap(); filterStations();
document.getElementById('statusTime').textContent = new Date().toLocaleTimeString([],{hour:'2-digit',minute:'2-digit'});
