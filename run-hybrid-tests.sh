#!/usr/bin/env bash
# Envoltorio de compatibilidad (RM-04): mismo nombre y mismos argumentos que antes.
# Equivale a ./run-preprod-tests.sh --target hybrid; la ayuda está en --help.
exec "$(dirname "$0")/run-preprod-tests.sh" --target hybrid "$@"
