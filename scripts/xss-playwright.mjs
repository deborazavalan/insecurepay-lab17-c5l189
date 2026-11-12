import { chromium } from 'playwright';
import { mkdirSync } from 'node:fs';

const FRONT = process.env.FRONT_URL ?? 'http://localhost:4200';
const SALIDA = process.env.SALIDA ?? 'evidencias';
const VERSION = process.env.VERSION_AUDITADA ?? 'local';
const IBAN_MARIA = 'CR05015202001026458902';
const PAYLOAD = `Pago de Servicios <img src="x" onerror="alert('AUDITORIA FORENSE: Brecha XSS confirmada. Sesion: ' + localStorage.getItem('insecure_pay_user'))">`;

mkdirSync(SALIDA, { recursive: true });

const navegador = await chromium.launch();
const pagina = await navegador.newPage({ viewport: { width: 1366, height: 900 } });
const alertas = [];

pagina.on('dialog', async dialogo => {
  alertas.push(dialogo.message());
  await dialogo.accept();
});

await pagina.goto(`${FRONT}/login`);
await pagina.fill('#username', 'carlos_mora');
await pagina.fill('#password', 'carlos123');
await pagina.click('button[type=submit]');
await pagina.waitForURL('**/dashboard');

await pagina.locator('a[href="/transferir"]').first().click();
await pagina.waitForURL('**/transferir');
await pagina.waitForFunction(() => {
  const s = document.querySelector('#origen');
  return s && [...s.options].some(o => o.text.includes('458901'));
});
await pagina.$eval('#origen', s => {
  s.selectedIndex = [...s.options].findIndex(o => o.text.includes('458901'));
  s.dispatchEvent(new Event('change'));
});
await pagina.fill('#destino', IBAN_MARIA);
await pagina.fill('#monto', '1000');
await pagina.fill('#concepto', PAYLOAD);
await pagina.screenshot({ path: `${SALIDA}/xss-1-formulario-${VERSION}.png`, fullPage: true });
await pagina.click('button[type=submit]');
await pagina.waitForSelector('.alert-info');
await pagina.waitForURL('**/transacciones');
await pagina.waitForSelector('table tbody tr');

const filas = pagina.locator('table tbody tr');
const total = await filas.count();
let indiceMayor = 0;
let idMayor = -1;
for (let i = 0; i < total; i++) {
  const id = parseInt((await filas.nth(i).locator('td').first().innerText()).replace(/\D/g, ''), 10);
  if (id > idMayor) {
    idMayor = id;
    indiceMayor = i;
  }
}
await filas.nth(indiceMayor).locator('button').click();
await pagina.waitForTimeout(1500);
await pagina.screenshot({ path: `${SALIDA}/xss-2-comprobante-${VERSION}.png`, fullPage: true });

const imagenesInyectadas = await pagina.locator('img[src="x"]').count();

console.log(`Version auditada: ${VERSION}`);
console.log(`Comprobante abierto: transaccion #${idMayor}`);
console.log(`Elementos <img src="x"> inyectados en el DOM: ${imagenesInyectadas}`);
if (alertas.length > 0) {
  console.log('RESULTADO: JavaScript ejecutado en el navegador de la victima.');
  alertas.forEach(a => console.log(`alert() -> ${a.replace(/("token":")([^"]{20})[^"]*/, '$1$2...[recortado]')}`));
} else {
  console.log('RESULTADO: No se ejecuto JavaScript; el concepto se renderizo como texto plano.');
}

await navegador.close();
