#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
======================================================================================================
Verwendung: ./docker-compose-local-rebuild.sh [Optionen] [-- <zusätzliche Maven-Argumente>]
Optionen (werden direkt an mvn-install-all.sh weitergereicht):
  -p, --projects LIST  Nur bestimmte Projekte bauen (Komma-Liste oder mehrfach angeben, Globs erlaubt)
                       Beispiele:
                         -p lib/*,server/*
                         -p lib/zeta-client-lib-java -p test/*
  -s, --skip-tests     Tests überspringen (setzt -DskipTests und -Dskip.unittests=true)
  -d, --dry-run        Nur anzeigen, welche Befehle ausgeführt würden (keine Ausführung)
  -h, --help           Hilfe anzeigen
======================================================================================================
USAGE
}

# Hilfe abfangen
if [[ $# -gt 0 ]]; then
  case "$1" in
    -h|--help) usage; exit 0 ;;
  esac
fi

doc/bin/vsdm/mvn-install-all.sh "$@"

# Dry-Run abfangen
if [[ $# -gt 0 ]]; then
  case "$1" in
    -d|--dry-run) exit 0 ;;
  esac
fi

doc/bin/vsdm/docker-build-all-local.sh
doc/bin/vsdm/docker-compose-local-restart.sh
