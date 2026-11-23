# =====================================================================
# Siembra datos de prueba en la red hospitalaria usando la API real.
# Se ejecuta CONTRA el gateway (8000), igual que haria el frontend, para
# que se respeten invariantes de dominio, outbox y proyecciones Kafka.
#
#   powershell -ExecutionPolicy Bypass -File .\semilla-datos.ps1
#   ... -Gateway http://localhost:8000 -Sufijo "demo"
#
# Es idempotente: si un paciente ya tiene admision ACTIVA se salta, y
# los correos de personal que ya existen se reutilizan.
# =====================================================================
param(
    [string]$Gateway = "http://localhost:8000",
    [string]$AdminEmail = "admin@hospital.com",
    [string]$AdminPass = "123456789",
    [string]$Sufijo = "demo",
    [int]$EsperaKafkaSeg = 8
)

$ErrorActionPreference = "Stop"
$script:Ok = 0
$script:Omitido = 0
$script:Fallo = 0

function ok($msg)     { $script:Ok++;      Write-Host "  [OK]     $msg" -ForegroundColor Green }
function omitir($msg) { $script:Omitido++; Write-Host "  [OMITE]  $msg" -ForegroundColor Yellow }
function fallo($msg)  { $script:Fallo++;   Write-Host "  [FALLA]  $msg" -ForegroundColor Red }
function seccion($msg) { Write-Host "`n== $msg ==" -ForegroundColor Cyan }

# Llama a la API y devuelve @{ Status; Body(objeto|null) }.
# Compatible con Windows PowerShell 5.1, que NO tiene -SkipHttpErrorCheck:
# en 4xx/5xx Invoke-WebRequest lanza excepcion y leemos el codigo/cuerpo del error.
function Api {
    param(
        [string]$Metodo, [string]$Ruta,
        [string]$Token = $null, $Cuerpo = $null
    )
    $headers = @{}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    $iwrArgs = @{
        Uri = "$Gateway$Ruta"
        Method = $Metodo
        Headers = $headers
        UseBasicParsing = $true
        TimeoutSec = 30
    }
    if ($null -ne $Cuerpo) {
        $iwrArgs["Body"] = ($Cuerpo | ConvertTo-Json -Depth 8)
        $iwrArgs["ContentType"] = "application/json"
    }
    try {
        $resp = Invoke-WebRequest @iwrArgs
        $status = [int]$resp.StatusCode
        $content = $resp.Content
    } catch {
        $ex = $_.Exception
        if ($ex.Response) {
            $status = [int]$ex.Response.StatusCode
            $stream = $ex.Response.GetResponseStream()
            if ($stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $content = $reader.ReadToEnd()
            } else { $content = $null }
        } else {
            return @{ Status = 0; Body = $null; Error = $ex.Message }
        }
    }
    $obj = $null
    if ($content) { try { $obj = $content | ConvertFrom-Json } catch { } }
    return @{ Status = $status; Body = $obj }
}

function Login($email, $pass) {
    $r = Api "POST" "/api/auth/login" $null @{ email = $email; password = $pass }
    if ($r.Status -ne 200) { return $null }
    return $r.Body.token
}

function AsegurarUsuario {
    param([string]$AdminToken, [string]$Email, [string]$Rol, [string]$Pass)
    $r = Api "POST" "/api/auth/usuarios" $AdminToken @{ email = $Email; password = $Pass; rol = $Rol }
    if ($r.Status -eq 201) { ok "Usuario $Rol creado: $Email" }
    elseif ($r.Status -eq 409) { omitir "Usuario $Rol ya existe: $Email" }
    else { fallo "No se pudo crear $Rol ($Email): $($r.Status) $($r.Body | ConvertTo-Json -Compress)" }
}

# ---------------------------------------------------------------- inicio
Write-Host "Sembrando datos en $Gateway (sufijo: $Sufijo)" -ForegroundColor White

seccion "0. Espera de servicios"
$servicios = [ordered]@{
    gateway     = "http://localhost:8000/api/status"
    auth        = "http://localhost:8085/api/status"
    admisiones  = "http://localhost:8080/api/status"
    farmacia    = "http://localhost:8081/api/status"
    laboratorio = "http://localhost:8082/api/status"
    consultas   = "http://localhost:8083/api/status"
}
# gateway y auth responden 401 en /api/status (requieren token): eso tambien
# significa que el proceso esta arriba, asi que 401/403 cuentan como "listo".
function Probar-Uri($u) {
    try {
        $r = Invoke-WebRequest -Uri $u -UseBasicParsing -TimeoutSec 5
        return [int]$r.StatusCode
    } catch {
        if ($_.Exception.Response) { return [int]$_.Exception.Response.StatusCode }
        return 0
    }
}
foreach ($k in $servicios.Keys) {
    $listo = $false
    for ($i = 0; $i -lt 30 -and -not $listo; $i++) {
        $code = Probar-Uri $servicios[$k]
        if ($code -ge 200 -and $code -lt 500) { $listo = $true } else { Start-Sleep -Seconds 2 }
    }
    if ($listo) { ok "$k responde" } else { fallo "$k no respondio" }
}

# ------------------------------------------------------------- personal
seccion "1. Cuentas de personal (auth)"
$admin = Login $AdminEmail $AdminPass
if (-not $admin) { fallo "No se pudo iniciar sesion como admin"; exit 1 }
ok "Admin autenticado"

$clave = "Clave12345"
$personas = [ordered]@{
    "admisiones.$Sufijo@hospital.test"    = "ADMISIONES"
    "medico.$Sufijo@hospital.test"        = "MEDICO"
    "laboratorio.$Sufijo@hospital.test"   = "LABORATORIO"
    "farmaceutico.$Sufijo@hospital.test"  = "FARMACEUTICO"
}
foreach ($email in $personas.Keys) {
    AsegurarUsuario $admin $email $personas[$email] $clave
}

$tokAdm    = Login "admisiones.$Sufijo@hospital.test" $clave
$tokMed    = Login "medico.$Sufijo@hospital.test" $clave
$tokLab    = Login "laboratorio.$Sufijo@hospital.test" $clave
$tokFarm   = Login "farmaceutico.$Sufijo@hospital.test" $clave
if ($tokAdm -and $tokMed -and $tokLab -and $tokFarm) { ok "Tokens de personal obtenidos" }
else { fallo "Faltan tokens de personal"; exit 1 }

# ------------------------------------------------------------- inventario
seccion "2. Inventario de camas (admisiones, admin)"
$sedes = [ordered]@{
    "Sede-Central"  = @{ URGENCIAS = 5; UCI = 2; CIRUGIA = 3 }
    "Sede-Norte"    = @{ URGENCIAS = 4; UCI = 1; CIRUGIA = 2 }
    "Sede-Sur"      = @{ URGENCIAS = 3; UCI = 1; CIRUGIA = 2 }
}
foreach ($sede in $sedes.Keys) {
    foreach ($serv in $sedes[$sede].Keys) {
        $tot = $sedes[$sede][$serv]
        $r = Api "POST" "/api/admisiones/inventario-camas" $admin @{ sedeId = $sede; servicio = $serv; camasTotales = $tot }
        if ($r.Status -eq 201) { ok "$sede/$serv = $tot camas" }
        else { fallo "Inventario $sede/${serv}: $($r.Status)" }
    }
}

# -------------------------------------------------------------- pacientes
seccion "3. Admisiones de pacientes (flujo completo)"
$pacientes = @(
    @{ id = "PAC-$Sufijo-001"; sede = "Sede-Central"; serv = "URGENCIAS"; medico = "Dr. House";    motivo = "Dolor toracico agudo";    trasladar = $true;  alta = $false },
    @{ id = "PAC-$Sufijo-002"; sede = "Sede-Central"; serv = "URGENCIAS"; medico = "Dra. Grey";    motivo = "Trauma craneoencefalico"; trasladar = $false; alta = $true  },
    @{ id = "PAC-$Sufijo-003"; sede = "Sede-Norte";   serv = "URGENCIAS"; medico = "Dr. Wilson";  motivo = "Insuficiencia respiratoria"; trasladar = $false; alta = $false },
    @{ id = "PAC-$Sufijo-004"; sede = "Sede-Sur";     serv = "CIRUGIA";   medico = "Dra. Yang";    motivo = "Apendicitis";             trasladar = $false; alta = $false }
)

$admisionesCreadas = @()
foreach ($p in $pacientes) {
    # Si ya existe una admision ACTIVA para el paciente, no reingresar.
    $existentes = Api "GET" "/api/admisiones" $tokMed
    $activa = $null
    if ($existentes.Status -eq 200) {
        $activa = $existentes.Body | Where-Object { $_.pacienteId -eq $p.id -and $_.estado -eq "ACTIVA" } | Select-Object -First 1
    }
    if ($activa) { omitir "$($p.id) ya tiene admision ACTIVA"; $admisionesCreadas += $activa; continue }

    $sedeEnc = [uri]::EscapeDataString($p.sede)
    $servEnc = [uri]::EscapeDataString($p.serv)
    $medEnc  = [uri]::EscapeDataString($p.medico)
    $motEnc  = [uri]::EscapeDataString($p.motivo)
    $ruta = "/api/admisiones/ingreso?pacienteId=$($p.id)&sedeId=$sedeEnc&servicio=$servEnc&medicoTratante=$medEnc&motivoIngreso=$motEnc"
    $r = Api "POST" $ruta $tokAdm
    if ($r.Status -ne 201) { fallo "Ingreso $($p.id): $($r.Status) $($r.Body | ConvertTo-Json -Compress)"; continue }
    $adm = $r.Body
    ok "Ingreso $($p.id) en $($p.sede)/$($p.serv) -> $($adm.id)"
    $admisionesCreadas += $adm

    # HU-07: verificar cobertura (stub = 80%)
    $c = Api "POST" "/api/admisiones/$($adm.id)/verificar-cobertura" $tokAdm
    if ($c.Status -eq 200) { ok "Cobertura $($p.id) = $($c.Body.coberturaPorcentaje)%" }
    else { fallo "Cobertura $($p.id): $($c.Status)" }

    # HU-03: traslado (medico)
    if ($p.trasladar) {
        $destinoSede = "Sede-Norte"
        $destinoServ = "UCI"
        $t = Api "PUT" "/api/admisiones/$($adm.id)/traslado?sedeDestinoId=$([uri]::EscapeDataString($destinoSede))&servicioDestino=$([uri]::EscapeDataString($destinoServ))" $tokMed
        if ($t.Status -eq 204) { ok "Traslado $($p.id) -> $destinoSede/$destinoServ" }
        else { fallo "Traslado $($p.id): $($t.Status) $($t.Body | ConvertTo-Json -Compress)" }
    }

    # HU-09: alta medica (medico) — solo si no hay ordenes de laboratorio pendientes
    if ($p.alta) {
        $a = Api "PUT" "/api/admisiones/$($adm.id)/alta" $tokMed
        if ($a.Status -eq 204) { ok "Alta $($p.id) registrada" }
        elseif ($a.Status -eq 503) { omitir "Alta $($p.id): laboratorio-service no respondio (gRPC 503)" }
        elseif ($a.Status -eq 409) { omitir "Alta $($p.id): paciente con ordenes de laboratorio pendientes" }
        else { fallo "Alta $($p.id): $($a.Status) $($a.Body | ConvertTo-Json -Compress)" }
    }
}

# ------------------------------------------------------------ laboratorio
seccion "4. Ordenes de laboratorio (laboratorio-service)"
if ($admisionesCreadas.Count -gt 0) {
    $pacLab = $admisionesCreadas[0].pacienteId
    $ordenes = @(
        @{ tipo = "Hemograma completo"; valor = "7.5"; unidad = "x10^3/uL"; cerrar = $true },
        @{ tipo = "Glucosa en ayunas";  valor = "95";  unidad = "mg/dL";    cerrar = $false }
    )
    foreach ($o in $ordenes) {
        $r = Api "POST" "/api/laboratorio/ordenes" $tokMed @{ pacienteId = $pacLab; tipoExamen = $o.tipo }
        if ($r.Status -eq 201) {
            ok "Orden '$($o.tipo)' creada para $pacLab -> $($r.Body.id)"
            $oid = $r.Body.id
            $res = Api "POST" "/api/laboratorio/ordenes/$oid/resultado" $tokLab @{ valor = $o.valor; unidad = $o.unidad }
            if ($res.Status -eq 200) { ok "Resultado registrado ($($o.valor) $($o.unidad))" }
            else { fallo "Resultado '$($o.tipo)': $($res.Status)" }
            if ($o.cerrar) {
                $ci = Api "PUT" "/api/laboratorio/ordenes/$oid/cierre" $tokLab
                if ($ci.Status -eq 204) { ok "Orden '$($o.tipo)' cerrada" }
                else { fallo "Cierre '$($o.tipo)': $($ci.Status)" }
            }
        }
        elseif ($r.Status -eq 409) { omitir "Orden '$($o.tipo)': ya existe una igual para el paciente" }
        else { fallo "Orden '$($o.tipo)': $($r.Status) $($r.Body | ConvertTo-Json -Compress)" }
    }
}

# --------------------------------------------------------------- farmacia
seccion "5. Solicitudes de farmacia (farmacia-service)"
$solicitudes = @(
    @{ pac = "PAC-$Sufijo-001"; med = "MEDICO-001"; medicamento = "Paracetamol 500mg"; despachar = $true },
    @{ pac = "PAC-$Sufijo-002"; med = "MEDICO-001"; medicamento = "Ibuprofeno 400mg";  despachar = $false }
)
foreach ($s in $solicitudes) {
    $r = Api "POST" "/api/farmacia/solicitudes" $tokFarm @{ pacienteId = $s.pac; medicoId = $s.med; medicamento = $s.medicamento }
    if ($r.Status -eq 201) {
        ok "Solicitud '$($s.medicamento)' creada -> $($r.Body.id)"
        $sid = $r.Body.id
        $cov = Api "POST" "/api/farmacia/solicitudes/$sid/verificar-cobertura" $tokFarm
        if ($cov.Status -eq 200) { ok "Cobertura '$($s.medicamento)' = $($cov.Body.cobertura)%" }
        else { fallo "Cobertura '$($s.medicamento)': $($cov.Status)" }
        if ($s.despachar) {
            $d = Api "POST" "/api/farmacia/solicitudes/$sid/despachar" $tokFarm
            if ($d.Status -eq 200) { ok "Medicamento '$($s.medicamento)' despachado" }
            else { fallo "Despacho '$($s.medicamento)': $($d.Status)" }
        }
    }
    else { fallo "Solicitud '$($s.medicamento)': $($r.Status) $($r.Body | ConvertTo-Json -Compress)" }
}

# -------------------------------------------------------------- consultas
seccion "6. Proyeccion CQRS en Mongo (consistencia eventual via Kafka)"
Write-Host "  Esperando $EsperaKafkaSeg s a que lleguen los eventos..." -ForegroundColor DarkGray
Start-Sleep -Seconds $EsperaKafkaSeg
foreach ($adm in $admisionesCreadas) {
    $r = Api "GET" "/api/consultas/admisiones/paciente/$($adm.pacienteId)" $tokMed
    if ($r.Status -eq 200) { ok "Consulta $($adm.pacienteId): $($r.Body.Count) ficha(s) en Mongo" }
    else { fallo "Consulta $($adm.pacienteId): $($r.Status)" }
}

# ---------------------------------------------------------------- resumen
Write-Host "`nRESULTADO: $script:Ok OK | $script:Omitido omitidos | $script:Fallo fallas" -ForegroundColor White
if ($script:Fallo -gt 0) { exit 1 }
