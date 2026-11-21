#!/usr/bin/env bash
GATEWAY=http://localhost:8000
ADMIN_EMAIL=admin@hospital.com
ADMIN_PASS=123456789

RESP=$(curl -s -X POST $GATEWAY/api/auth/login -H 'Content-Type: application/json' \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASS\"}")
TOKEN=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('token',''))")
[ -z "$TOKEN" ] && { echo "SIN TOKEN: $RESP"; exit 1; }

echo "== Usuarios (un 409 significa que ya existían) =="
for datos in "admisiones@hospital.com ADMISIONES" "medico@hospital.com MEDICO"; do
  read -r correo rol <<< "$datos"
  curl -s -X POST $GATEWAY/api/auth/usuarios -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$correo\",\"password\":\"12345678\",\"rol\":\"$rol\"}"; echo
done

echo "== Camas =="
for datos in "TUNJA URGENCIAS 10" "TUNJA UCI 4" "DUITAMA URGENCIAS 6" "DUITAMA HOSPITALIZACION 12" "SOGAMOSO PEDIATRIA 8"; do
  read -r sede servicio total <<< "$datos"
  curl -s -X POST $GATEWAY/api/admisiones/inventario-camas -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' \
    -d "{\"sedeId\":\"$sede\",\"servicio\":\"$servicio\",\"camasTotales\":$total}"; echo
done