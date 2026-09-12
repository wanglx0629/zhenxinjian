# 从单一真源 .trae/ 重新生成各 IDE skills/commands 副本目录
# 真源：.trae/skills/、.trae/commands/（唯一入库口径，ARCH-内聚-003 治理）
# 用法：powershell -NoProfile -ExecutionPolicy Bypass -File scripts/sync-ide-skills.ps1
# 作者: wanglx
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
Set-Location $repo

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Copy-Skill($name, $targetRoot) {
  $src = Join-Path $repo ".trae\skills\$name"
  $dst = Join-Path $repo "$targetRoot\skills\$name"
  if (Test-Path $dst) { Remove-Item $dst -Recurse -Force }
  Copy-Item $src $dst -Recurse -Force
  Write-Output "copy  $name -> $targetRoot"
}

# openspec 调用占位符反向映射：/opsx-<short> -> $openspec-<long> (Codex) or /openspec-<long> (other agents)
$opsxMap = [ordered]@{
  'apply'    = 'apply-change'
  'archive'  = 'archive-change'
  'continue' = 'continue-change'
  'explore'  = 'explore'
  'new'      = 'new-change'
  'propose'  = 'propose'
  'sync'     = 'sync-specs'
  'update'   = 'update-change'
  'verify'   = 'verify-change'
}

function Convert-ToAgents($text) {
  foreach ($k in $opsxMap.Keys) {
    $long = $opsxMap[$k]
    $pattern = [regex]::Escape("/opsx-$k")
    $replacement = '$$openspec-' + $long + ' (Codex) or /openspec-' + $long + ' (other agents)'
    $text = $text -replace $pattern, $replacement
  }
  return $text
}

$khufu    = @('khufu-api','khufu-e2e','khufu-harness','khufu-it','khufu-ut')
$openmole = @('openmole-apply','openmole-archive','openmole-explore','openmole-plan','openmole-verify')
$openspec = @('openspec-apply-change','openspec-archive-change','openspec-explore','openspec-propose','openspec-sync-specs','openspec-update-change','openspec-verify-change')
$trio     = @('frontend-design','mcp-builder','miniprogram-ui-ux-design','webapp-testing')
$opsxCmds = @('opsx-apply','opsx-archive','opsx-explore','opsx-propose','opsx-sync','opsx-update','opsx-verify')

# 1. .claude/skills：khufu 五件套（纯拷贝）
foreach ($s in $khufu) { Copy-Skill $s '.claude' }

# 2. .codex/skills：khufu + openmole + 三方共享（纯拷贝）
foreach ($s in ($khufu + $openmole + $trio)) { Copy-Skill $s '.codex' }

# 3. .agent/skills：ui-ux-pro-max（纯拷贝）
Copy-Skill 'ui-ux-pro-max' '.agent'

# 4. .opencode/skills：openspec + 三方共享（纯拷贝）
foreach ($s in ($openspec + $trio)) { Copy-Skill $s '.opencode' }

# 5. .opencode/commands：khufu 以 commands/<name>/SKILL.md 布局落地（纯拷贝）
$ocCmd = Join-Path $repo '.opencode\commands'
foreach ($s in $khufu) {
  $dst = Join-Path $ocCmd "$s\SKILL.md"
  if (Test-Path (Split-Path $dst)) { Remove-Item (Split-Path $dst) -Recurse -Force }
  New-Item -ItemType Directory -Path (Split-Path $dst) -Force | Out-Null
  Copy-Item (Join-Path $repo ".trae\skills\$s\SKILL.md") $dst -Force
  Write-Output "copy  $s -> .opencode/commands layout"
}

# 6. .opencode/commands/opsx-*.md：.trae/commands 副本剔除 front-matter name: 行
foreach ($c in $opsxCmds) {
  $raw = [System.IO.File]::ReadAllText((Join-Path $repo ".trae\commands\$c.md"))
  $stripped = $raw -replace '(?m)^name:[^\r\n]*\r?\n', ''
  [System.IO.File]::WriteAllText((Join-Path $ocCmd "$c.md"), $stripped, $utf8NoBom)
  Write-Output "strip $c name: -> .opencode/commands"
}

# 7. .agents/skills：openspec 七件套（调用占位符反向映射）+ .openspec-target 标记
foreach ($s in $openspec) {
  $dst = Join-Path $repo ".agents\skills\$s\SKILL.md"
  if (Test-Path (Split-Path $dst)) { Remove-Item (Split-Path $dst) -Recurse -Force }
  New-Item -ItemType Directory -Path (Split-Path $dst) -Force | Out-Null
  $raw = [System.IO.File]::ReadAllText((Join-Path $repo ".trae\skills\$s\SKILL.md"))
  [System.IO.File]::WriteAllText($dst, (Convert-ToAgents $raw), $utf8NoBom)
  Write-Output "xform $s -> .agents"
}
[System.IO.File]::WriteAllText((Join-Path $repo '.agents\skills\.openspec-target'), "codex`r`n", $utf8NoBom)
Write-Output 'mark  .agents/skills/.openspec-target = codex'

Write-Output 'DONE: 5 IDE dirs regenerated from .trae/ SoT'
