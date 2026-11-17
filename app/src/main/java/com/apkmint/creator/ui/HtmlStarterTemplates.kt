package com.apkmint.creator.ui

data class StarterTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val defaultAppName: String,
    val defaultPackage: String,
    val html: String
)

object HtmlStarterTemplates {
    val all = listOf(
        StarterTemplate(
            id = "portfolio",
            title = "Bio / Portfolio",
            subtitle = "Personal link card",
            defaultAppName = "My Portfolio",
            defaultPackage = "com.apkmint.portfolio",
            html = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>My Portfolio</title>
  <style>
    * { box-sizing: border-box; font-family: system-ui, sans-serif; }
    body { margin: 0; min-height: 100vh; background: #0f172a; color: #f8fafc; display: flex; justify-content: center; padding: 24px 16px; }
    .wrap { width: 100%; max-width: 420px; text-align: center; }
    .avatar { width: 92px; height: 92px; border-radius: 50%; background: linear-gradient(135deg, #0d9488, #3b82f6); margin: 12px auto; display: flex; align-items: center; justify-content: center; font-size: 36px; font-weight: 700; }
    h1 { margin: 8px 0 4px; font-size: 22px; }
    p { margin: 0 0 24px; color: #94a3b8; font-size: 14px; }
    .link { display: block; padding: 14px 18px; margin-bottom: 12px; border-radius: 14px; background: #1e293b; color: #fff; text-decoration: none; font-weight: 600; border: 1px solid #334155; }
    .link:active { transform: scale(0.98); background: #0d9488; }
  </style>
</head>
<body>
  <div class="wrap">
    <div class="avatar">A</div>
    <h1>Creative Developer</h1>
    <p>Android • Web • UI/UX Designer</p>
    <a class="link" href="https://github.com">GitHub Projects</a>
    <a class="link" href="https://youtube.com">YouTube Channel</a>
    <a class="link" href="mailto:hello@example.com">Contact via Email</a>
  </div>
</body>
</html>"""
        ),
        StarterTemplate(
            id = "calculator",
            title = "Calculator",
            subtitle = "Offline utility app",
            defaultAppName = "Mint Calc",
            defaultPackage = "com.apkmint.calculator",
            html = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Mint Calc</title>
  <style>
    * { box-sizing: border-box; font-family: system-ui, sans-serif; }
    body { margin: 0; background: #111827; color: #fff; display: flex; flex-direction: column; height: 100vh; padding: 16px; }
    .screen { flex: 1; display: flex; align-items: flex-end; justify-content: flex-end; font-size: 46px; font-weight: 600; padding: 16px; word-break: break-all; }
    .grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
    button { height: 64px; border: none; border-radius: 18px; font-size: 22px; font-weight: 600; background: #1f2937; color: #fff; }
    button.op { background: #0d9488; }
    button.clear { background: #dc2626; }
    button:active { opacity: 0.8; }
  </style>
</head>
<body>
  <div class="screen" id="disp">0</div>
  <div class="grid">
    <button class="clear" onclick="clr()">C</button>
    <button onclick="add('(')">(</button>
    <button onclick="add(')')">)</button>
    <button class="op" onclick="add('/')">÷</button>
    <button onclick="add('7')">7</button><button onclick="add('8')">8</button><button onclick="add('9')">9</button><button class="op" onclick="add('*')">×</button>
    <button onclick="add('4')">4</button><button onclick="add('5')">5</button><button onclick="add('6')">6</button><button class="op" onclick="add('-')">−</button>
    <button onclick="add('1')">1</button><button onclick="add('2')">2</button><button onclick="add('3')">3</button><button class="op" onclick="add('+')">+</button>
    <button onclick="add('0')" style="grid-column: span 2;">0</button><button onclick="add('.')">.</button><button class="op" onclick="calc()">=</button>
  </div>
  <script>
    var d = document.getElementById('disp');
    function add(v){ d.innerText = d.innerText === '0' ? v : d.innerText + v; }
    function clr(){ d.innerText = '0'; }
    function calc(){ try { d.innerText = eval(d.innerText); } catch(e) { d.innerText = 'Error'; } }
  </script>
</body>
</html>"""
        ),
        StarterTemplate(
            id = "quiz",
            title = "Quiz App",
            subtitle = "Interactive Q&A",
            defaultAppName = "Quick Quiz",
            defaultPackage = "com.apkmint.quickquiz",
            html = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Quick Quiz</title>
  <style>
    * { box-sizing: border-box; font-family: system-ui, sans-serif; }
    body { margin: 0; background: #f1f5f9; color: #0f172a; padding: 20px; }
    .card { background: #fff; border-radius: 20px; padding: 22px; box-shadow: 0 8px 24px rgba(0,0,0,0.06); max-width: 440px; margin: 20px auto; }
    .badge { display: inline-block; background: #ccfbf1; color: #0f766e; padding: 4px 12px; border-radius: 99px; font-size: 12px; font-weight: 700; }
    h2 { font-size: 20px; margin: 14px 0 18px; }
    .opt { width: 100%; text-align: left; padding: 14px 16px; margin-bottom: 10px; border: 1px solid #cbd5e1; border-radius: 12px; background: #fff; font-size: 15px; font-weight: 500; }
    .opt:active { background: #0d9488; color: #fff; border-color: #0d9488; }
  </style>
</head>
<body>
  <div class="card" id="box"></div>
  <script>
    const qs = [
      { q: "Which language runs natively in a WebView?", a: ["HTML/CSS/JS", "C++ only", "Swift"], c: 0 },
      { q: "Which file holds Android app permissions?", a: ["strings.xml", "AndroidManifest.xml", "styles.xml"], c: 1 }
    ];
    let i = 0, score = 0;
    function render() {
      const box = document.getElementById('box');
      if (i >= qs.length) {
        box.innerHTML = `<span class="badge">FINISHED</span><h2>Your Score: ${'$'}{score}/${'$'}{qs.length}</h2><button class="opt" onclick="i=0;score=0;render()">Play Again</button>`;
        return;
      }
      const cur = qs[i];
      box.innerHTML = `<span class="badge">Question ${'$'}{i+1}/${'$'}{qs.length}</span><h2>${'$'}{cur.q}</h2>` +
        cur.a.map((t, idx) => `<button class="opt" onclick="ans(${'$'}{idx})">${'$'}{t}</button>`).join('');
    }
    function ans(idx) { if (idx === qs[i].c) score++; i++; render(); }
    render();
  </script>
</body>
</html>"""
        )
    )
}
