param([int]$port = 3000)

$wsl_ip = (wsl -d Ubuntu ip addr show eth0 | Select-String -Pattern "inet\s+(\d+\.\d+\.\d+\.\d+)" | % { $_.Matches.Groups[1].Value })

Write-Host "WSL IP: $wsl_ip"
Write-Host "转发 Windows :$port → WSL :$port"

netsh interface portproxy add v4tov4 listenaddress=0.0.0.0 listenport=$port connectaddress=$wsl_ip connectport=$port
New-NetFirewallRule -DisplayName "WSL Port $port" -Direction Inbound -Protocol TCP -LocalPort $port -Action Allow

netsh interface portproxy show all
