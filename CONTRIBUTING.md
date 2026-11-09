# Cómo trabajar en el repositorio

Guía corta para los dos integrantes del equipo.

## Ramas

- `main` siempre debe compilar y pasar las pruebas. No se trabaja directo en `main`.
- Cada historia o ajuste va en su propia rama, con un nombre corto: `rutas-seguras`, `ajuste-informe`,
  `documento-fase4`.
- Al terminar se abre un pull request hacia `main`. GitHub Actions corre las pruebas; si pasan y el otro
  integrante lo revisa, se integra.

```bash
git switch main && git pull
git switch -c nombre-de-la-rama
# ...cambios...
git push -u origin nombre-de-la-rama
gh pr create --fill
```

## Mensajes de commit

En español, en imperativo o como descripción corta, con un prefijo que diga qué tipo de cambio es:

| Prefijo | Para |
|---|---|
| `feat` | Una función nueva (`feat(rutas): ruta segura con A*`) |
| `fix` | Un error corregido |
| `test` | Pruebas nuevas o ajustadas |
| `docs` | Documentación |
| `ci` | Integración continua y scripts |

Entre paréntesis va el módulo, y si el cambio responde a una historia de usuario se cita (`HU 04`).

## Antes de subir cambios

```bash
cd backend && mvn test
cd ia-service && .venv/bin/python -m pytest
cd frontend && npm run lint && npm run build
cd frontend && npm run test:e2e        # con ./scripts/iniciar.sh corriendo
```

Si cambias el modelo de datos, crea una migración nueva en `backend/src/main/resources/db/migration`
(`V3__descripcion.sql`, `V4__...`). Nunca se edita una migración que ya se aplicó.

## Convenciones del código

- Nombres de clases, variables y textos en español, igual que el dominio del proyecto.
- Cada clase de servicio o controlador empieza con un comentario que dice qué historia de usuario implementa.
- Los textos que ve el usuario son cortos, en segunda persona y sin tecnicismos.
- Los datos de prueba son sintéticos. No se suben denuncias reales ni datos personales.
