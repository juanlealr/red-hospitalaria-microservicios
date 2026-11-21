#!/usr/bin/env bash
# =====================================================================
# Pruebas de API: AUTENTICACIÓN + ADMISIONES + COBERTURA + CONSULTAS CQRS
# Requisitos: bash, curl, jq.
# Servicios arriba: eureka (8761), gateway (8000), auth (8085),
# admisiones (8080), consultas-admisiones (8083), Postgres, Mongo y Kafka.
#
# Uso:   bash pruebas_auth_admisiones.sh
# Vars:  GW, ADM (admisiones directo), ADMIN_EMAIL, ADMIN_PASS
# Cada ejecución usa datos con sufijo único, así que se puede repetir.
# =====================================================================
set -u
GW="${GW:-http://localhost:8000}"
ADM="${ADM:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@hospital.com}"
ADMIN_PASS="${ADMIN_PASS:-123456789}"
SUF="$(date +%s)"

command -v jq >/dev/null || { echo "Falta jq (instálalo y reintenta)"; exit 1; }
command -v curl >/dev/null || { echo "Falta curl"; exit 1; }

PASS=0; FAIL=0; SKIP=0
BODY="$(mktemp)"; TMP="$(mktemp -d)"
trap 'rm -rf "$BODY" "$TMP"' EXIT

# ---------- utilidades ----------
req() { # método ruta [token] [json]
  local metodo="$1" ruta="$2" token="${3:-}" cuerpo="${4:-}"
  local args=(-s -o "$BODY" -w '%{http_code}' -X "$metodo" "$GW$ruta")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  if [ -n "$cuerpo" ]; then args+=(-H 'Content-Type: application/json' --data "$cuerpo"); fi
  curl "${args[@]}"
}
ok() { # descripción esperado obtenido
  if [ "$2" = "$3" ]; then
    PASS=$((PASS+1)); printf '  \033[32mOK   \033[0m %s\n' "$1"
  else
    FAIL=$((FAIL+1)); printf '  \033[31mFALLA\033[0m %s  (esperado %s, obtuvo %s)  %s\n' "$1" "$2" "$3" "$(head -c 160 "$BODY")"
  fi
}
skip() { SKIP=$((SKIP+1)); printf '  \033[33mSKIP \033[0m %s\n' "$1"; }
t() { # descripción esperado método ruta [token] [json]
  local desc="$1" esp="$2"; shift 2
  local code; code="$(req "$@")"
  ok "$desc" "$esp" "$code"
}
seccion() { printf '\n\033[1m== %s ==\033[0m\n' "$1"; }
jb() { jq -r "$1" "$BODY"; }
tok_de() { local v="TOK_$1"; echo "${!v}"; }
b64url() { base64 | tr -d '=\n' | tr '+/' '-_'; }

ROLES="ADMISIONES MEDICO LABORATORIO FARMACEUTICO ADMINISTRADOR"

# =====================================================================
seccion "A. AUTENTICACIÓN — login"
# =====================================================================
code="$(req POST /api/auth/login "" "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASS\"}")"
ok "A01 login admin semilla" 200 "$code"
TOK_ADMINISTRADOR_SEMILLA="$(jb .token)"
ok "A02 login devuelve rol ADMINISTRADOR" ADMINISTRADOR "$(jb .rol)"
ok "A03 login devuelve expiraEnSegundos > 0" true "$(jb '.expiraEnSegundos > 0')"
[ -z "$TOK_ADMINISTRADOR_SEMILLA" ] || [ "$TOK_ADMINISTRADOR_SEMILLA" = null ] && { echo "Sin token de admin: no se puede continuar"; exit 1; }
AD="$TOK_ADMINISTRADOR_SEMILLA"

t "A04 contraseña incorrecta -> 401" 401 POST /api/auth/login "" "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"incorrecta123\"}"
t "A05 correo inexistente -> 401" 401 POST /api/auth/login "" '{"email":"nadie@x.com","password":"12345678"}'
req POST /api/auth/login "" '{"email":"nadie@x.com","password":"12345678"}' >/dev/null; M1="$(jb .mensaje)"
req POST /api/auth/login "" "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"mala\"}" >/dev/null; M2="$(jb .mensaje)"
ok "A06 mismo mensaje para correo y clave malos (no filtra cuál falló)" "$M1" "$M2"
t "A07 cuerpo vacío {} -> 400" 400 POST /api/auth/login "" '{}'
t "A08 sin password -> 400" 400 POST /api/auth/login "" '{"email":"a@b.com"}'
t "A09 password en blanco -> 400" 400 POST /api/auth/login "" '{"email":"a@b.com","password":"   "}'
t "A10 JSON mal formado -> 400" 400 POST /api/auth/login "" '{"email": '
t "A11 inyección SQL en email -> 401 (no 500)" 401 POST /api/auth/login "" "{\"email\":\"' OR '1'='1\",\"password\":\"12345678\"}"

# =====================================================================
seccion "B. AUTENTICACIÓN — crear usuarios (solo ADMINISTRADOR)"
# =====================================================================
for rol in $ROLES; do
  mail="$(echo "$rol" | tr 'A-Z' 'a-z').$SUF@hospital.test"
  t "B01 admin crea usuario $rol -> 201" 201 POST /api/auth/usuarios "$AD" "{\"email\":\"$mail\",\"password\":\"Clave12345\",\"rol\":\"$rol\"}"
  ok "B02 respuesta trae el rol $rol" "$rol" "$(jb .rol)"
  ok "B03 respuesta NO expone password/hash" false "$(jb 'has("password") or has("passwordHash")')"
  code="$(req POST /api/auth/login "" "{\"email\":\"$mail\",\"password\":\"Clave12345\"}")"
  ok "B04 login de $rol -> 200" 200 "$code"
  ok "B05 token de $rol trae rol correcto" "$rol" "$(jb .rol)"
  printf -v "TOK_$rol" '%s' "$(jb .token)"
done
MAIL_DUP="medico.$SUF@hospital.test"
t "B06 correo duplicado -> 409" 409 POST /api/auth/usuarios "$AD" "{\"email\":\"$MAIL_DUP\",\"password\":\"Clave12345\",\"rol\":\"MEDICO\"}"
t "B07 correo duplicado con MAYÚSCULAS -> 409" 409 POST /api/auth/usuarios "$AD" "{\"email\":\"MEDICO.$SUF@HOSPITAL.TEST\",\"password\":\"Clave12345\",\"rol\":\"MEDICO\"}"
t "B08 clave de 7 caracteres -> 400" 400 POST /api/auth/usuarios "$AD" "{\"email\":\"c7.$SUF@h.test\",\"password\":\"1234567\",\"rol\":\"MEDICO\"}"
t "B09 clave de 8 caracteres (límite) -> 201" 201 POST /api/auth/usuarios "$AD" "{\"email\":\"c8.$SUF@h.test\",\"password\":\"12345678\",\"rol\":\"MEDICO\"}"
t "B10 correo con formato inválido -> 400" 400 POST /api/auth/usuarios "$AD" '{"email":"no-es-correo","password":"Clave12345","rol":"MEDICO"}'
t "B11 sin rol -> 400" 400 POST /api/auth/usuarios "$AD" "{\"email\":\"sr.$SUF@h.test\",\"password\":\"Clave12345\"}"
t "B12 rol inexistente -> 400" 400 POST /api/auth/usuarios "$AD" "{\"email\":\"ri.$SUF@h.test\",\"password\":\"Clave12345\",\"rol\":\"SUPERMAN\"}"
t "B13 sin password -> 400" 400 POST /api/auth/usuarios "$AD" "{\"email\":\"sp.$SUF@h.test\",\"rol\":\"MEDICO\"}"
t "B14 crear usuario SIN token -> 401" 401 POST /api/auth/usuarios "" "{\"email\":\"x.$SUF@h.test\",\"password\":\"Clave12345\",\"rol\":\"MEDICO\"}"
for rol in ADMISIONES MEDICO LABORATORIO FARMACEUTICO; do
  t "B15 $rol intenta crear usuario -> 403" 403 POST /api/auth/usuarios "$(tok_de $rol)" "{\"email\":\"y$rol.$SUF@h.test\",\"password\":\"Clave12345\",\"rol\":\"ADMINISTRADOR\"}"
done
t "B16 autoescalada: MEDICO no puede crearse un admin" 403 POST /api/auth/usuarios "$TOK_MEDICO" "{\"email\":\"esc.$SUF@h.test\",\"password\":\"Clave12345\",\"rol\":\"ADMINISTRADOR\"}"

# =====================================================================
seccion "C. AUTENTICACIÓN — validez del token"
# =====================================================================
t "C01 sin token -> 401" 401 GET /api/admisiones
t "C02 token basura -> 401" 401 GET /api/admisiones "esto.no.es.jwt"
H="$(echo "$AD" | cut -d. -f1)"; P="$(echo "$AD" | cut -d. -f2)"; S="$(echo "$AD" | cut -d. -f3)"
FALSO_P="$(printf '{"sub":"hacker@x.com","rol":"ADMINISTRADOR","exp":9999999999}' | b64url)"
t "C04 payload alterado con firma original -> 401" 401 GET /api/admisiones "$H.$FALSO_P.$S"
NONE_H="$(printf '{"alg":"none","typ":"JWT"}' | b64url)"
t "C05 token alg=none sin firma -> 401" 401 GET /api/admisiones "$NONE_H.$FALSO_P."
t "C06 firma truncada -> 401" 401 GET /api/admisiones "$H.$P.${S:0:10}"
code="$(curl -s -o "$BODY" -w '%{http_code}' "$ADM/api/admisiones")"
ok "C08 admisiones-service directo (8080) sin token -> 401" 401 "$code"
code="$(curl -s -o "$BODY" -w '%{http_code}' -H "Authorization: Bearer $H.$FALSO_P.$S" "$ADM/api/admisiones")"
ok "C09 admisiones-service directo con token falsificado -> 401" 401 "$code"
code="$(curl -s -o "$BODY" -w '%{http_code}' "$ADM/api/status")"
ok "C10 /api/status público en el servicio -> 200" 200 "$code"

# =====================================================================
seccion "D. AUTORIZACIÓN — matriz rol × endpoint (vía gateway)"
# =====================================================================
permiso() { # método ruta json rolesPermitidos(csv) etiqueta
  local m="$1" r="$2" b="$3" perm=",$4," et="$5" rol code res
  for rol in $ROLES; do
    code="$(req "$m" "$r" "$(tok_de $rol)" "$b")"
    if [[ "$perm" == *",$rol,"* ]]; then
      [ "$code" != 401 ] && [ "$code" != 403 ] && res=permitido || res="bloqueado($code)"
      ok "$et — $rol debe poder" permitido "$res"
    else
      ok "$et — $rol NO debe poder" 403 "$code"
    fi
  done
}
permiso POST "/api/admisiones/ingreso" "" "ADMISIONES" "D01 POST ingreso"
permiso POST "/api/admisiones/x/verificar-cobertura" "" "ADMISIONES" "D01b POST verificar-cobertura"
permiso GET  "/api/admisiones/cobertura/PAC-TEST" "" "ADMISIONES,ADMINISTRADOR" "D01c GET cobertura paciente"
permiso PUT  "/api/admisiones/x/traslado" "" "MEDICO" "D02 PUT traslado"
permiso PUT  "/api/admisiones/x/alta" "" "MEDICO" "D03 PUT alta"
permiso POST "/api/admisiones/inventario-camas" '{}' "ADMISIONES,ADMINISTRADOR" "D04 POST inventario-camas"
permiso GET  "/api/admisiones/inventario-camas/reporte-ocupacion?sedeId=NOEXISTE" "" "ADMINISTRADOR" "D05 GET reporte-ocupacion"
permiso GET  "/api/admisiones" "" "ADMISIONES,MEDICO,ADMINISTRADOR" "D06 GET admisiones"
permiso GET  "/api/admisiones/inventario-camas/todas" "" "ADMISIONES,MEDICO,ADMINISTRADOR" "D07 GET inventario todas"
permiso GET  "/api/consultas/admisiones" "" "ADMISIONES,MEDICO,ADMINISTRADOR" "D08 GET consultas admisiones"
permiso GET  "/api/consultas/admisiones/reporte/ocupacion?sedeId=NOEXISTE" "" "ADMINISTRADOR" "D09 GET consultas reporte"
permiso POST "/api/auth/usuarios" '{}' "ADMINISTRADOR" "D10 POST usuarios"
permiso POST "/api/farmacia/solicitudes" '{}' "MEDICO,FARMACEUTICO" "D11 POST farmacia solicitudes (HU-06)"

# =====================================================================
seccion "E. ADMISIONES — capacidad de camas (HU-02 / carga de inventario)"
# =====================================================================
S1="SEDE1_$SUF"; S2="SEDE2_$SUF"; S3="SEDE3_$SUF"
cap() { req POST /api/admisiones/inventario-camas "${3-$TOK_ADMISIONES}" "{\"sedeId\":\"$1\",\"servicio\":\"$2\",\"camasTotales\":$4}"; }
code="$(cap $S1 URGENCIAS "$TOK_ADMISIONES" 1)"; ok "E01 ADMISIONES registra S1/URGENCIAS=1 -> 201" 201 "$code"
code="$(cap $S1 UCI "$AD" 1)";                    ok "E02 ADMINISTRADOR registra S1/UCI=1 -> 201" 201 "$code"
code="$(cap $S2 URGENCIAS "$TOK_ADMISIONES" 2)";  ok "E03 S2/URGENCIAS=2 -> 201" 201 "$code"
code="$(cap $S3 URGENCIAS "$TOK_ADMISIONES" 1)";  ok "E04 S3/URGENCIAS=1 -> 201" 201 "$code"
code="$(cap $S3 UCI "$TOK_ADMISIONES" 2)";        ok "E05 S3/UCI=2 -> 201" 201 "$code"
code="$(cap $S1 CIRUGIA "$TOK_ADMISIONES" -1)";   ok "E06 capacidad negativa -> 400" 400 "$code"
code="$(req POST /api/admisiones/inventario-camas "$TOK_ADMISIONES" '{"sedeId":"","servicio":"X","camasTotales":1}')"; ok "E07 sede vacía -> 400" 400 "$code"
code="$(req POST /api/admisiones/inventario-camas "$TOK_ADMISIONES" '{"sedeId":"X","servicio":"","camasTotales":1}')"; ok "E08 servicio vacío -> 400" 400 "$code"
code="$(req POST /api/admisiones/inventario-camas "$TOK_ADMISIONES" '{"sedeId":"X","servicio":"Y","camasTotales":"abc"}')"; ok "E09 total no numérico -> 400" 400 "$code"
t "E11 listar todas (HU-02) -> 200" 200 GET /api/admisiones/inventario-camas/todas "$TOK_ADMISIONES"
ok "E12 la lista incluye las 2 filas de S1" 2 "$(jb "[.[]|select(.sedeId==\"$S1\")]|length")"
t "E13 disponibilidad por sede y servicio -> 200" 200 GET "/api/admisiones/inventario-camas?sedeId=$S1&servicio=URGENCIAS" "$TOK_MEDICO"
ok "E14 camasDisponibles = 1" 1 "$(jb .camasDisponibles)"
t "E15 por sede -> 200" 200 GET "/api/admisiones/inventario-camas/sede/$S1" "$TOK_ADMISIONES"
ok "E16 por sede devuelve 2 servicios" 2 "$(jb length)"
t "E17 sede/servicio inexistente -> 404" 404 GET "/api/admisiones/inventario-camas?sedeId=NOEXISTE&servicio=X" "$TOK_ADMISIONES"
t "E18 falta parámetro servicio -> 400" 400 GET "/api/admisiones/inventario-camas?sedeId=$S1" "$TOK_ADMISIONES"

# =====================================================================
seccion "F. ADMISIONES — HU-01 registrar ingreso y HU-07 cobertura"
# =====================================================================
PA="PAC${SUF}A"; PB="PAC${SUF}B"; PC="PAC${SUF}C"; PD="PAC${SUF}D"
ing() {
  local pac="$1" sede="$2" serv="$3"
  local tok="${4:-$TOK_ADMISIONES}"
  local med="${5:-Dr.%20House}"
  local mot="${6:-Dolor%20toracico%20agudo}"
  req POST "/api/admisiones/ingreso?pacienteId=$pac&sedeId=$sede&servicio=$serv&medicoTratante=$med&motivoIngreso=$mot" "$tok"
}
code="$(ing $PA $S1 URGENCIAS)"; ok "F01 ingreso paciente A en S1/URGENCIAS -> 201" 201 "$code"
ID_A="$(jb .id)"
ok "F02 estado ACTIVA" ACTIVA "$(jb .estado)"
ok "F03 sede y servicio correctos" "$S1/URGENCIAS" "$(jb '.sedeActualId + "/" + .servicioActual')"
ok "F03b medicoTratante guardado" "Dr. House" "$(jb .medicoTratante)"
ok "F03c motivoIngreso guardado" "Dolor toracico agudo" "$(jb .motivoIngreso)"
ok "F03d coberturaPorcentaje inicial es null" true "$(jb '.coberturaPorcentaje == null')"
ok "F04 sin traslados al inicio" 0 "$(jb '.traslados|length')"
t "F05 la cama quedó ocupada (disponibles=0)" 200 GET "/api/admisiones/inventario-camas?sedeId=$S1&servicio=URGENCIAS" "$TOK_ADMISIONES"
ok "F05b camasOcupadas = 1" 1 "$(jb .camasOcupadas)"

# HU-07 Verificación de cobertura por personal de admisiones
t "F05c consulta previa de cobertura paciente A (HU-07) -> 200" 200 GET "/api/admisiones/cobertura/$PA" "$TOK_ADMISIONES"
ok "F05d stub devuelve 80% de cobertura" 80 "$(jb .porcentajeCobertura)"
t "F05e asociar cobertura verificada a la admisión A -> 200" 200 POST "/api/admisiones/$ID_A/verificar-cobertura" "$TOK_ADMISIONES"
ok "F05f admisión A queda con coberturaPorcentaje = 80" 80 "$(jb .coberturaPorcentaje)"
t "F05g verificar cobertura en admisión inexistente -> 404" 404 POST "/api/admisiones/no-existe/verificar-cobertura" "$TOK_ADMISIONES"

code="$(ing $PB $S1 URGENCIAS)"; ok "F06 sin cama libre -> 409" 409 "$code"
code="$(ing $PA $S2 URGENCIAS)"; ok "F07 paciente con admisión ACTIVA no puede reingresar -> 409" 409 "$code"
code="$(ing $PB NOEXISTE URGENCIAS)"; ok "F08 sede sin inventario -> 404" 404 "$code"
code="$(ing $PB $S1 NOEXISTE)"; ok "F09 servicio sin inventario -> 404" 404 "$code"
code="$(ing "%20%20" $S2 URGENCIAS)"; ok "F10 paciente en blanco -> 400" 400 "$code"
code="$(req POST "/api/admisiones/ingreso?sedeId=$S2&servicio=URGENCIAS&medicoTratante=Dr.X&motivoIngreso=Test" "$TOK_ADMISIONES")"; ok "F11 falta pacienteId -> 400" 400 "$code"
code="$(req POST "/api/admisiones/ingreso?pacienteId=$PB&servicio=URGENCIAS&medicoTratante=Dr.X&motivoIngreso=Test" "$TOK_ADMISIONES")"; ok "F12 falta sedeId -> 400" 400 "$code"
code="$(req POST "/api/admisiones/ingreso?pacienteId=$PB&sedeId=$S2&servicio=URGENCIAS&motivoIngreso=Test" "$TOK_ADMISIONES")"; ok "F12b falta medicoTratante -> 400" 400 "$code"
code="$(req POST "/api/admisiones/ingreso?pacienteId=$PB&sedeId=$S2&servicio=URGENCIAS&medicoTratante=Dr.X" "$TOK_ADMISIONES")"; ok "F12c falta motivoIngreso -> 400" 400 "$code"
code="$(ing $PB $S2 URGENCIAS "$TOK_ADMISIONES" "%20%20" "Motivo")"; ok "F12d medicoTratante en blanco -> 400" 400 "$code"
code="$(ing $PB $S2 URGENCIAS "$TOK_MEDICO")"; ok "F13 MEDICO no puede registrar ingreso -> 403" 403 "$code"
code="$(ing $PB $S2 URGENCIAS "$AD")"; ok "F14 ADMINISTRADOR no puede registrar ingreso -> 403" 403 "$code"
t "F15 GET admisión por id -> 200" 200 GET "/api/admisiones/$ID_A" "$TOK_MEDICO"
t "F16 GET admisión inexistente -> 404" 404 GET "/api/admisiones/no-existe" "$TOK_MEDICO"
t "F17 listar admisiones -> 200" 200 GET /api/admisiones "$TOK_ADMISIONES"
ok "F18 la lista contiene la admisión A" true "$(jb "map(.id)|index(\"$ID_A\") != null")"
code="$(cap $S1 URGENCIAS "$TOK_ADMISIONES" 0)"; ok "F19 bajar capacidad por debajo de las ocupadas -> 400" 400 "$code"

# =====================================================================
seccion "G. ADMISIONES — HU-03 traslado"
# =====================================================================
tras() { req PUT "/api/admisiones/$1/traslado?sedeDestinoId=$2&servicioDestino=$3" "${4-$TOK_MEDICO}"; }
code="$(tras $ID_A $S2 URGENCIAS)"; ok "G01 traslado A: S1/URG -> S2/URG -> 204" 204 "$code"
req GET "/api/admisiones/inventario-camas?sedeId=$S1&servicio=URGENCIAS" "$TOK_ADMISIONES" >/dev/null
ok "G02 origen liberado (ocupadas=0)" 0 "$(jb .camasOcupadas)"
req GET "/api/admisiones/inventario-camas?sedeId=$S2&servicio=URGENCIAS" "$TOK_ADMISIONES" >/dev/null
ok "G03 destino ocupado (ocupadas=1)" 1 "$(jb .camasOcupadas)"
req GET "/api/admisiones/$ID_A" "$TOK_MEDICO" >/dev/null
ok "G04 historial conserva 1 traslado" 1 "$(jb '.traslados|length')"
ok "G05 sede actual = S2" "$S2" "$(jb .sedeActualId)"
ok "G06 el traslado guarda origen y destino (sede y servicio)" "$S1/URGENCIAS>$S2/URGENCIAS" "$(jb '.traslados[0].sedeOrigenId + "/" + .traslados[0].servicioOrigen + ">" + .traslados[0].sedeDestinoId + "/" + .traslados[0].servicioDestino')"
code="$(tras $ID_A $S2 URGENCIAS)"; ok "G07 trasladar al mismo sitio -> 400" 400 "$code"
code="$(ing $PC $S1 URGENCIAS)"; ok "G08 (prep) ingreso C en S1/URG (ya libre) -> 201" 201 "$code"; ID_C="$(jb .id)"
code="$(tras $ID_C $S1 UCI)"; ok "G09 traslado entre servicios de la misma sede -> 204" 204 "$code"
code="$(ing $PD $S1 URGENCIAS)"; ok "G10 (prep) ingreso D en S1/URG -> 201" 201 "$code"; ID_D="$(jb .id)"
code="$(tras $ID_D $S1 UCI)"; ok "G11 destino SIN cama (UCI llena) -> 409 [HU-03 criterio 1]" 409 "$code"
req GET "/api/admisiones/$ID_D" "$TOK_MEDICO" >/dev/null
ok "G12 el traslado rechazado NO altera la admisión (sigue en URGENCIAS, 0 traslados)" "URGENCIAS/0" "$(jb '.servicioActual + "/" + (.traslados|length|tostring)')"
code="$(tras $ID_D NOEXISTE UCI)"; ok "G13 destino sin inventario -> 404" 404 "$code"
code="$(tras no-existe $S2 URGENCIAS)"; ok "G14 admisión inexistente -> 404" 404 "$code"
code="$(req PUT "/api/admisiones/$ID_D/traslado?sedeDestinoId=$S2" "$TOK_MEDICO")"; ok "G15 falta servicioDestino -> 400" 400 "$code"
code="$(tras $ID_D $S2 URGENCIAS "$TOK_ADMISIONES")"; ok "G16 ADMISIONES no puede trasladar -> 403" 403 "$code"
code="$(tras $ID_D $S2 URGENCIAS "$AD")"; ok "G17 ADMINISTRADOR no puede trasladar -> 403" 403 "$code"
code="$(tras $ID_D $S2 URGENCIAS "")"; ok "G18 sin token -> 401" 401 "$code"

# =====================================================================
seccion "H. ADMISIONES — HU-10 reporte de ocupación (S1: 2 camas, ambas ocupadas)"
# =====================================================================
t "H01 reporte S1 como ADMINISTRADOR -> 200" 200 GET "/api/admisiones/inventario-camas/reporte-ocupacion?sedeId=$S1" "$AD"
ok "H02 camasTotales=2" 2 "$(jb .camasTotales)"
ok "H03 camasOcupadas=2" 2 "$(jb .camasOcupadas)"
ok "H04 camasLibres=0" 0 "$(jb .camasLibres)"
ok "H05 porcentajeOcupacion=100" true "$(jb '.porcentajeOcupacion == 100')"
ok "H06 detalle por servicio (2 filas)" 2 "$(jb '.servicios|length')"
t "H07 reporte S2 (1 de 2 ocupada)" 200 GET "/api/admisiones/inventario-camas/reporte-ocupacion?sedeId=$S2" "$AD"
ok "H08 porcentaje S2 = 50" true "$(jb '.porcentajeOcupacion == 50')"
t "H09 sede inexistente -> 404" 404 GET "/api/admisiones/inventario-camas/reporte-ocupacion?sedeId=NOEXISTE" "$AD"
t "H10 falta sedeId -> 400" 400 GET "/api/admisiones/inventario-camas/reporte-ocupacion" "$AD"
t "H11 ADMISIONES no ve el reporte -> 403" 403 GET "/api/admisiones/inventario-camas/reporte-ocupacion?sedeId=$S1" "$TOK_ADMISIONES"
code="$(cap $S1 URGENCIAS "$TOK_ADMISIONES" 3)"; ok "H12 subir capacidad S1/URG a 3 -> 201 (actualiza, no duplica)" 201 "$code"
req GET "/api/admisiones/inventario-camas/sede/$S1" "$TOK_ADMISIONES" >/dev/null
ok "H13 S1 sigue teniendo 2 filas" 2 "$(jb length)"

# =====================================================================
seccion "I. ADMISIONES — HU-09 alta médica (requiere mock o laboratorio-service en 9091)"
# =====================================================================
alta() { req PUT "/api/admisiones/$1/alta" "${2-$TOK_MEDICO}"; }
code="$(alta $ID_A "$TOK_ADMISIONES")"; ok "I01 ADMISIONES no puede dar de alta -> 403" 403 "$code"
code="$(alta no-existe)"; ok "I02 alta de admisión inexistente -> 404" 404 "$code"
code="$(alta $ID_A)"
ALTA_OK=0
if [ "$code" = 503 ]; then
  skip "I03 alta A -> 503: laboratorio-service/mock gRPC (9091) no responde (prueba el camino feliz con mock gRPC arriba)"
else
  ok "I03 alta A -> 204" 204 "$code"; ALTA_OK=1
fi
if [ "$ALTA_OK" = 1 ]; then
  req GET "/api/admisiones/$ID_A" "$TOK_MEDICO" >/dev/null
  ok "I04 estado DADA_DE_ALTA" DADA_DE_ALTA "$(jb .estado)"
  ok "I05 fechaAlta registrada" true "$(jb '.fechaAlta != null')"
  req GET "/api/admisiones/inventario-camas?sedeId=$S2&servicio=URGENCIAS" "$TOK_ADMISIONES" >/dev/null
  ok "I06 cama liberada (S2/URG ocupadas=0)" 0 "$(jb .camasOcupadas)"
  code="$(alta $ID_A)"; ok "I07 doble alta -> 409" 409 "$code"
  code="$(tras $ID_A $S1 URGENCIAS)"; ok "I08 trasladar admisión dada de alta -> 409" 409 "$code"
  t "I08b verificar cobertura sobre admisión dada de alta -> 409" 409 POST "/api/admisiones/$ID_A/verificar-cobertura" "$TOK_ADMISIONES"
  code="$(ing $PA $S2 URGENCIAS)"; ok "I09 reingreso del mismo paciente tras el alta -> 201" 201 "$code"
fi

# =====================================================================
seccion "J. CONCURRENCIA e INTEGRIDAD"
# =====================================================================
for i in 1 2; do
  ( curl -s -o /dev/null -w '%{http_code}' -X POST "$GW/api/admisiones/ingreso?pacienteId=RACE${SUF}$i&sedeId=$S3&servicio=URGENCIAS&medicoTratante=Dr.%20Race&motivoIngreso=Urgencia" \
      -H "Authorization: Bearer $TOK_ADMISIONES" > "$TMP/c$i" ) &
done; wait
ok "J01 2 ingresos simultáneos, 1 sola cama -> uno 201 y otro 409" "201 409" "$(printf '%s\n%s\n' "$(cat "$TMP/c1")" "$(cat "$TMP/c2")" | sort | paste -sd' ' -)"
req GET "/api/admisiones/inventario-camas?sedeId=$S3&servicio=URGENCIAS" "$TOK_ADMISIONES" >/dev/null
ok "J02 ocupadas = 1 (nunca 2)" 1 "$(jb .camasOcupadas)"
for i in 1 2; do
  ( curl -s -o /dev/null -w '%{http_code}' -X POST "$GW/api/admisiones/ingreso?pacienteId=MISMO$SUF&sedeId=$S3&servicio=UCI&medicoTratante=Dr.%20Race&motivoIngreso=Urgencia" \
      -H "Authorization: Bearer $TOK_ADMISIONES" > "$TMP/d$i" ) &
done; wait
ok "J03 mismo paciente 2 veces a la vez -> uno 201 y otro 409" "201 409" "$(printf '%s\n%s\n' "$(cat "$TMP/d1")" "$(cat "$TMP/d2")" | sort | paste -sd' ' -)"
req GET "/api/admisiones/inventario-camas?sedeId=$S3&servicio=UCI" "$TOK_ADMISIONES" >/dev/null
ok "J04 solo 1 cama UCI ocupada (el perdedor no deja la cama tomada)" 1 "$(jb .camasOcupadas)"
code="$(ing "PAC%22COMILLA$SUF" $S3 UCI "$TOK_ADMISIONES" "Dr.%20%22Comilla%22" "Motivo%20%22Especial%22")"; ok "J05 paciente con comilla doble en el ID -> 201" 201 "$code"

# =====================================================================
seccion "K. HU-08 consultas / CQRS (consistencia eventual vía Kafka, espera 8 s)"
# =====================================================================
sleep 8
t "K01 consolidado del paciente A -> 200" 200 GET "/api/consultas/admisiones/paciente/$PA/consolidado" "$TOK_MEDICO"
if [ "$ALTA_OK" = 1 ]; then
  ok "K02 2 fichas (admisión dada de alta + reingreso)" 2 "$(jb '.admisiones|length')"
  ok "K03 una ficha DADA_DE_ALTA con 1 traslado y cobertura 80%" 1 "$(jb '[.admisiones[]|select(.estado=="DADA_DE_ALTA" and .totalTraslados==1 and .coberturaPorcentaje==80)]|length')"
else
  ok "K02 1 ficha ACTIVA con 1 traslado y cobertura 80%" 1 "$(jb '[.admisiones[]|select(.estado=="ACTIVA" and .totalTraslados==1 and .coberturaPorcentaje==80)]|length')"
fi
ok "K03b proyección conserva medicoTratante, motivoIngreso y servicioActual" "Dr. House|Dolor toracico agudo|URGENCIAS" "$(jb "[.admisiones[]|select(.admisionId==\"$ID_A\")][0] | .medicoTratante + \"|\" + .motivoIngreso + \"|\" + .servicioActual")"
t "K04 ficha por id de admisión -> 200" 200 GET "/api/consultas/admisiones/$ID_C" "$TOK_ADMISIONES"
t "K05 ficha inexistente -> 404" 404 GET "/api/consultas/admisiones/no-existe" "$TOK_ADMISIONES"
t "K06 filtro ?estado=activa (minúsculas) -> 200" 200 GET "/api/consultas/admisiones?estado=activa" "$TOK_MEDICO"
t "K07 por paciente -> 200" 200 GET "/api/consultas/admisiones/paciente/$PC" "$TOK_MEDICO"
t "K08 consolidado de paciente desconocido -> 200 con listas vacías" 200 GET "/api/consultas/admisiones/paciente/NADIE$SUF/consolidado" "$TOK_MEDICO"
ok "K09 listas vacías" 0 "$(jb '.admisiones|length')"
t "K10 reporte consultas ocupación S1 (ADMIN)" 200 GET "/api/consultas/admisiones/reporte/ocupacion?sedeId=$S1" "$AD"
ok "K11 el read-model coincide con el write-model (2 activas en S1)" 2 "$(jb .camasOcupadasEnTiempoReal)"
t "K12 la ficha del paciente con comilla (J05) llegó a consultas" 200 GET "/api/consultas/admisiones/paciente/PAC%22COMILLA$SUF" "$TOK_MEDICO"
ok "K13 ...y tiene 1 ficha (JSON bien escapado en outbox)" 1 "$(jb length)"

# ---------- resumen ----------
printf '\n\033[1mRESULTADO: %d OK · %d FALLAS · %d omitidas\033[0m\n' "$PASS" "$FAIL" "$SKIP"
[ "$FAIL" -eq 0 ]