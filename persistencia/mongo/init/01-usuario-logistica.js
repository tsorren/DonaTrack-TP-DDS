// Crea el usuario de logistica-service con permisos solo sobre la base logistica.
// Corre una sola vez, con el volumen de datos vacío (docker-entrypoint-initdb.d), autenticado
// como el usuario root. Usuario y contraseña llegan por variables de entorno (ver .env.example).
db.getSiblingDB('logistica').createUser({
  user: process.env.LOGISTICA_MONGO_USER,
  pwd: process.env.LOGISTICA_MONGO_PASSWORD,
  roles: [{ role: 'readWrite', db: 'logistica' }],
});
