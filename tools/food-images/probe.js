// Bing 图片搜索探测 v2 — 带素材站域名过滤 + Bing 缩略图 CDN 小尺寸下载
// 作者: wanglx
const https = require('https');
const zlib = require('zlib');
const fs = require('fs');
const path = require('path');

// 素材站/水印图库黑名单（murl 或来源页命中即跳过）
const BLACKLIST = [
  'nipic.', '699pic.', 'photophoto.', 'aitaotu.', 'vcg.com', 'tuchong.', 'gettyimages.',
  'shutterstock.', '123rf.', 'dreamstime.', 'alamy.', 'stockfood.', 'quanjing.',
  'thinkstock.', 'agefotostock.', 'photostock.', 'picfair.', 'foodiesfeed.', 'gaoimg.',
  '58pic.', 'ibaotu.', '90sheji.', 'uitkit.', 'pikbest.', 'pig66.', 'nipic', 'tpyzq.'
];
// 优先来源（菜谱/百科/公开站点）
const PREFER = ['wikipedia.org', 'wikimedia.org', 'xiachufang.com', 'douguo.com', 'meishij.net',
  'msn.com', 'sohu.com', '163.com', 'qq.com', 'sinaimg', 'hongxiu.', 'jiukouple.', 'douyin'];

function hostOf(u) { try { return new URL(u).hostname; } catch (e) { return ''; } }
function blacklisted(u) { const l = u.toLowerCase(); return BLACKLIST.some(b => l.includes(b)); }
function preferred(u) { const l = u.toLowerCase(); return PREFER.some(p => l.includes(p)); }

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

// Bing 缩略图 URL 升尺寸（tse CDN 国内可达、稳定、统一小图）
function bumpThumb(turl) {
  if (!turl) return null;
  const u = new URL(turl);
  u.searchParams.set('w', '320');
  u.searchParams.set('h', '240');
  u.searchParams.set('c', '7');
  return u.toString();
}

function candidates(html) {
  const out = [];
  const re = /m="({[^"]+})"/g;
  let m;
  while ((m = re.exec(html)) !== null) {
    try {
      const o = JSON.parse(m[1].replace(/&quot;/g, '"').replace(/&amp;/g, '&'));
      if (o.murl) out.push({ murl: o.murl, purl: o.purl || '', turl: o.turl || '' });
    } catch (e) { /* skip */ }
  }
  return out;
}

async function probeFood(name) {
  const searchUrl = 'https://cn.bing.com/images/search?q=' + encodeURIComponent(name) + '&form=HDRSC2&first=1';
  const html = (await get(searchUrl, false)).toString('utf8');
  let cands = candidates(html);
  const total = cands.length;
  cands = cands.filter(c => !blacklisted(c.murl) && !blacklisted(c.purl));
  // 排序：偏好来源在前
  cands.sort((a, b) => (preferred(b.murl) || preferred(b.purl) ? 1 : 0) - (preferred(a.murl) || preferred(a.purl) ? 1 : 0));
  console.log('== ' + name + ' : total=' + total + ' afterFilter=' + cands.length);
  if (!cands.length) return false;
  for (const c of cands.slice(0, 5)) {
    const thumb = bumpThumb(c.turl);
    if (!thumb) continue;
    try {
      const buf = await get(thumb, true);
      if (buf.length < 3000) throw new Error('too small');
      const outDir = path.join(__dirname, 'probe_out');
      if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });
      const file = path.join(outDir, name.replace(/[\\/:*?"<>|]/g, '_') + '.jpg');
      fs.writeFileSync(file, buf);
      console.log('   SAVED ' + buf.length + 'B host=' + hostOf(c.murl) + ' page=' + hostOf(c.purl));
      return true;
    } catch (e) { console.log('   skip: ' + e.message); }
  }
  return false;
}

(async () => {
  for (const n of process.argv.slice(2)) {
    try { await probeFood(n); } catch (e) { console.log('== ' + n + ' FAILED: ' + e.message); }
  }
})();
