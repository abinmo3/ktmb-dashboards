// Real coordinates mapped to canvas (Peninsular Malaysia: lat 1.3-6.7°N, lon 100.2-103.8°E)
  const stations = [
    {x: 13.0, y:  4.0, n:'Padang Besar', hub:true, color:'#6366F1'},
    {x: 18.4, y: 19.7, n:'Alor Setar'},
    {x: 28.2, y: 44.7, n:'Bukit Mertajam'},
    {x: 18.4, y: 43.5, n:'Butterworth', hub:true, color:'#F59E0B'},
    {x: 80.2, y: 64.3, n:'Kuala Kangsar'},
    {x: 57.4, y: 61.7, n:'Taiping'},
    {x: 95.3, y: 70.1, n:'Ipoh', hub:true},
    {x:135.4, y:122.2, n:'Klang'},
    {x:150.6, y:120.6, n:'Subang Jaya'},
    {x:149.5, y:116.3, n:'Sungai Buloh'},
    {x:160.6, y:115.4, n:'Batu Caves'},
    {x:149.5, y:112.7, n:'Rawang'},
    {x:162.3, y:118.1, n:'Kuala Lumpur'},
    {x:161.0, y:118.9, n:'KL Sentral', hub:true, color:'#0D9488'},
    {x:163.6, y:120.8, n:'BTS'},
    {x:172.3, y:123.6, n:'Kajang'},
    {x:188.5, y:132.5, n:'Seremban', hub:true},
    {x:261.1, y:137.2, n:'Gemas', hub:true, color:'#6366F1'},
    {x:283.8, y:139.7, n:'Segamat'},
    {x:338.0, y:155.7, n:'Kluang'},
    {x:381.3, y:172.2, n:'Kempas Baru'},
    {x:386.0, y:174.6, n:'JB Sentral', hub:true, color:'#6366F1'},
    {x:232.9, y:107.2, n:'Mentakab'},
    {x:235.1, y: 92.0, n:'Jerantut'},
    {x:200.9, y: 83.9, n:'Kuala Lipis', hub:true},
    {x:197.2, y: 44.0, n:'Dabong'},
    {x:191.7, y: 60.7, n:'Gua Musang', hub:true},
    {x:216.7, y: 39.0, n:'Krai'},
    {x:216.7, y: 19.3, n:'Wakaf Bharu'},
    {x:213.4, y: 16.8, n:'Tumpat'},
  ];

  // Edges between connected stations
  const edges = [
    [0,1],[1,2],[2,3],[3,4],[4,5],[5,6],         // Utara
    [7,8],[8,9],[9,10],[10,11],[11,12],[12,13],    // Komuter West
    [13,14],[14,15],[15,16],[16,17],                // Komuter South
    [13,6],[13,16],[6,3],[6,0],                     // ETS
    [17,18],[17,22],[22,23],[23,24],[24,25],        // Jungle Line
    [25,26],[26,27],[27,28],[28,29],                 // Jungle Line cont.
    [18,19],[19,20],[20,21],                         // Southern
  ];

  // Peninsula outline (simplified)
  const outline = [
    [6.65,100.20],[6.55,100.25],[6.30,100.30],[5.90,100.22],[5.40,100.30],
    [5.10,100.40],[4.60,100.45],[4.30,100.65],[3.80,101.00],[3.50,101.30],
    [3.00,101.20],[2.50,101.80],[2.20,102.10],[1.80,102.50],[1.40,103.50],
    [1.50,103.80],[2.20,103.70],[2.80,103.40],[3.50,103.30],[4.20,103.00],
    [4.90,102.50],[5.30,102.10],[5.90,102.10],[6.20,102.15],[6.50,102.00],
    [6.65,101.80],[6.65,100.20]
  ];

  const toX = lon => (lon - 100.2) / 3.6 * w;
  const toY = lat => (6.7 - lat) / 5.4 * h;
  const pathD = outline.map(([lat,lon], i) =>
    (i === 0 ? 'M' : 'L') + toX(lon).toFixed(1) + ',' + toY(lat).toFixed(1)
  ).join(' ') + ' Z';

  canvas.innerHTML = '<svg style="position:absolute;inset:0;width:100%;height:100%" viewBox="0 0 '+w+' '+h+'"><path d="'+pathD+'" fill="rgba(13,148,136,0.06)" stroke="rgba(13,148,136,0.15)" stroke-width="1"/></svg>'
    + edges.map(([a,b]) => {
    const ax = stations[a].x, ay = stations[a].y;
    const bx = stations[b].x, by = stations[b].y;
    const dx = bx-ax, dy = by-ay;
    const len = Math.sqrt(dx*dx+dy*dy);
    const angle = Math.atan2(dy,dx)*180/Math.PI;
    const col = stations[a].color || '#14B8A6';
    return '<div class="network-edge" style="left:'+ax+'px;top:'+ay+'px;width:'+len+'px;transform:rotate('+angle+'deg);background:'+col+'"></div>';
  }).join('') + stations.map(n => {
    const size = n.hub ? 12 : 6;
    const pulse = n.hub ? ' animation: pulse 2s infinite;' : '';
    const col = n.color || '#14B8A6';
    return '<div class="'+(n.hub?'network-node hub':'network-node')+'" style="left:'+n.x+'px;top:'+n.y+'px;background:'+col+';width:'+size+'px;height:'+size+'px'+pulse+'" title="'+n.n+'"></div>';
  }).join('');
}
setTimeout(drawNetwork, 100);