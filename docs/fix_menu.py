with open('ktmb-pulse-upgraded.html','r') as f: html = f.read()

# 1. Service switch
old_sw = '  <div class="service-switch">\n    <button class="active" data-service="all">All Network</button>\n    <button data-service="selatan">Komuter</button>\n    <button data-service="utara">Utara</button>\n  </div>'
new_sw = '''  <div class="service-switch">
    <button class="active" data-service="all">All</button>
    <button data-service="komuter">Komuter</button>
    <button data-service="utara">Utara</button>
    <button data-service="ets">ETS</button>
    <button data-service="intercity">Intercity</button>
    <button data-service="shuttle">Shuttle</button>
    <button data-service="brt">BRT</button>
  </div>'''
html = html.replace(old_sw, new_sw)

# 2. CSS - make service switch scrollable
html = html.replace(
    '.service-switch{\n  display:flex;margin:0 16px 10px;background:var(--bg3);border-radius:10px;padding:2px;flex-shrink:0;\n}',
    '.service-switch{display:flex;margin:0 10px 10px;background:var(--bg3);border-radius:10px;padding:2px;flex-shrink:0;overflow-x:auto;gap:1px}'
)
html = html.replace(
    '.service-switch button{\n  flex:1;padding:7px 10px;border:none;border-radius:9px;\n  font-size:12px;font-weight:600;cursor:pointer;transition:all 0.25s;\n  background:transparent;color:var(--text2);font-family:inherit;\n}',
    '.service-switch button{flex:0 0 auto;padding:5px 10px;border:none;border-radius:9px;font-size:10px;font-weight:600;cursor:pointer;transition:all 0.25s;background:transparent;color:var(--text2);font-family:inherit;white-space:nowrap}'
)

# 3. Origin/dest dropdowns — expand to all stations
stations = ['KL Sentral','Seremban','Ipoh','Butterworth','Kajang','Subang Jaya','Shah Alam','BTS','Kuala Lumpur','Klang','Sungai Buloh','Rawang','Batu Caves','Midvalley','Bank Negara','Abdullah Hukum','Alor Setar','Padang Besar','Taiping','Gemas','JB Sentral','Segamat','Kuala Lipis','Gua Musang','Tumpat','Dabong','Woodlands CIQ','Sunway-Setia Jaya','USJ7']
opts = ''.join(['<option>'+s+'</option>' for s in stations])

import re
# Replace origin select
fo_m = re.search(r'<select class="field-inp" id="fo" onchange="updF\(\)">.*?</select>', html, re.DOTALL)
if fo_m:
    html = html.replace(fo_m.group(0), '<select class="field-inp" id="fo" onchange="updF()">'+opts+'</select>')

fd_m = re.search(r'<select class="field-inp" id="fd" onchange="updF\(\)">.*?</select>', html, re.DOTALL)
if fd_m:
    html = html.replace(fd_m.group(0), '<select class="field-inp" id="fd" onchange="updF()">'+opts+'</select>')

# 4. Update filter JS for map — handle all 6 services
old_af = "if(AF==='selatan'){vS=[];S.forEach(function(n){if(n.v==='komuter'||n.v==='ets')vS.push(n)})}\n  if(AF==='utara'){vS=[];S.forEach(function(n){if(n.v==='utara'||n.v==='ets')vS.push(n)})}"
new_af = "if(AF==='komuter'){vS=[];S.forEach(function(n){if(n.v==='komuter')vS.push(n)})}\n  if(AF==='utara'){vS=[];S.forEach(function(n){if(n.v==='utara')vS.push(n)})}\n  if(AF==='ets'){vS=[];S.forEach(function(n){if(n.v==='ets')vS.push(n)})}\n  if(AF==='intercity'){vS=[];S.forEach(function(n){if(n.v==='intercity')vS.push(n)})}\n  if(AF==='shuttle'){vS=[];S.forEach(function(n){if(n.v==='shuttle')vS.push(n)})}\n  if(AF==='brt'){vS=[];S.forEach(function(n){if(n.v==='brt')vS.push(n)})}"
html = html.replace(old_af, new_af)

# 5. Update filterStations for new service names
old_fs = "(AF==='all'||(AF==='selatan'&&s.v.indexOf('Komuter')>=0)||(AF==='utara'&&s.v.indexOf('Utara')>=0))"
new_fs = "(AF==='all'||(AF==='komuter'&&s.v==='komuter')||(AF==='utara'&&s.v==='utara')||(AF==='ets'&&s.v==='ets')||(AF==='intercity'&&s.v==='intercity')||(AF==='shuttle'&&s.v==='shuttle')||(AF==='brt'&&s.v==='brt'))"
html = html.replace(old_fs, new_fs)

# 6. Add shuttle+brt colors
old_cc = "var CC={komuter:'#0D9488',utara:'#F59E0B',ets:'#6366F1',intercity:'#8B5CF6'};"
new_cc = "var CC={komuter:'#0D9488',utara:'#F59E0B',ets:'#6366F1',intercity:'#8B5CF6',shuttle:'#EC4899',brt:'#06B6D4'};"
html = html.replace(old_cc, new_cc)

# 7. Update station data service codes
for old, new in [("v:'Komuter/ETS'","v:'komuter'"),("v:'Komuter/ETS/Intercity'","v:'komuter'"),("v:'ETS/Utara'","v:'ets'"),("v:'ETS/Intercity'","v:'intercity'"),("v:'ETS/Intercity/Shuttle Tebrau'","v:'shuttle'"),("v:'Shuttle Tebrau'","v:'shuttle'"),("v:'BRT Sunway'","v:'brt'"),("v:'Komuter'","v:'komuter'"),("v:'Intercity'","v:'intercity'")]:
    html = html.replace(old, new)

with open('ktmb-pulse-upgraded.html','w') as f: f.write(html)
print('Done! Lines:', len(html.split('\n')))
