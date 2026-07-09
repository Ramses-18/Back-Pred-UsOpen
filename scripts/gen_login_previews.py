import asyncio
from playwright.async_api import async_playwright

DESIGNS = """
<!DOCTYPE html><html lang="es"><head><meta charset="UTF-8"><meta name="viewport" content="width=430"><style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif}
</style></head><body>
"""

DESIGN_1 = """
<div style="min-height:932px;width:430px;display:flex;flex-direction:column;background:#0D3B1E;position:relative;overflow:hidden">
  <!-- Diagonal pattern -->
  <div style="position:absolute;inset:0;opacity:.08;background:repeating-linear-gradient(45deg,transparent,transparent 18px,#fff 18px,#fff 20px)"></div>
  <div style="position:absolute;inset:0;opacity:.04;background:repeating-linear-gradient(-45deg,transparent,transparent 18px,#fff 18px,#fff 20px)"></div>
  <!-- Hero -->
  <div style="position:relative;padding:64px 32px 40px;text-align:center">
    <div style="font-size:52px;margin-bottom:12px">🏆</div>
    <h1 style="font-family:Georgia,serif;font-size:32px;color:#C8A951;line-height:1.15;letter-spacing:.02em">The Championships<br/>Wimbledon 2026</h1>
    <p style="font-size:13px;color:rgba(200,169,81,.5);margin-top:8px;letter-spacing:.1em;text-transform:uppercase">Pronósticos · Singles Masculino</p>
  </div>
  <!-- Form card -->
  <div style="position:relative;flex:1;background:#F5F0E8;border-radius:28px 28px 0 0;padding:32px 28px 48px;border-top:2px solid rgba(200,169,81,.3)">
    <div style="display:flex;border:1px solid #D6CFC2;border-radius:10px;overflow:hidden;margin-bottom:24px">
      <div style="flex:1;padding:12px;text-align:center;font-size:14;font-weight:600;background:#1B5E20;color:#fff;cursor:pointer">Ingresar</div>
      <div style="flex:1;padding:12px;text-align:center;font-size:14;font-weight:500;color:#8A8272;cursor:pointer">Registrarse</div>
    </div>
    <div style="margin-bottom:16px"><label style="font-size:12px;font-weight:600;color:#5A5548;display:block;margin-bottom:6px">Correo electrónico</label>
      <input type="email" placeholder="tu@email.com" style="width:100%;padding:14px 16px;border:1px solid #D6CFC2;border-radius:10px;font-size:14;background:#FFF;color:#333;outline:none"/></div>
    <div style="margin-bottom:24px"><label style="font-size:12px;font-weight:600;color:#5A5548;display:block;margin-bottom:6px">Contraseña</label>
      <input type="password" placeholder="••••••••" value="••••••••" style="width:100%;padding:14px 16px;border:1px solid #D6CFC2;border-radius:10px;font-size:14;background:#FFF;color:#333;outline:none"/></div>
    <button style="width:100%;padding:15px;border:none;border-radius:12px;background:linear-gradient(135deg,#1B5E20,#2E7D32);color:#fff;font-size:15;font-weight:700;cursor:pointer;letter-spacing:.02em">Ingresar</button>
    <p style="text-align:center;font-size:11px;color:#A09882;margin-top:16px;line-height:1.5">Bienvenido a Wimbledon 2026.<br/>Si no tenés cuenta, registrate.</p>
  </div>
</div>
"""

DESIGN_2 = """
<div style="min-height:932px;width:430px;display:flex;flex-direction:column;position:relative;overflow:hidden;background:#0a1a0f">
  <!-- Grass gradient overlay -->
  <div style="position:absolute;top:0;left:0;right:0;height:55%;background:linear-gradient(180deg,rgba(10,26,15,.95) 0%,rgba(27,94,32,.3) 70%,transparent 100%)"></div>
  <div style="position:absolute;top:0;left:0;right:0;height:100%;background:linear-gradient(180deg,rgba(27,94,32,.15) 0%,transparent 50%)"></div>
  <!-- Hero -->
  <div style="position:relative;padding:80px 32px 48px;text-align:center">
    <p style="font-size:11px;letter-spacing:.3em;color:rgba(255,255,255,.3);text-transform:uppercase;margin-bottom:16px">The Championships</p>
    <h1 style="font-family:Georgia,serif;font-size:64px;color:#fff;font-weight:300;letter-spacing:.04em;line-height:1">SW 19</h1>
    <div style="width:40px;height:2px;background:#4CAF50;margin:20px auto"></div>
    <p style="font-size:13px;color:rgba(255,255,255,.4)">Wimbledon 2026 · Pronósticos</p>
  </div>
  <!-- Form -->
  <div style="position:relative;flex:1;margin:0 20px;background:rgba(255,255,255,.97);border-radius:20px;padding:32px 24px 48px;box-shadow:0 20px 60px rgba(0,0,0,.3)">
    <div style="display:flex;border:1px solid #E8E4DC;border-radius:8px;overflow:hidden;margin-bottom:28px">
      <div style="flex:1;padding:11px;text-align:center;font-size:13;font-weight:600;background:#1B5E20;color:#fff;cursor:pointer">Ingresar</div>
      <div style="flex:1;padding:11px;text-align:center;font-size:13;font-weight:500;color:#999;cursor:pointer">Registrarse</div>
    </div>
    <div style="margin-bottom:16px"><label style="font-size:11px;font-weight:600;color:#666;display:block;margin-bottom:6px;text-transform:uppercase;letter-spacing:.05em">Email</label>
      <input type="email" placeholder="tu@email.com" style="width:100%;padding:14px 0;border:none;border-bottom:2px solid #1B5E20;font-size:14;background:transparent;color:#333;outline:none"/></div>
    <div style="margin-bottom:28px"><label style="font-size:11px;font-weight:600;color:#666;display:block;margin-bottom:6px;text-transform:uppercase;letter-spacing:.05em">Contraseña</label>
      <input type="password" placeholder="••••••••" value="••••••••" style="width:100%;padding:14px 0;border:none;border-bottom:2px solid #1B5E20;font-size:14;background:transparent;color:#333;outline:none"/></div>
    <button style="width:100%;padding:16px;border:none;border-radius:12px;background:#1B5E20;color:#fff;font-size:15;font-weight:700;cursor:pointer">Ingresar</button>
    <p style="text-align:center;font-size:11px;color:#aaa;margin-top:16px">Bienvenido a Wimbledon 2026</p>
  </div>
</div>
"""

DESIGN_3 = """
<div style="min-height:932px;width:430px;display:flex;flex-direction:column;background:#F5F0E1;position:relative">
  <!-- Double border frame -->
  <div style="position:absolute;top:16px;left:16px;right:16px;bottom:16px;border:2px solid #1B5E20;border-radius:4px;pointer-events:none;z-index:1"></div>
  <div style="position:absolute;top:22px;left:22px;right:22px;bottom:22px;border:1px solid #C8A951;border-radius:2px;pointer-events:none;z-index:1"></div>
  <!-- Hero -->
  <div style="padding:80px 40px 40px;text-align:center">
    <div style="font-family:'Times New Roman',Georgia,serif;font-size:14px;letter-spacing:.25em;color:#1B5E20;text-transform:uppercase;margin-bottom:20px">The All England Lawn Tennis Club</div>
    <h1 style="font-family:'Times New Roman',Georgia,serif;font-size:38px;color:#1B5E20;line-height:1.1;font-weight:700">Wimbledon<br/>2026</h1>
    <div style="width:1px;height:40px;background:#C8A951;margin:20px auto"></div>
    <p style="font-family:'Times New Roman',Georgia,serif;font-size:14px;color:#8A7A5A;font-style:italic">Pronósticos · Gentlemen's Singles</p>
  </div>
  <!-- Form -->
  <div style="flex:1;padding:24px 40px 60px">
    <div style="display:flex;border:1px solid #C8A951;border-radius:6px;overflow:hidden;margin-bottom:28px">
      <div style="flex:1;padding:12px;text-align:center;font-size:13;font-weight:600;font-family:Georgia,serif;background:#1B5E20;color:#fff;cursor:pointer">Ingresar</div>
      <div style="flex:1;padding:12px;text-align:center;font-size:13;font-weight:500;font-family:Georgia,serif;color:#8A7A5A;cursor:pointer">Registrarse</div>
    </div>
    <div style="margin-bottom:16px"><label style="font-family:Georgia,serif;font-size:12px;font-weight:600;color:#5A5548;display:block;margin-bottom:8px">Correo electrónico</label>
      <input type="email" placeholder="tu@email.com" style="width:100%;padding:14px 16px;border:1px solid #D6CFC2;border-radius:8px;font-size:14;font-family:Georgia,serif;background:#FFFDF8;color:#333;outline:none"/></div>
    <div style="margin-bottom:28px"><label style="font-family:Georgia,serif;font-size:12px;font-weight:600;color:#5A5548;display:block;margin-bottom:8px">Contraseña</label>
      <input type="password" placeholder="••••••••" value="••••••••" style="width:100%;padding:14px 16px;border:1px solid #D6CFC2;border-radius:8px;font-size:14;font-family:Georgia,serif;background:#FFFDF8;color:#333;outline:none"/></div>
    <button style="width:100%;padding:16px;border:2px solid #1B5E20;border-radius:8px;background:#1B5E20;color:#fff;font-size:15;font-weight:700;font-family:Georgia,serif;cursor:pointer;text-transform:uppercase;letter-spacing:.1em">Ingresar</button>
    <p style="text-align:center;font-family:Georgia,serif;font-size:11px;color:#A09882;margin-top:20px;font-style:italic">Si no tenés cuenta, registrate para participar.</p>
  </div>
</div>
"""

DESIGN_4 = """
<div style="min-height:932px;width:430px;display:flex;flex-direction:column;background:#121212;position:relative">
  <!-- Subtle gradient accent -->
  <div style="position:absolute;top:-100px;right:-100px;width:300px;height:300px;background:radial-gradient(circle,rgba(76,175,80,.08) 0%,transparent 70%);border-radius:50%"></div>
  <!-- Hero -->
  <div style="padding:72px 32px 32px">
    <div style="font-size:11px;letter-spacing:.3em;color:#4CAF50;text-transform:uppercase;margin-bottom:12px">Wimbledon 2026</div>
    <h1 style="font-size:28px;color:#fff;font-weight:800;line-height:1.15">Pronósticos<br/><span style="color:#4CAF50">del Torneo</span></h1>
    <p style="font-size:13px;color:rgba(255,255,255,.3);margin-top:8px">Singles Masculino</p>
  </div>
  <!-- Form -->
  <div style="flex:1;padding:0 32px 60px">
    <div style="display:flex;gap:0;margin-bottom:32px;border-bottom:2px solid #2A2A2A">
      <div style="flex:1;padding-bottom:12px;text-align:center;font-size:14;font-weight:600;color:#4CAF50;border-bottom:2px solid #4CAF50;margin-bottom:-2px;cursor:pointer">Ingresar</div>
      <div style="flex:1;padding-bottom:12px;text-align:center;font-size:14;font-weight:500;color:#666;cursor:pointer">Registrarse</div>
    </div>
    <div style="margin-bottom:28px"><label style="font-size:11px;font-weight:600;color:#666;display:block;margin-bottom:8px;text-transform:uppercase;letter-spacing:.08em">Email</label>
      <input type="email" placeholder="tu@email.com" style="width:100%;padding:14px 0;border:none;border-bottom:1px solid #333;font-size:15;background:transparent;color:#fff;outline:none"/></div>
    <div style="margin-bottom:32px"><label style="font-size:11px;font-weight:600;color:#666;display:block;margin-bottom:8px;text-transform:uppercase;letter-spacing:.08em">Contraseña</label>
      <input type="password" placeholder="••••••••" value="••••••••" style="width:100%;padding:14px 0;border:none;border-bottom:1px solid #333;font-size:15;background:transparent;color:#fff;outline:none"/></div>
    <button style="width:100%;padding:16px;border:none;border-radius:14px;background:linear-gradient(135deg,#1B5E20 0%,#4CAF50 100%);color:#fff;font-size:15;font-weight:700;cursor:pointer;letter-spacing:.02em">INGRESAR</button>
    <p style="text-align:center;font-size:11px;color:#555;margin-top:20px">SW19 · Wimbledon 2026</p>
  </div>
</div>
"""

DESIGN_5 = """
<div style="min-height:932px;width:430px;display:flex;flex-direction:column;position:relative;overflow:hidden">
  <!-- Grass texture gradient -->
  <div style="position:absolute;inset:0;background:linear-gradient(170deg,#1a472a 0%,#2d6a3e 30%,#3a8a50 50%,#2d6a3e 70%,#1a472a 100%)"></div>
  <div style="position:absolute;inset:0;background:repeating-linear-gradient(90deg,transparent,transparent 2px,rgba(255,255,255,.02) 2px,rgba(255,255,255,.02) 4px)"></div>
  <!-- Tennis ball -->
  <div style="position:relative;text-align:center;padding:56px 32px 32px">
    <div style="display:inline-block;width:64px;height:64px;background:radial-gradient(circle at 35% 35%,#d4f542,#a8d935,#7cb518);border-radius:50%;margin-bottom:20px;box-shadow:0 4px 20px rgba(0,0,0,.2)">
      <div style="position:absolute;top:50%;left:8%;right:8%;height:3px;background:rgba(255,255,255,.6);border-radius:2px;transform:translateY(-50%) rotate(-15deg)"></div>
    </div>
    <h1 style="font-family:Georgia,serif;font-size:30px;color:#fff;line-height:1.15;text-shadow:0 2px 8px rgba(0,0,0,.3)">Wimbledon 2026</h1>
    <p style="font-size:13px;color:rgba(255,255,255,.6);margin-top:6px">Pronósticos · Singles Masculino</p>
  </div>
  <!-- Form -->
  <div style="position:relative;flex:1;margin:0 20px;background:rgba(255,255,255,.12);backdrop-filter:blur(20px);-webkit-backdrop-filter:blur(20px);border-radius:24px 24px 0 0;padding:28px 24px 48px;border-top:1px solid rgba(255,255,255,.15)">
    <div style="display:flex;border:1px solid rgba(255,255,255,.15);border-radius:10px;overflow:hidden;margin-bottom:24px">
      <div style="flex:1;padding:12px;text-align:center;font-size:14;font-weight:600;background:rgba(255,255,255,.2);color:#fff;cursor:pointer">Ingresar</div>
      <div style="flex:1;padding:12px;text-align:center;font-size:14;font-weight:500;color:rgba(255,255,255,.5);cursor:pointer">Registrarse</div>
    </div>
    <div style="margin-bottom:16px"><label style="font-size:12px;font-weight:600;color:rgba(255,255,255,.7);display:block;margin-bottom:6px">Correo electrónico</label>
      <input type="email" placeholder="tu@email.com" style="width:100%;padding:14px 16px;border:1px solid rgba(255,255,255,.15);border-radius:10px;font-size:14;background:rgba(255,255,255,.1);color:#fff;outline:none;placeholder-color:rgba(255,255,255,.4)"/></div>
    <div style="margin-bottom:24px"><label style="font-size:12px;font-weight:600;color:rgba(255,255,255,.7);display:block;margin-bottom:6px">Contraseña</label>
      <input type="password" placeholder="••••••••" value="••••••••" style="width:100%;padding:14px 16px;border:1px solid rgba(255,255,255,.15);border-radius:10px;font-size:14;background:rgba(255,255,255,.1);color:#fff;outline:none"/></div>
    <button style="width:100%;padding:16px;border:none;border-radius:12px;background:#fff;color:#1B5E20;font-size:15;font-weight:700;cursor:pointer">Ingresar</button>
    <p style="text-align:center;font-size:11px;color:rgba(255,255,255,.4);margin-top:16px">Bienvenido al torneo 🎾</p>
  </div>
</div>
"""

DESIGN_6 = """
<div style="min-height:932px;width:430px;display:flex;flex-direction:column;background:#FFFFFF;position:relative">
  <!-- Green accent line top -->
  <div style="height:4px;background:#1B5E20;width:100%"></div>
  <!-- Hero -->
  <div style="padding:72px 32px 32px">
    <p style="font-size:11px;letter-spacing:.2em;color:#1B5E20;text-transform:uppercase;font-weight:600">SW 19</p>
    <h1 style="font-size:26px;color:#111;font-weight:300;margin-top:8px;line-height:1.3">Wimbledon<br/><span style="font-weight:800">2026</span></h1>
    <p style="font-size:13px;color:#aaa;margin-top:8px">Pronósticos · Singles Masculino</p>
  </div>
  <!-- Divider -->
  <div style="margin:0 32px 32px;height:1px;background:#eee"></div>
  <!-- Form -->
  <div style="flex:1;padding:0 32px 60px">
    <div style="display:flex;gap:24px;margin-bottom:32px">
      <div style="font-size:14;font-weight:700;color:#1B5E20;cursor:pointer;border-bottom:2px solid #1B5E20;padding-bottom:8px">Ingresar</div>
      <div style="font-size:14;font-weight:500;color:#ccc;cursor:pointer;padding-bottom:8px">Registrarse</div>
    </div>
    <div style="margin-bottom:24px">
      <input type="email" placeholder="Correo electrónico" style="width:100%;padding:14px 0;border:none;border-bottom:1px solid #ddd;font-size:15;background:transparent;color:#111;outline:none;transition:border-color .2s"/></div>
    <div style="margin-bottom:32px">
      <input type="password" placeholder="Contraseña" value="••••••••" style="width:100%;padding:14px 0;border:none;border-bottom:1px solid #ddd;font-size:15;background:transparent;color:#111;outline:none"/></div>
    <button style="width:100%;padding:16px;border:none;border-radius:50px;background:#1B5E20;color:#fff;font-size:15;font-weight:600;cursor:pointer">Ingresar</button>
    <p style="text-align:center;font-size:11px;color:#ccc;margin-top:20px">¿No tenés cuenta? <span style="color:#1B5E20;font-weight:600">Registrate</span></p>
  </div>
</div>
"""

DESIGN_7 = """
<div style="min-height:932px;width:430px;display:flex;flex-direction:column;background:#0a1f0d;position:relative;overflow:hidden">
  <!-- LED scanlines -->
  <div style="position:absolute;inset:0;background:repeating-linear-gradient(0deg,transparent,transparent 2px,rgba(0,0,0,.15) 2px,rgba(0,0,0,.15) 4px);pointer-events:none"></div>
  <!-- Scoreboard header -->
  <div style="position:relative;padding:48px 24px 24px;text-align:center">
    <div style="display:inline-flex;align-items:center;gap:8px;background:rgba(76,175,80,.1);border:1px solid rgba(76,175,80,.2);border-radius:6px;padding:6px 16px;margin-bottom:24px">
      <div style="width:6px;height:6px;border-radius:50%;background:#4CAF50;box-shadow:0 0 8px #4CAF50"></div>
      <span style="font-size:10px;letter-spacing:.15em;color:#4CAF50;text-transform:uppercase">Live Scoring</span>
    </div>
    <h1 style="font-family:'Courier New',monospace;font-size:36px;color:#4CAF50;font-weight:700;line-height:1;text-shadow:0 0 20px rgba(76,175,80,.3)">WIMBLEDON</h1>
    <div style="font-family:'Courier New',monospace;font-size:14px;color:rgba(76,175,80,.5);margin-top:4px;letter-spacing:.2em">2026 · PRONÓSTICOS</div>
  </div>
  <!-- Scoreboard-style tabs -->
  <div style="position:relative;margin:0 24px;display:flex;background:#0d2a12;border:1px solid rgba(76,175,80,.2);border-radius:4px;overflow:hidden;margin-bottom:24px">
    <div style="flex:1;padding:12px;text-align:center;font-family:'Courier New',monospace;font-size:13;font-weight:700;color:#0a1f0d;background:#4CAF50;cursor:pointer">SET 1 — INGRESAR</div>
    <div style="flex:1;padding:12px;text-align:center;font-family:'Courier New',monospace;font-size:13;color:rgba(76,175,80,.4);cursor:pointer">SET 2 — REGISTRO</div>
  </div>
  <!-- Form -->
  <div style="position:relative;flex:1;padding:0 24px 48px">
    <div style="margin-bottom:20px"><label style="font-family:'Courier New',monospace;font-size:10px;letter-spacing:.15em;color:rgba(76,175,80,.5);display:block;margin-bottom:8px;text-transform:uppercase">Email</label>
      <input type="email" placeholder="tu@email.com" style="width:100%;padding:14px 16px;border:1px solid rgba(76,175,80,.2);border-radius:4px;font-size:14;font-family:'Courier New',monospace;background:rgba(76,175,80,.05);color:#4CAF50;outline:none"/></div>
    <div style="margin-bottom:28px"><label style="font-family:'Courier New',monospace;font-size:10px;letter-spacing:.15em;color:rgba(76,175,80,.5);display:block;margin-bottom:8px;text-transform:uppercase">Password</label>
      <input type="password" placeholder="••••••••" value="••••••••" style="width:100%;padding:14px 16px;border:1px solid rgba(76,175,80,.2);border-radius:4px;font-size:14;font-family:'Courier New',monospace;background:rgba(76,175,80,.05);color:#4CAF50;outline:none"/></div>
    <button style="width:100%;padding:16px;border:2px solid #4CAF50;border-radius:4px;background:transparent;color:#4CAF50;font-size:15;font-weight:700;font-family:'Courier New',monospace;cursor:pointer;letter-spacing:.15em;text-transform:uppercase;display:flex;align-items:center;justify-content:center;gap:10px">
      <span>▶</span> PLAY
    </button>
    <p style="text-align:center;font-family:'Courier New',monospace;font-size:10px;color:rgba(76,175,80,.3);margin-top:20px;letter-spacing:.1em">© THE ALL ENGLAND CLUB 2026</p>
  </div>
</div>
"""

DESIGN_8 = """
<div style="min-height:932px;width:430px;display:flex;flex-direction:column;position:relative;overflow:hidden">
  <!-- Left green panel -->
  <div style="background:#1B5E20;padding:64px 28px 48px;position:relative">
    <div style="position:absolute;bottom:-30px;right:-30px;width:120px;height:120px;border:3px solid rgba(255,255,255,.08);border-radius:50%"></div>
    <div style="position:absolute;bottom:-60px;right:-60px;width:180px;height:180px;border:2px solid rgba(255,255,255,.04);border-radius:50%"></div>
    <div style="font-size:11px;letter-spacing:.25em;color:rgba(255,255,255,.35);text-transform:uppercase;margin-bottom:16px">SW 19</div>
    <h1 style="font-family:Georgia,serif;font-size:32px;color:#fff;line-height:1.15;font-weight:700">The<br/>Championships</h1>
    <p style="font-size:13px;color:rgba(255,255,255,.5);margin-top:10px">Wimbledon 2026<br/>Pronósticos</p>
    <div style="margin-top:32px;display:flex;gap:8px">
      <div style="width:24px;height:3px;background:rgba(255,255,255,.5);border-radius:2px"></div>
      <div style="width:24px;height:3px;background:rgba(255,255,255,.2);border-radius:2px"></div>
      <div style="width:24px;height:3px;background:rgba(255,255,255,.2);border-radius:2px"></div>
    </div>
  </div>
  <!-- Right cream form -->
  <div style="flex:1;background:#FAF8F4;padding:32px 28px 48px;border-radius:28px 28px 0 0;margin-top:-20px;position:relative;z-index:1">
    <div style="display:flex;border:1px solid #E8E2D6;border-radius:10px;overflow:hidden;margin-bottom:24px">
      <div style="flex:1;padding:12px;text-align:center;font-size:14;font-weight:600;background:#1B5E20;color:#fff;cursor:pointer">Ingresar</div>
      <div style="flex:1;padding:12px;text-align:center;font-size:14;font-weight:500;color:#A09882;cursor:pointer">Registrarse</div>
    </div>
    <div style="margin-bottom:16px"><label style="font-size:12px;font-weight:600;color:#6A6456;display:block;margin-bottom:6px">Correo electrónico</label>
      <input type="email" placeholder="tu@email.com" style="width:100%;padding:14px 16px;border:1px solid #E8E2D6;border-radius:10px;font-size:14;background:#fff;color:#333;outline:none"/></div>
    <div style="margin-bottom:24px"><label style="font-size:12px;font-weight:600;color:#6A6456;display:block;margin-bottom:6px">Contraseña</label>
      <input type="password" placeholder="••••••••" value="••••••••" style="width:100%;padding:14px 16px;border:1px solid #E8E2D6;border-radius:10px;font-size:14;background:#fff;color:#333;outline:none"/></div>
    <button style="width:100%;padding:16px;border:none;border-radius:12px;background:#1B5E20;color:#fff;font-size:15;font-weight:700;cursor:pointer">Ingresar</button>
    <p style="text-align:center;font-size:11px;color:#B0A896;margin-top:16px">Bienvenido a Wimbledon 2026</p>
  </div>
</div>
"""

DESIGN_9 = """
<div style="min-height:932px;width:430px;display:flex;flex-direction:column;background:#050505;position:relative;overflow:hidden">
  <!-- Neon court lines -->
  <div style="position:absolute;top:0;left:0;right:0;bottom:0;opacity:.15">
    <div style="position:absolute;top:120px;left:40px;right:40px;height:1px;background:#39FF14;box-shadow:0 0 10px #39FF14"></div>
    <div style="position:absolute;top:120px;left:40px;right:40px;height:300px;border:1px solid #39FF14;border-top:none;box-shadow:0 0 15px rgba(57,255,20,.3)"></div>
    <div style="position:absolute;top:270px;left:50%;width:1px;height:150px;background:#39FF14;transform:translateX(-50%);box-shadow:0 0 10px #39FF14"></div>
    <div style="position:absolute;top:120px;left:50%;width:80px;height:80px;border:1px solid #39FF14;border-radius:50%;transform:translate(-50%,-50%);box-shadow:0 0 15px rgba(57,255,20,.3)"></div>
  </div>
  <!-- Hero -->
  <div style="position:relative;padding:80px 32px 40px;text-align:center">
    <p style="font-size:10px;letter-spacing:.4em;color:#39FF14;text-transform:uppercase;opacity:.6;margin-bottom:16px">The Championships</p>
    <h1 style="font-size:42px;font-weight:900;color:#fff;line-height:1;text-shadow:0 0 30px rgba(57,255,20,.2),0 0 60px rgba(57,255,20,.1)">WIMBLE<br/>DON</h1>
    <p style="font-size:13px;color:#39FF14;opacity:.5;margin-top:12px;letter-spacing:.2em">2 0 2 6</p>
  </div>
  <!-- Form -->
  <div style="position:relative;flex:1;padding:0 28px 60px">
    <div style="display:flex;gap:0;margin-bottom:28px">
      <div style="flex:1;padding-bottom:10px;text-align:center;font-size:13;font-weight:700;color:#39FF14;border-bottom:2px solid #39FF14;text-shadow:0 0 10px rgba(57,255,20,.3);cursor:pointer;letter-spacing:.05em">INGRESAR</div>
      <div style="flex:1;padding-bottom:10px;text-align:center;font-size:13;font-weight:500;color:#333;cursor:pointer;letter-spacing:.05em">REGISTRARSE</div>
    </div>
    <div style="margin-bottom:24px">
      <input type="email" placeholder="tu@email.com" style="width:100%;padding:14px 16px;border:1px solid #1a1a1a;border-radius:8px;font-size:14;background:#0a0a0a;color:#39FF14;outline:none;box-shadow:0 0 0 transparent;transition:box-shadow .3s"/></div>
    <div style="margin-bottom:32px">
      <input type="password" placeholder="••••••••" value="••••••••" style="width:100%;padding:14px 16px;border:1px solid #1a1a1a;border-radius:8px;font-size:14;background:#0a0a0a;color:#39FF14;outline:none"/></div>
    <button style="width:100%;padding:16px;border:1px solid #39FF14;border-radius:8px;background:transparent;color:#39FF14;font-size:14;font-weight:700;cursor:pointer;letter-spacing:.1em;text-transform:uppercase;box-shadow:0 0 20px rgba(57,255,20,.15),inset 0 0 20px rgba(57,255,20,.05)">INGRESAR</button>
    <p style="text-align:center;font-size:10px;color:#222;margin-top:20px;letter-spacing:.1em">SW19 · NIGHT SESSION</p>
  </div>
</div>
"""

DESIGN_10 = """
<div style="min-height:932px;width:430px;display:flex;flex-direction:column;background:#1B5E20;position:relative;overflow:hidden">
  <!-- Vintage poster texture -->
  <div style="position:absolute;inset:0;opacity:.06;background:repeating-linear-gradient(0deg,transparent,transparent 1px,rgba(0,0,0,.3) 1px,rgba(0,0,0,.3) 2px)"></div>
  <!-- Red accent stripe -->
  <div style="height:6px;background:#C62828;width:100%"></div>
  <!-- Hero -->
  <div style="position:relative;padding:56px 24px 24px;text-align:center">
    <div style="font-size:11px;letter-spacing:.3em;color:rgba(255,255,255,.4);text-transform:uppercase">The All England Lawn Tennis & Croquet Club</div>
    <h1 style="font-family:Georgia,serif;font-size:52px;color:#fff;font-weight:900;line-height:.95;margin-top:16px;text-shadow:2px 2px 0 rgba(0,0,0,.2)">WIMBLE<br/>DON</h1>
    <div style="display:inline-block;margin-top:16px;padding:4px 20px;border:2px solid rgba(255,255,255,.3);border-radius:4px">
      <span style="font-family:Georgia,serif;font-size:28px;color:#FFF9C4;font-weight:700;letter-spacing:.05em">2026</span>
    </div>
    <p style="font-size:12px;color:rgba(255,255,255,.5);margin-top:12px;letter-spacing:.1em">PRONÓSTICOS · GENTLEMEN'S SINGLES</p>
  </div>
  <!-- Cream bottom -->
  <div style="position:relative;flex:1;background:#F5F0E1;border-radius:20px 20px 0 0;margin-top:16px;padding:28px 24px 48px">
    <div style="display:flex;border:2px solid #1B5E20;border-radius:8px;overflow:hidden;margin-bottom:24px">
      <div style="flex:1;padding:12px;text-align:center;font-size:13;font-weight:700;background:#1B5E20;color:#fff;cursor:pointer;font-family:Georgia,serif">Ingresar</div>
      <div style="flex:1;padding:12px;text-align:center;font-size:13;font-weight:500;color:#8A7A5A;cursor:pointer;font-family:Georgia,serif">Registrarse</div>
    </div>
    <div style="margin-bottom:16px"><label style="font-size:11px;font-weight:700;color:#5A5548;display:block;margin-bottom:6px;text-transform:uppercase;letter-spacing:.08em">Email</label>
      <input type="email" placeholder="tu@email.com" style="width:100%;padding:14px 16px;border:2px solid #D6CFC2;border-radius:8px;font-size:14;font-family:Georgia,serif;background:#FFFDF8;color:#333;outline:none"/></div>
    <div style="margin-bottom:24px"><label style="font-size:11px;font-weight:700;color:#5A5548;display:block;margin-bottom:6px;text-transform:uppercase;letter-spacing:.08em">Contraseña</label>
      <input type="password" placeholder="••••••••" value="••••••••" style="width:100%;padding:14px 16px;border:2px solid #D6CFC2;border-radius:8px;font-size:14;font-family:Georgia,serif;background:#FFFDF8;color:#333;outline:none"/></div>
    <button style="width:100%;padding:16px;border:2px solid #C62828;border-radius:8px;background:#C62828;color:#fff;font-size:14;font-weight:700;font-family:Georgia,serif;cursor:pointer;text-transform:uppercase;letter-spacing:.1em">Ingresar</button>
    <p style="text-align:center;font-size:10px;color:#A09882;margin-top:16px;font-family:Georgia,serif;font-style:italic">Si no tenés cuenta, registrate para participar del torneo.</p>
  </div>
</div>
"""

ALL_DESIGNS = [DESIGN_1, DESIGN_2, DESIGN_3, DESIGN_4, DESIGN_5, DESIGN_6, DESIGN_7, DESIGN_8, DESIGN_9, DESIGN_10]
NAMES = ["1-Club House", "2-Centre Court", "3-All England", "4-Dark Premium", "5-Grass Texture", "6-Minimal White", "7-Scoreboard", "8-Split Screen", "9-Neon Night", "10-Wimbledon Posters"]


async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch()
        page = await browser.new_page(viewport={"width": 430, "height": 932})

        for i, (design, name) in enumerate(zip(ALL_DESIGNS, NAMES)):
            html = DESIGNS + design + "</body></html>"
            out = f"/home/z/my-project/download/login-{name.replace(' ', '-').lower()}.png"
            await page.set_content(html, wait_until="networkidle")
            await page.screenshot(path=out, full_page=True)
            print(f"  {i+1}/10 {name} -> {out}")

        await browser.close()
        print("Done!")


asyncio.run(main())