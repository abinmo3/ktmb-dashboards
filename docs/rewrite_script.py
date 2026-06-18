import re
with open('/c/Users/abinm/OneDrive/Documents/01_Active_Work/Business_Projects/ktmb_dashboards/docs/ktmb-pulse-upgraded.html','r') as f:
    html = f.read()

script_start = html.index('<script>')
script_end = html.index('</script>') + len('</script>')
before = html[:script_start]
after = html[script_end:]

# Read the clean script from this file's companion
with open('/c/Users/abinm/OneDrive/Documents/01_Active_Work/Business_Projects/ktmb_dashboards/docs/script_clean.js','r') as f:
    new_script = f.read()

html = before + '<script>\n' + new_script + '\n</script>' + after
with open('/c/Users/abinm/OneDrive/Documents/01_Active_Work/Business_Projects/ktmb_dashboards/docs/ktmb-pulse-upgraded.html','w') as f:
    f.write(html)
print('Updated! Lines:', len(html.split('\n')))
