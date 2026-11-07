#!/usr/bin/env bash
# Envoltorio de compatibilidad (RM-04): mismo nombre y mismos argumentos que antes
# (--skip-build, --test <Clase>). Equivale a ./run-preprod-tests.sh --stay: corre la suite y
# deja el stack arriba hasta presionar una tecla.
exec "$(dirname "$0")/run-preprod-tests.sh" --stay "$@"
