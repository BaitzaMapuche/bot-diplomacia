# Bot de Telegram - Diplomacia

Bot de Telegram con Spring Boot, webhook y persistencia en PostgreSQL.

## Dependencias
- Java 17
- Spring Boot 3.1.x
- Telegram Bots Meta 6.9.7.1
- PostgreSQL

## Variables de entorno
- `BOT_TOKEN` - token de Telegram
- `DATABASE_URL` - URL JDBC de PostgreSQL
- `DB_USER` - usuario de la base de datos
- `DB_PASSWORD` - contraseña de la base de datos
- `PORT` - puerto HTTP (por defecto 8080)
- `OWNER_TELEGRAM_ID` - tu ID numérico de Telegram (el único que puede usar `/autorizar`); consíguelo hablando con @userinfobot
- `WEBHOOK_SECRET` - secreto que Telegram manda de vuelta en cada petición real al webhook, para que el bot rechace peticiones falsificadas. Generar uno con:
  ```bash
  openssl rand -hex 32
  ```
  Hay que registrarlo tanto en Render como al llamar `setWebhook` (ver más abajo) — si no coinciden, el bot rechaza todas las peticiones con 403.
- `TOKEN_ENC_KEY` - clave AES-256 en Base64 para cifrar en reposo los tokens de sesión del juego. Generar una nueva con:
  ```bash
  openssl rand -base64 32
  ```
  Guárdala igual que un secreto — si la pierdes, los tokens ya guardados quedan indescifrables y cada usuario tendrá que volver a mandar `/token`.

## Comandos del bot
- `/token <token>` - vincula/actualiza tu sesión de diplomacia.com.tr (el bot borra el mensaje inmediatamente después, nunca queda guardado el texto plano en el chat ni en el log de mensajes)
- `/autosubir <habilidad> <recurso>` - activa la subida automática de una estadística (habilidades: Cuartel, Guerra, Cientifico; recursos: Dinero, Diamante)
- `/parar <habilidad|all>` - detiene la subida automática de una estadística (o todas)
- `/estado` - muestra tus tareas activas y cuándo vuelve a intentar cada una
- `/autorizar <telegram_id>` - (solo el owner) autoriza a otro usuario a usar el bot

## Comandos útiles
- `mvn clean package`
- `docker build -t bot-diplomacia .`
- `docker run -e BOT_TOKEN=... -e DATABASE_URL=... -e DB_USER=... -e DB_PASSWORD=... -p 8080:8080 bot-diplomacia`

## Webhook
Registra el webhook después de desplegar, incluyendo el `secret_token` (debe ser igual al `WEBHOOK_SECRET` configurado en Render):

```bash
https://api.telegram.org/bot<YOUR_TOKEN>/setWebhook?url=https://tu-bot.onrender.com/webhook&secret_token=<WEBHOOK_SECRET>
```
