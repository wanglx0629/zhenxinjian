// 食物图片批量下载 — 从 Bing 图片搜索按 foods_200.json 抓取 200 条内置食物配图
// 用法: node download.js [起 止]  （如 node download.js 0 50 先跑前 50 条验证）
// 产物: out/{code}.jpg + manifest.json（含来源与失败清单，供人工抽查与署名追溯）
// 作者: wanglx
const https = require('https');
const zlib = require('zlib');
const fs = require('fs');
const path = require('path');

const JSON_PATH = path.resolve(__dirname, '../../apps/zhenxinjian-backend/src/main/resources/foods_200.json');
const OUT_DIR = path.join(__dirname, 'out');

// 素材站/水印图库黑名单（图片直链或来源页命中即弃用）
const BLACKLIST = [
  'nipic', '699pic.', 'photophoto.', 'aitaotu.', 'vcg.com', 'tuchong.', 'gettyimages.',
  'shutterstock.', '123rf.', 'dreamstime.', 'alamy.', 'stockfood.', 'quanjing.',
  'thinkstock.', 'agefototock.', 'agefotostock.', 'picfair.', 'foodiesfeed.', 'gaoimg.',
  '58pic.', 'ibaotu.', '90sheji.', 'uitkit.', 'pikbest.', 'pig66.', 'tpyzq.',
  '9game.', 'zhihu.com' // 游戏站/需登录内容站
];
// 优先来源（百科/门户/菜谱站）
const PREFER = ['baike.baidu.com', 'wikipedia.org', 'xiachufang.com', 'douguo.com', 'meishij.net',
  'sohu.com', 'sinaimg', '163.com', 'qq.com', 'itc.cn', 'bcebos.com', 'ifeng.', 'chinanews.'];

const sleep = ms => new Promise(r => setTimeout(r, ms));
function hostOf(u) { try { return new URL(u).hostname; } catch (e) { return ''; } }
function blacklisted(u) { const l = (u || '').toLowerCase(); return BLACKLIST.some(b => l.includes(b)); }
function preferred(u) { const l = (u || '').toLowerCase(); return PREFER.some(p => l.includes(p)); }

function get(url, binary, depth = 0) {
  return new Promise((resolve, reject) => {
    if (depth > 3) return reject(new Error('too many redirects'));
    const req = https.get(url, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36',
        'Accept': binary ? 'image/*,*/*;q=0.8' : 'text/html,application/xhtml+xml',
        'Accept-Language': 'zh-CN,zh;q=0.9',
        'Accept-Encoding': 'gzip',
        'Referer': 'https://cn.bing.com/'
      }
    }, res => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        res.resume();
        return resolve(get(new URL(res.headers.location, url).toString(), binary, depth + 1));
      }
      if (res.statusCode !== 200) { res.resume(); return reject(new Error('HTTP ' + res.statusCode)); }
      const chunks = [];
      res.on('data', c => chunks.push(c));
      res.on('end', () => {
        let buf = Buffer.concat(chunks);
        if ((res.headers['content-encoding'] || '') === 'gzip') buf = zlib.gunzipSync(buf);
        resolve(buf);
      });
    });
    req.on('error', reject);
    req.setTimeout(15000, () => req.destroy(new Error('timeout')));
  });
}

function bumpThumb(turl) {
  if (!turl) return null;
  try {
    const u = new URL(turl);
    u.searchParams.set('w', '320');
    u.searchParams.set('h', '240');
    u.searchParams.set('c', '7');
    return u.toString();
  } catch (e) { return null; }
}

function candidates(html) {
  const out = [];
  const re = /m="({[^"]+})"/g;
  let m;
  while ((m = re.exec(html)) !== null) {
    try {
      const o = JSON.parse(m[1].replace(/&quot;/g, '"').replace(/&amp;/g, '&'));
      if (o.murl) out.push({ murl: o.murl, purl: o.purl || '', turl: o.turl || '', t: o.t || '' });
    } catch (e) { /* skip */ }
  }
  return out;
}

// 搜索词：去掉括注与多余空白（"大米（粳米，生）" → "大米"）
function searchTerm(name) {
  return name.replace(/（[^）]*）/g, '').replace(/\([^)]*\)/g, '').trim();
}

// 有效的图片魔数（jpeg/png/webp）
function isImage(buf) {
  if (buf.length < 12) return false;
  if (buf[0] === 0xFF && buf[1] === 0xD8) return 'jpg';
  if (buf[0] === 0x89 && buf[1] === 0x50 && buf[2] === 0x4E && buf[3] === 0x47) return 'png';
  if (buf.slice(0, 4).toString() === 'RIFF' && buf.slice(8, 12).toString() === 'WEBP') return 'webp';
  // Bing 缩略图可能返回渐进 jpeg 变体，宽松放行 jpg 头
  return null;
}

function score(c, term) {
  let s = 0;
  const t = (c.t || '').toLowerCase();
  if (term && t.includes(term.toLowerCase())) s += 2;
  if (preferred(c.murl) || preferred(c.purl)) s += 1;
  return s;
}

// fetchOne 返回值形态简化：直接返回 { meta, buf }
// 首搜（纯词）候选全为图库站或下载失败时，追加「菜谱」后缀二次搜索兜底
async function fetchFood(entry) {
  const term = searchTerm(entry.name);
  for (const q of [term, term + ' 菜谱']) {
    const searchUrl = 'https://cn.bing.com/images/search?q=' + encodeURIComponent(q)
      + '&form=HDRSC2&first=1&qft=+filterui:photo-photo&mkt=zh-CN';
    let cands;
    try {
      const html = (await get(searchUrl, false)).toString('utf8');
      cands = candidates(html).filter(c => !blacklisted(c.murl) && !blacklisted(c.purl));
    } catch (e) { cands = []; }
    const ranked = cands.map((c, i) => ({ c, i }))
      .sort((a, b) => score(b.c, term) - score(a.c, term) || a.i - b.i);
    for (const { c } of ranked.slice(0, 6)) {
      const thumb = bumpThumb(c.turl);
      if (!thumb) continue;
      try {
        const buf = await get(thumb, true);
        if (!isImage(buf)) throw new Error('not an image');
        if (buf.length < 3000) throw new Error('too small');
        return {
          meta: {
            ok: true, term, q, host: hostOf(c.murl), pageHost: hostOf(c.purl),
            murl: c.murl, purl: c.purl, bytes: buf.length
          },
          buf
        };
      } catch (e) { /* next */ }
      await sleep(250);
    }
    await sleep(400);
  }
  return { meta: { ok: false, term }, buf: null };
}

(async () => {
  const [startArg, endArg] = process.argv.slice(2);
  const start = parseInt(startArg || '0', 10);
  const end = parseInt(endArg || '200', 10);
  const root = JSON.parse(fs.readFileSync(JSON_PATH, 'utf8'));
  const foods = root.foods.slice(start, end);
  if (!fs.existsSync(OUT_DIR)) fs.mkdirSync(OUT_DIR, { recursive: true });

  // 断点续跑：已有 manifest 则读入
  const manifestPath = path.join(OUT_DIR, 'manifest.json');
  const manifest = fs.existsSync(manifestPath) ? JSON.parse(fs.readFileSync(manifestPath, 'utf8')) : { items: {}, misses: [] };
  if (!manifest.items) manifest.items = {};
  if (!manifest.misses) manifest.misses = [];

  let done = 0, okCount = 0;
  for (const f of foods) {
    done++;
    const key = f.id;
    if (manifest.items[key] && manifest.items[key].ok) { okCount++; continue; }
    try {
      const { meta, buf } = await fetchFood(f);
      if (meta.ok) {
        fs.writeFileSync(path.join(OUT_DIR, key + '.jpg'), buf);
        manifest.items[key] = Object.assign({ code: key, name: f.name }, meta);
        okCount++;
        manifest.misses = manifest.misses.filter(k => k !== key); // 续跑成功即摘出失败清单
      } else {
        manifest.items[key] = Object.assign({ code: key, name: f.name }, meta);
        if (!manifest.misses.includes(key)) manifest.misses.push(key);
      }
    } catch (e) {
      manifest.items[key] = { code: key, name: f.name, ok: false, term: searchTerm(f.name), error: e.message };
      if (!manifest.misses.includes(key)) manifest.misses.push(key);
    }
    if (done % 10 === 0) {
      fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));
      console.log('progress ' + (start + done) + '/' + root.total + ' ok=' + okCount);
    }
    await sleep(400 + Math.floor(Math.random() * 300));
  }
  fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));
  console.log('DONE range=' + start + '-' + end + ' ok=' + okCount + '/' + foods.length);
  console.log('misses=' + JSON.stringify(manifest.misses));
})();
