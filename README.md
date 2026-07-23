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

## Comandos útiles
- `mvn clean package`
- `docker build -t bot-diplomacia .`
- `docker run -e BOT_TOKEN=... -e DATABASE_URL=... -e DB_USER=... -e DB_PASSWORD=... -p 8080:8080 bot-diplomacia`

## Webhook
Registra el webhook después de desplegar:

```bash
https://api.telegram.org/bot<YOUR_TOKEN>/setWebhook?url=https://tu-bot.onrender.com/webhook
```
