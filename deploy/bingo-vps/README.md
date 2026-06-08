# Bingo URP VPS deployment

Target layout, separate from the existing GeoURP app:

- Frontend: `/var/www/bingo/frontend/browser`
- Backend: `/var/www/bingo/backend/bingo-backend.jar`
- Java runtime: `/var/www/bingo/runtime`
- Data: `/var/www/bingo/data`
- Backend service: `bingo-backend.service`
- Backend local port: `127.0.0.1:8088`
- Public frontend path: `https://geourp.org/bingo/`
- Public API path: `https://geourp.org/bingo-api/`

This package is self-contained and uses an embedded H2 database, so it does
not need Java or MySQL installed through apt.

Install files:

```bash
mkdir -p /etc/bingo /var/www/bingo/backend /var/www/bingo/frontend /var/www/bingo/data
cp backend/bingo-backend.jar /var/www/bingo/backend/bingo-backend.jar
cp -a frontend/browser /var/www/bingo/frontend/
cp -a runtime /var/www/bingo/runtime
cp deploy/bingo-backend.env.example /etc/bingo/bingo-backend.env
# edit /etc/bingo/bingo-backend.env with real secrets
chown -R www-data:www-data /var/www/bingo
chmod 640 /etc/bingo/bingo-backend.env
```

Install service:

```bash
cp deploy/bingo-backend.service /etc/systemd/system/bingo-backend.service
systemctl daemon-reload
systemctl enable --now bingo-backend
```

Nginx:

Add the contents of `nginx-bingo-locations.conf` inside the existing
`server_name geourp.org www.geourp.org;` SSL server block, before `location /`.

Then:

```bash
nginx -t
systemctl reload nginx
```

Smoke test:

```bash
curl -I https://geourp.org/bingo/
curl https://geourp.org/bingo-api/public/cards/0001/verify
```

```bash
nginx -t
systemctl reload nginx
```
