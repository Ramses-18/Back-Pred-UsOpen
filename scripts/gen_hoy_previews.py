import asyncio
from playwright.async_api import async_playwright

# Common dark Centre Court palette
BG = '#0a1a0f'
CARD = 'rgba(255,255,255,.06)'
CARD_BORDER = 'rgba(255,255,255,.08)'
TEXT = '#f5f5f5'
TEXT_MID = '#ccc'
TEXT_MUTED = '#666'
GREEN = '#1B5E20'
GREEN_BRIGHT = '#4CAF50'
GREEN_PALE = 'rgba(76,175,80,.1)'
GREEN_BORDER = 'rgba(76,175,80,.25)'
GOLD = '#C8A951'
CREAM = '#F5F0E1'
DANGER = '#ff5252'

# Reusable match card
def match_card(p1="A. Zverev", s1="6  7  6", p2="C. Alcaraz", s2="4  5  2", status="live"):
    dot = '<span style="display:inline-block;width:6px;height:6px;border-radius:50%;background:#ff5252;animation:pulse .8s infinite;margin-right:4px"></span>' if status == 'live' else ''
    badge = f'<span style="font-size:9px;color:#ff5252;font-weight:700;display:flex;align-items:center">{dot}LIVE</span>' if status == 'live' else ''
    bg = 'rgba(76,175,80,.08)' if status == 'live' else CARD
    border = 'rgba(76,175,80,.2)' if status == 'live' else CARD_BORDER
    return f'''<div style="background:{bg};border:1px solid {border};border-radius:14px;padding:14px 16px;margin-bottom:10px">
      {f'<div style="display:flex;justify-content:space-between;align-items:center;padding-bottom:8px;border-bottom:1px solid {border};margin-bottom:8px"><span style="font-size:10px;color:{TEXT_MUTED}">Court 1</span>{badge}</div>' if status == 'live' else f'<div style="display:flex;justify-content:space-between;padding-bottom:8px;border-bottom:1px solid {border};margin-bottom:8px"><span style="font-size:10px;color:{TEXT_MUTED}">Court 1 · 14:00</span><span style="font-size:9px;color:{GREEN_BRIGHT};background:{GREEN_PALE};padding:2px 8px;border-radius:10px">R16</span></div>'}
      <div style="display:flex;justify-content:space-between;align-items:center">
        <div style="display:flex;align-items:center;gap:10px;flex:1">
          <div style="width:32px;height:32px;border-radius:50%;background:{CARD_BORDER};display:flex;align-items:center;justify-content:center;font-size:12px;color:{TEXT_MUTED}">AZ</div>
          <div><div style="font-size:14;font-weight:600;color:{TEXT}">{p1}</div><div style="font-size:11px;color:{TEXT_MUTED};font-family:monospace;letter-spacing:.1em">{s1}</div></div>
        </div>
        <div style="font-size:12px;color:{TEXT_MUTED};margin:0 8px">vs</div>
        <div style="display:flex;align-items:center;gap:10px;flex:1;flex-direction:row-reverse">
          <div style="width:32px;height:32px;border-radius:50%;background:{CARD_BORDER};display:flex;align-items:center;justify-content:center;font-size:12px;color:{TEXT_MUTED}">CA</div>
          <div style="text-align:right"><div style="font-size:14;font-weight:600;color:{TEXT}">{p2}</div><div style="font-size:11px;color:{TEXT_MUTED};font-family:monospace;letter-spacing:.1em">{s2}</div></div>
        </div>
      </div>
    </div>'''

def match_card_pick(p1="D. Medvedev", p2="J. Sinner", time="16:30"):
    return f'''<div style="background:{CARD};border:1px solid {CARD_BORDER};border-radius:14px;padding:14px 16px;margin-bottom:10px">
      <div style="display:flex;justify-content:space-between;padding-bottom:8px;border-bottom:1px solid {CARD_BORDER};margin-bottom:8px">
        <span style="font-size:10px;color:{TEXT_MUTED}">Centre Court · {time}</span>
        <span style="font-size:9px;color:{GREEN_BRIGHT};background:{GREEN_PALE};padding:2px 8px;border-radius:10px">R16</span>
      </div>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <div style="font-size:14;font-weight:500;color:{TEXT_MID}">{p1}</div>
        <div style="font-size:11px;color:{TEXT_MUTED}">{time}</div>
        <div style="font-size:14;font-weight:500;color:{TEXT_MID}">{p2}</div>
      </div>
    </div>'''

# Topbar
def topbar(title="SW 19"):
    return f'''<div style="background:rgba(10,26,15,.95);backdrop-filter:blur(12px);padding:16px 20px;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid rgba(255,255,255,.05)">
    <span style="font-family:Georgia,serif;font-size:18px;color:#fff;font-weight:700;letter-spacing:.03em">{title}</span>
    <div style="display:flex;gap:8px">
      <div style="width:32px;height:32px;border-radius:8px;background:{CARD};border:1px solid {CARD_BORDER};display:flex;align-items:center;justify-content:center;font-size:14px">🏆</div>
      <div style="width:32px;height:32px;border-radius:8px;background:{CARD};border:1px solid {CARD_BORDER};display:flex;align-items:center;justify-content:center;font-size:14px">📊</div>
    </div>
  </div>'''

# Bottom nav
def bottomnav(active=0):
    tabs = ['Hoy','Scores','Draw','Chat','Ranking']
    icons = ['📅','⏱','🌳','💬','🏅']
    items = ''
    for i, (t, ic) in enumerate(zip(tabs, icons)):
        c = GREEN_BRIGHT if i == active else TEXT_MUTED
        items += f'<div style="flex:1;display:flex;flex-direction:column;align-items:center;gap:3px;font-size:9px;color:{c};font-weight:{"700" if i == active else "500"}">{ic}<span>{t}</span></div>'
    return f'''<div style="position:fixed;bottom:0;left:0;right:0;height:60px;background:rgba(10,15,12,.97);backdrop-filter:blur(12px);border-top:1px solid rgba(255,255,255,.05);display:flex;align-items:center;max-width:430px;margin:0 auto">{items}</div>'''

def wrap(body):
    return f'''<style>@keyframes pulse {{ 0%,100% {{ opacity:1 }} 50% {{ opacity:.3 }} }}</style>
    <div style="max-width:430px;margin:0 auto;min-height:932px;background:{BG};color:{TEXT};font-family:-apple-system,BlinkMacSystemFont,sans-serif;overflow:hidden;position:relative">
      <div style="padding-bottom:60px">
        {body}
      </div>
      {bottomnav(0)}
    </div>'''

# ========== DESIGNS ==========

def design_1():
    """Clean Cards - Simple dark cards, subtle borders, no court grouping"""
    return wrap(f'''{topbar()}
    <div style="padding:16px 16px 8px">
      <p style="font-size:11px;letter-spacing:.2em;color:{TEXT_MUTED};text-transform:uppercase;margin-bottom:4px">Martes 7 de Julio, 2026</p>
      <h2 style="font-family:Georgia,serif;font-size:22px;color:{TEXT};font-weight:700">Partidos de Hoy</h2>
    </div>
    <div style="padding:8px 16px">
      <div style="display:flex;gap:8px;margin-bottom:16px">
        <span style="font-size:11px;font-weight:700;color:{DANGER};background:rgba(255,82,82,.1);padding:4px 12px;border-radius:20px">2 En juego</span>
        <span style="font-size:11px;font-weight:600;color:{TEXT_MUTED};background:{CARD};padding:4px 12px;border-radius:20px">4 Por jugar</span>
      </div>
      <p style="font-size:10px;color:{GREEN_BRIGHT};font-weight:600;letter-spacing:.08em;text-transform:uppercase;margin-bottom:10px">Centre Court</p>
      {match_card("A. Zverev", "6  7  6", "C. Alcaraz", "4  5  2", "live")}
      {match_card("C. Ruud", "3  6", "H. Hurkacz", "6  4", "live")}
      <p style="font-size:10px;color:{GREEN_BRIGHT};font-weight:600;letter-spacing:.08em;text-transform:uppercase;margin:16px 0 10px">Court 1</p>
      {match_card_pick("D. Medvedev", "J. Sinner", "16:30")}
      {match_card_pick("F. Tsitsipas", "T. Fritz", "18:00")}
    </div>''')

def design_2():
    """Scoreboard LED - Green monospace, LED screen feel"""
    return wrap(f'''{topbar()}
    <div style="padding:16px 16px 8px">
      <div style="display:flex;justify-content:space-between;align-items:flex-end">
        <div>
          <p style="font-family:'Courier New',monospace;font-size:10px;color:{GREEN_BRIGHT};letter-spacing:.2em;text-transform:uppercase">Tue 07 Jul 2026</p>
          <h2 style="font-family:Georgia,serif;font-size:22px;color:{TEXT};margin-top:4px">Hoy</h2>
        </div>
        <div style="font-family:'Courier New',monospace;font-size:11px;color:{TEXT_MUTED};background:{CARD};border:1px solid {CARD_BORDER};padding:4px 10px;border-radius:6px">6 MATCHES</div>
      </div>
    </div>
    <div style="padding:8px 16px">
      <div style="display:flex;gap:0;margin-bottom:16px;border-bottom:1px solid {CARD_BORDER}">
        <span style="font-family:'Courier New',monospace;font-size:11px;font-weight:700;color:{DANGER};padding:8px 16px 8px 0;border-bottom:2px solid {DANGER};margin-bottom:-1px">EN JUEGO (2)</span>
        <span style="font-family:'Courier New',monospace;font-size:11px;color:{TEXT_MUTED};padding:8px 0 8px 16px">POR JUGAR (4)</span>
      </div>
      {match_card("A. Zverev", "6  7  6", "C. Alcaraz", "4  5  2", "live")}
      {match_card("C. Ruud", "3  6", "H. Hurkacz", "6  4", "live")}
      {match_card_pick("D. Medvedev", "J. Sinner", "16:30")}
      {match_card_pick("F. Tsitsipas", "T. Fritz", "18:00")}
    </div>''')

def design_3():
    """Court Sections - Accordion-style court headers"""
    return wrap(f'''{topbar()}
    <div style="padding:16px 16px 8px">
      <p style="font-size:11px;letter-spacing:.2em;color:{GOLD};text-transform:uppercase;margin-bottom:4px">Martes 7 de Julio</p>
      <h2 style="font-family:Georgia,serif;font-size:22px;color:{TEXT}">Partidos del Día</h2>
    </div>
    <div style="padding:8px 16px">
      <div style="display:flex;gap:8px;margin-bottom:16px;flex-wrap:wrap">
        <span style="font-size:10px;font-weight:700;color:{DANGER};background:rgba(255,82,82,.08);border:1px solid rgba(255,82,82,.2);padding:5px 12px;border-radius:20px">🔴 2 En juego</span>
        <span style="font-size:10px;font-weight:600;color:{GREEN_BRIGHT};background:{GREEN_PALE};border:1px solid {GREEN_BORDER};padding:5px 12px;border-radius:20px">🎾 4 Por jugar</span>
      </div>
      <div style="background:rgba(76,175,80,.04);border:1px solid {GREEN_BORDER};border-radius:12px;padding:14px 16px;margin-bottom:12px">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
          <span style="font-size:13px;font-weight:700;color:{GREEN_BRIGHT}">Centre Court</span>
          <span style="font-size:10px;color:{TEXT_MUTED};background:{CARD};padding:2px 8px;border-radius:10px">3 partidos</span>
        </div>
        {match_card("A. Zverev", "6  7  6", "C. Alcaraz", "4  5  2", "live")}
        {match_card_pick("D. Medvedev", "J. Sinner", "16:30")}
      </div>
      <div style="background:{CARD};border:1px solid {CARD_BORDER};border-radius:12px;padding:14px 16px;margin-bottom:12px">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
          <span style="font-size:13px;font-weight:700;color:{TEXT}">Court 1</span>
          <span style="font-size:10px;color:{TEXT_MUTED};background:{CARD};padding:2px 8px;border-radius:10px">2 partidos</span>
        </div>
        {match_card("C. Ruud", "3  6", "H. Hurkacz", "6  4", "live")}
        {match_card_pick("F. Tsitsipas", "T. Fritz", "18:00")}
      </div>
    </div>''')

def design_4():
    """Minimal Timeline - Time-based vertical timeline"""
    return wrap(f'''{topbar()}
    <div style="padding:16px 16px 8px">
      <h2 style="font-family:Georgia,serif;font-size:24px;color:{TEXT}">Hoy</h2>
      <p style="font-size:12px;color:{TEXT_MUTED};margin-top:4px">6 partidos programados</p>
    </div>
    <div style="padding:8px 16px">
      <div style="display:flex;gap:6px;margin-bottom:20px;overflow-x:auto">
        <span style="font-size:10px;font-weight:700;color:#0a1a0f;background:{DANGER};padding:5px 14px;border-radius:20px;white-space:nowrap">En juego</span>
        <span style="font-size:10px;font-weight:600;color:{TEXT_MUTED};background:{CARD};border:1px solid {CARD_BORDER};padding:5px 14px;border-radius:20px;white-space:nowrap">Por jugar</span>
      </div>
      <!-- Timeline -->
      <div style="position:relative;padding-left:20px">
        <div style="position:absolute;left:6px;top:8px;bottom:8px;width:1px;background:{CARD_BORDER}"></div>
        <div style="margin-bottom:20px;position:relative">
          <div style="position:absolute;left:-20px;top:4px;width:13px;height:13px;border-radius:50%;background:{DANGER};border:2px solid {BG};box-shadow:0 0 8px rgba(255,82,82,.4)"></div>
          <p style="font-size:10px;color:{DANGER};font-weight:700;margin-bottom:6px;letter-spacing:.05em">EN VIVO</p>
          {match_card("A. Zverev", "6  7  6", "C. Alcaraz", "4  5  2", "live")}
          {match_card("C. Ruud", "3  6", "H. Hurkacz", "6  4", "live")}
        </div>
        <div style="margin-bottom:20px;position:relative">
          <div style="position:absolute;left:-20px;top:4px;width:13px;height:13px;border-radius:50%;background:{CARD_BORDER};border:2px solid {BG}"></div>
          <p style="font-size:10px;color:{TEXT_MUTED};font-weight:600;margin-bottom:6px;letter-spacing:.05em">16:30</p>
          {match_card_pick("D. Medvedev", "J. Sinner", "16:30")}
        </div>
        <div style="position:relative">
          <div style="position:absolute;left:-20px;top:4px;width:13px;height:13px;border-radius:50%;background:{CARD_BORDER};border:2px solid {BG}"></div>
          <p style="font-size:10px;color:{TEXT_MUTED};font-weight:600;margin-bottom:6px;letter-spacing:.05em">18:00</p>
          {match_card_pick("F. Tsitsipas", "T. Fritz", "18:00")}
        </div>
      </div>
    </div>''')

def design_5():
    """Hero Live + Scroll - Big live section on top, upcoming below"""
    return wrap(f'''{topbar()}
    <div style="padding:16px 16px 8px;display:flex;justify-content:space-between;align-items:flex-end">
      <div>
        <h2 style="font-family:Georgia,serif;font-size:22px;color:{TEXT}">Hoy</h2>
        <p style="font-size:11px;color:{TEXT_MUTED};margin-top:2px">Martes 7 de Julio</p>
      </div>
      <span style="font-size:10px;font-weight:700;color:{DANGER};background:rgba(255,82,82,.1);border:1px solid rgba(255,82,82,.15);padding:4px 10px;border-radius:6px;display:flex;align-items:center;gap:4px"><span style="width:6px;height:6px;border-radius:50%;background:{DANGER};animation:pulse .8s infinite"></span>2 EN VIVO</span>
    </div>
    <!-- Live section -->
    <div style="padding:12px 16px">
      <div style="background:linear-gradient(135deg,rgba(255,82,82,.08),rgba(76,175,80,.05));border:1px solid rgba(255,82,82,.15);border-radius:16px;padding:16px;margin-bottom:16px">
        <p style="font-size:10px;color:{DANGER};font-weight:700;letter-spacing:.1em;margin-bottom:10px;text-transform:uppercase">En juego ahora</p>
        {match_card("A. Zverev", "6  7  6", "C. Alcaraz", "4  5  2", "live")}
        {match_card("C. Ruud", "3  6", "H. Hurkacz", "6  4", "live")}
      </div>
      <!-- Upcoming -->
      <p style="font-size:10px;color:{TEXT_MUTED};font-weight:600;letter-spacing:.08em;text-transform:uppercase;margin-bottom:10px">Próximos</p>
      {match_card_pick("D. Medvedev", "J. Sinner", "16:30")}
      {match_card_pick("F. Tsitsipas", "T. Fritz", "18:00")}
    </div>''')

def design_6():
    """Glassmorphism - Frosted glass cards on grass background"""
    return wrap(f'''{topbar()}
    <div style="padding:16px 16px 8px">
      <h2 style="font-family:Georgia,serif;font-size:22px;color:{TEXT}">Hoy</h2>
      <p style="font-size:11px;color:{TEXT_MUTED};margin-top:2px">Martes 7 de Julio · 6 partidos</p>
    </div>
    <div style="padding:8px 16px">
      <div style="display:flex;gap:8px;margin-bottom:16px">
        <span style="font-size:10px;font-weight:700;color:{DANGER};background:rgba(255,82,82,.12);backdrop-filter:blur(8px);padding:5px 14px;border-radius:20px;border:1px solid rgba(255,82,82,.2)">2 En juego</span>
        <span style="font-size:10px;font-weight:600;color:{TEXT_MUTED};background:rgba(255,255,255,.06);backdrop-filter:blur(8px);padding:5px 14px;border-radius:20px;border:1px solid {CARD_BORDER}">4 Por jugar</span>
      </div>
      {match_card("A. Zverev", "6  7  6", "C. Alcaraz", "4  5  2", "live")}
      {match_card("C. Ruud", "3  6", "H. Hurkacz", "6  4", "live")}
      {match_card_pick("D. Medvedev", "J. Sinner", "16:30")}
      {match_card_pick("F. Tsitsipas", "T. Fritz", "18:00")}
    </div>''')

def design_7():
    """Stats Header - Top stats bar, compact cards"""
    return wrap(f'''{topbar()}
    <div style="padding:16px 16px 8px">
      <h2 style="font-family:Georgia,serif;font-size:20px;color:{TEXT};margin-bottom:12px">Hoy</h2>
      <!-- Stats row -->
      <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:8px;margin-bottom:16px">
        <div style="background:rgba(255,82,82,.08);border:1px solid rgba(255,82,82,.15);border-radius:10px;padding:12px;text-align:center">
          <div style="font-size:22px;font-weight:800;color:{DANGER}">2</div>
          <div style="font-size:9px;color:{TEXT_MUTED};letter-spacing:.05em;margin-top:2px">EN JUEGO</div>
        </div>
        <div style="background:{GREEN_PALE};border:1px solid {GREEN_BORDER};border-radius:10px;padding:12px;text-align:center">
          <div style="font-size:22px;font-weight:800;color:{GREEN_BRIGHT}">4</div>
          <div style="font-size:9px;color:{TEXT_MUTED};letter-spacing:.05em;margin-top:2px">POR JUGAR</div>
        </div>
        <div style="background:{CARD};border:1px solid {CARD_BORDER};border-radius:10px;padding:12px;text-align:center">
          <div style="font-size:22px;font-weight:800;color:{TEXT_MID}">6</div>
          <div style="font-size:9px;color:{TEXT_MUTED};letter-spacing:.05em;margin-top:2px">TOTAL</div>
        </div>
      </div>
      <div style="display:flex;gap:0;margin-bottom:12px;border-bottom:1px solid {CARD_BORDER}">
        <span style="font-size:12px;font-weight:700;color:{TEXT};padding:8px 12px 8px 0;border-bottom:2px solid {GREEN_BRIGHT};margin-bottom:-1px">En juego</span>
        <span style="font-size:12px;color:{TEXT_MUTED};padding:8px 0 8px 12px">Por jugar</span>
      </div>
      {match_card("A. Zverev", "6  7  6", "C. Alcaraz", "4  5  2", "live")}
      {match_card("C. Ruud", "3  6", "H. Hurkacz", "6  4", "live")}
      {match_card_pick("D. Medvedev", "J. Sinner", "16:30")}
    </div>''')

def design_8():
    """Horizontal Scroll - Horizontal swipeable cards for each match"""
    return wrap(f'''{topbar()}
    <div style="padding:16px 16px 8px">
      <h2 style="font-family:Georgia,serif;font-size:22px;color:{TEXT}">Hoy</h2>
      <p style="font-size:11px;color:{TEXT_MUTED};margin-top:2px">Martes 7 de Julio</p>
    </div>
    <div style="padding:8px 16px">
      <div style="display:flex;gap:8px;margin-bottom:16px">
        <span style="font-size:10px;font-weight:700;color:{DANGER};background:rgba(255,82,82,.1);padding:5px 14px;border-radius:20px">2 En juego</span>
        <span style="font-size:10px;font-weight:600;color:{TEXT_MUTED};background:{CARD};padding:5px 14px;border-radius:20px">4 Por jugar</span>
      </div>
      <!-- Wide horizontal cards -->
      <div style="display:flex;gap:10px;overflow-x:auto;padding-bottom:8px;margin-bottom:12px">
        <div style="min-width:300px;background:rgba(255,82,82,.06);border:1px solid rgba(255,82,82,.15);border-radius:14px;padding:14px 16px">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px">
            <span style="font-size:10px;color:{TEXT_MUTED}">Centre Court</span>
            <span style="font-size:9px;color:{DANGER};font-weight:700;display:flex;align-items:center"><span style="display:inline-block;width:5px;height:5px;border-radius:50%;background:{DANGER};animation:pulse .8s infinite;margin-right:4px"></span>LIVE</span>
          </div>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <div><div style="font-size:13px;font-weight:600;color:{TEXT}">A. Zverev</div><div style="font-size:18px;font-weight:800;color:{TEXT};font-family:monospace;margin-top:4px">6 - 7 - 6</div></div>
            <div style="font-size:11px;color:{TEXT_MUTED};margin:0 12px">vs</div>
            <div style="text-align:right"><div style="font-size:13px;font-weight:600;color:{TEXT}">C. Alcaraz</div><div style="font-size:18px;font-weight:800;color:{TEXT};font-family:monospace;margin-top:4px">4 - 5 - 2</div></div>
          </div>
        </div>
        <div style="min-width:300px;background:rgba(255,82,82,.06);border:1px solid rgba(255,82,82,.15);border-radius:14px;padding:14px 16px">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px">
            <span style="font-size:10px;color:{TEXT_MUTED}">Court 1</span>
            <span style="font-size:9px;color:{DANGER};font-weight:700;display:flex;align-items:center"><span style="display:inline-block;width:5px;height:5px;border-radius:50%;background:{DANGER};animation:pulse .8s infinite;margin-right:4px"></span>LIVE</span>
          </div>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <div><div style="font-size:13px;font-weight:600;color:{TEXT}">C. Ruud</div><div style="font-size:18px;font-weight:800;color:{TEXT};font-family:monospace;margin-top:4px">3 - 6</div></div>
            <div style="font-size:11px;color:{TEXT_MUTED};margin:0 12px">vs</div>
            <div style="text-align:right"><div style="font-size:13px;font-weight:600;color:{TEXT}">H. Hurkacz</div><div style="font-size:18px;font-weight:800;color:{TEXT};font-family:monospace;margin-top:4px">6 - 4</div></div>
          </div>
        </div>
      </div>
      <p style="font-size:10px;color:{TEXT_MUTED};font-weight:600;letter-spacing:.08em;text-transform:uppercase;margin-bottom:10px">Próximos</p>
      {match_card_pick("D. Medvedev", "J. Sinner", "16:30")}
      {match_card_pick("F. Tsitsipas", "T. Fritz", "18:00")}
    </div>''')

def design_9():
    """Split Status - Two-column layout for live vs upcoming"""
    return wrap(f'''{topbar()}
    <div style="padding:16px 16px 8px">
      <h2 style="font-family:Georgia,serif;font-size:22px;color:{TEXT}">Hoy</h2>
    </div>
    <div style="padding:8px 16px">
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:16px">
        <div style="background:rgba(255,82,82,.06);border:1px solid rgba(255,82,82,.12);border-radius:12px;padding:12px;text-align:center">
          <div style="display:flex;align-items:center;justify-content:center;gap:4px;margin-bottom:2px"><span style="width:6px;height:6px;border-radius:50%;background:{DANGER};animation:pulse .8s infinite"></span><span style="font-size:9px;color:{DANGER};font-weight:700;letter-spacing:.08em">LIVE</span></div>
          <div style="font-size:20px;font-weight:800;color:{DANGER}">2</div>
        </div>
        <div style="background:{GREEN_PALE};border:1px solid {GREEN_BORDER};border-radius:12px;padding:12px;text-align:center">
          <div style="font-size:9px;color:{GREEN_BRIGHT};font-weight:700;letter-spacing:.08em;margin-bottom:2px">POR JUGAR</div>
          <div style="font-size:20px;font-weight:800;color:{GREEN_BRIGHT}">4</div>
        </div>
      </div>
      {match_card("A. Zverev", "6  7  6", "C. Alcaraz", "4  5  2", "live")}
      {match_card("C. Ruud", "3  6", "H. Hurkacz", "6  4", "live")}
      {match_card_pick("D. Medvedev", "J. Sinner", "16:30")}
      {match_card_pick("F. Tsitsipas", "T. Fritz", "18:00")}
    </div>''')

def design_10():
    """Elegant Serif - Georgia everywhere, gold accents, luxury feel"""
    return wrap(f'''<div style="background:rgba(10,26,15,.95);backdrop-filter:blur(12px);padding:16px 20px;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid rgba(200,169,81,.15)">
    <span style="font-family:Georgia,serif;font-size:18px;color:{GOLD};font-weight:700;letter-spacing:.03em">SW 19</span>
    <div style="display:flex;gap:8px">
      <div style="width:32px;height:32px;border-radius:8px;background:rgba(200,169,81,.08);border:1px solid rgba(200,169,81,.15);display:flex;align-items:center;justify-content:center;font-size:12px;color:{GOLD}">📊</div>
    </div>
  </div>
    <div style="padding:20px 16px 8px;text-align:center">
      <p style="font-family:Georgia,serif;font-size:11px;letter-spacing:.25em;color:{GOLD};text-transform:uppercase;opacity:.7">The Championships</p>
      <h2 style="font-family:Georgia,serif;font-size:26px;color:{TEXT};margin-top:8px;font-weight:300">Partidos del Día</h2>
      <div style="width:40px;height:1px;background:{GOLD};margin:12px auto;opacity:.4"></div>
    </div>
    <div style="padding:8px 16px">
      <div style="display:flex;justify-content:center;gap:8px;margin-bottom:16px">
        <span style="font-family:Georgia,serif;font-size:10px;font-weight:700;color:{DANGER};background:rgba(255,82,82,.08);border:1px solid rgba(255,82,82,.15);padding:5px 14px;border-radius:20px">2 En juego</span>
        <span style="font-family:Georgia,serif;font-size:10px;font-weight:600;color:{TEXT_MUTED};background:{CARD};border:1px solid {CARD_BORDER};padding:5px 14px;border-radius:20px">4 Por jugar</span>
      </div>
      <div style="border:1px solid rgba(200,169,81,.1);border-radius:14px;overflow:hidden;margin-bottom:10px">
        <div style="background:rgba(200,169,81,.04);padding:8px 16px;border-bottom:1px solid rgba(200,169,81,.1)"><span style="font-family:Georgia,serif;font-size:11px;color:{GOLD};font-weight:600">Centre Court</span></div>
        <div style="padding:10px 16px">
          {match_card("A. Zverev", "6  7  6", "C. Alcaraz", "4  5  2", "live")}
        </div>
      </div>
      <div style="border:1px solid {CARD_BORDER};border-radius:14px;overflow:hidden;margin-bottom:10px">
        <div style="background:{CARD};padding:8px 16px;border-bottom:1px solid {CARD_BORDER}"><span style="font-family:Georgia,serif;font-size:11px;color:{TEXT_MID};font-weight:600">Court 1</span></div>
        <div style="padding:10px 16px">
          {match_card("C. Ruud", "3  6", "H. Hurkacz", "6  4", "live")}
          {match_card_pick("F. Tsitsipas", "T. Fritz", "18:00")}
        </div>
      </div>
    </div>''')


async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch()
        page = await browser.new_page(viewport={"width": 430, "height": 932})

        designs = [
            ("1-Clean-Cards", design_1),
            ("2-Scoreboard-LED", design_2),
            ("3-Court-Sections", design_3),
            ("4-Minimal-Timeline", design_4),
            ("5-Hero-Live-Scroll", design_5),
            ("6-Glassmorphism", design_6),
            ("7-Stats-Header", design_7),
            ("8-Horizontal-Scroll", design_8),
            ("9-Split-Status", design_9),
            ("10-Elegant-Serif", design_10),
        ]

        for name, fn in designs:
            out = f"/home/z/my-project/download/hoy-{name.lower()}.png"
            await page.set_content(fn(), wait_until="networkidle")
            await page.screenshot(path=out, full_page=True)
            print(f"  {name} -> {out}")

        await browser.close()
        print("Done!")

asyncio.run(main())