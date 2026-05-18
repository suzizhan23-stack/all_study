@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

if "%1"=="" (set port=3000) else (set port=%1)

for /f "tokens=2 delims= " %%i in ('wsl ip addr show eth0 ^| findstr /r "inet [0-9]"') do set wsl_ip=%%i
for /f "tokens=1 delims=/" %%i in ("!wsl_ip!") do set wsl_ip=%%i

echo WSL IP: !wsl_ip!
echo 转发 Windows :!port! --^> WSL :!port!

netsh interface portproxy add v4tov4 listenaddress=0.0.0.0 listenport=!port! connectaddress=!wsl_ip! connectport=!port!
netsh advfirewall firewall add rule name="WSL Port !port!" dir=in protocol=tcp localport=!port! action=allow

echo.
echo 当前端口转发列表：
netsh interface portproxy show all
