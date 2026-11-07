// Healthcheck del servicio mongodb. La primera vez inicia el replica set de un nodo, que
// MongoTransactionManager necesita para las transacciones multi-documento, y responde sano
// recién cuando el nodo es primario: antes no acepta escrituras.
try {
  rs.status();
} catch (replicaSetSinIniciar) {
  rs.initiate({ _id: 'rs0', members: [{ _id: 0, host: 'mongodb:27017' }] });
}
quit(db.hello().isWritablePrimary ? 0 : 1);
