#!/usr/bin/env bash
set -uo pipefail 2>/dev/null || set -o pipefail

# ----------------------------------
# Projektliste (Default)
# ----------------------------------
projects=(
  "."
  "lib/card-client-lib-java"
  "lib/zeta-client-lib-java"
  "lib/popp-client-lib-java"
  "lib/vsdm-fhir-lib-java"
  "client/card-terminal-client-simservice"
  "client/vsdm-client-simservice-java"
  "server/popp-server-mockservice"
  "server/vsdm-server-simservice"
  "server/zeta-pep-server-mockservice"
  "server/zeta-pdp-server-mockservice"
  "test/vsdm-testsuite"
)

# ----------------------------------
# Optionen
# ----------------------------------
SKIP_TESTS=false
DRY_RUN=false
MVN_EXTRA_ARGS=()
INCLUDE_PATTERNS=()

usage() {
  cat <<'USAGE'
=======================================================================================
Verwendung: mvn-install-all.sh [Optionen] [-- <zusätzliche Maven-Argumente>]

Optionen:
  -p, --projects LIST  Nur bestimmte Projekte bauen. LIST kann sein:
                         - Komma-Liste:    -p lib/*,server/*
                         - Mehrere Werte:  -p lib/* server/* test/*
  -s, --skip-tests     Tests überspringen (setzt -DskipTests und -Dskip.unittests=true)
  -d, --dry-run        Nur anzeigen, was gebaut würde (keine Ausführung)
  -h, --help           Hilfe anzeigen

Beispiele:
  ./mvn-install-all.sh
  ./mvn-install-all.sh -s -p server/*
=======================================================================================
USAGE
}

split_csv_into_array() {
  local csv="$1"
  local IFS=','; for p in $csv; do INCLUDE_PATTERNS+=("$p"); done
}

# Argumente parsen (sammelt nach -p alle nicht-Optionen bis zum nächsten Schalter/--/Ende)
while [[ $# -gt 0 ]]; do
  case "$1" in
    -p|--projects)
      shift
      if [[ $# -eq 0 ]]; then echo "Fehler: -p|--projects benötigt mindestens einen Wert."; exit 2; fi
      while [[ $# -gt 0 && "$1" != "--" && "$1" != -* ]]; do
        split_csv_into_array "$1"
        shift
      done
      ;;
    -s|--skip-tests) SKIP_TESTS=true; shift ;;
    -d|--dry-run)    DRY_RUN=true; shift ;;
    -h|--help)       usage; exit 0 ;;
    --)              shift; MVN_EXTRA_ARGS=("$@"); break ;;
    *)               echo "Unbekannte Option oder unerwartetes Argument: $1"; usage; exit 2 ;;
  esac
done

# ----------------------------------
# Projektfilterung
# ----------------------------------
matches_any_pattern() {
  local s="$1"; shift
  for pat in "$@"; do
    if [[ "$s" == $pat ]]; then return 0; fi
  done
  return 1
}

filtered_projects=()
if ((${#INCLUDE_PATTERNS[@]})); then
  for p in "${projects[@]}"; do
    if matches_any_pattern "$p" "${INCLUDE_PATTERNS[@]}"; then
      filtered_projects+=("$p")
    fi
  done
else
  filtered_projects=("${projects[@]}")
fi

if ((${#filtered_projects[@]}==0)); then
  echo "Keine Projekte nach Filterung übrig. Prüfe deine -p Muster."
  exit 3
fi

# ----------------------------------
# Maven-Argumente
# ----------------------------------
MVN_ARGS=(--batch-mode)
if "$SKIP_TESTS"; then
  MVN_ARGS+=(-DskipTests -Dskip.unittests=true)
else
  MVN_ARGS+=(-Dskip.unittests=false)
fi
if ((${#MVN_EXTRA_ARGS[@]})); then
  MVN_ARGS+=("${MVN_EXTRA_ARGS[@]}")
fi

# ----------------------------------
# Ergebnis-Sammler
# ----------------------------------
SUCCESS_PROJECTS=()
FAILED_PROJECTS=()
DURATIONS_S=()

now_s() { date +%s; }
pad()   { printf "%-48s" "$1"; }
secs_to_hms() {
  local s="$1"
  printf "%02d:%02d:%02d" $((s/3600)) $(((s%3600)/60)) $((s%60))
}

build_project() {
  local project="$1"
  local pom="$project/pom.xml"

  echo
  echo "-------------------------------------"
  echo "Building project: $project"
  echo "-------------------------------------"

  if [[ ! -f "$pom" ]]; then
    echo "WARNUNG: POM nicht gefunden: $pom" >&2
    return 2
  fi

  echo "Befehl: mvn -f \"$pom\" clean install ${MVN_ARGS[*]}"
  if "$DRY_RUN"; then
    return 0
  fi

  mvn -f "$pom" clean install "${MVN_ARGS[@]}"
}

# ----------------------------------
# Ausführung
# ----------------------------------
for i in "${!filtered_projects[@]}"; do
  project="${filtered_projects[$i]}"
  start=$(now_s)
  if build_project "$project"; then
    SUCCESS_PROJECTS+=("$project")
  else
    FAILED_PROJECTS+=("$project")
  fi
  end=$(now_s)
  DURATIONS_S[$i]=$(( end - start ))
done

# ----------------------------------
# Zusammenfassung
# ----------------------------------
echo
echo "==================== Build-Zusammenfassung ===================="

if ((${#SUCCESS_PROJECTS[@]})); then
  echo
  echo "Erfolgreich:"
  for p in "${SUCCESS_PROJECTS[@]}"; do
    dur=0
    for j in "${!filtered_projects[@]}"; do
      if [[ "${filtered_projects[$j]}" == "$p" ]]; then dur="${DURATIONS_S[$j]:-0}"; break; fi
    done
    printf "%s  %s\n" "$(pad "$p")" "$(secs_to_hms "$dur")"
  done
fi

if ((${#FAILED_PROJECTS[@]})); then
  echo
  echo "Fehlgeschlagen:"
  for p in "${FAILED_PROJECTS[@]}"; do
    dur=0
    for j in "${!filtered_projects[@]}"; do
      if [[ "${filtered_projects[$j]}" == "$p" ]]; then dur="${DURATIONS_S[$j]:-0}"; break; fi
    done
    printf "%s  %s\n" "$(pad "$p")" "$(secs_to_hms "$dur")"
  done
fi

echo
total_s=0
for j in "${!filtered_projects[@]}"; do
  total_s=$(( total_s + ${DURATIONS_S[$j]:-0} ))
done
echo "Gesamtdauer: $(secs_to_hms "$total_s")"

if ((${#FAILED_PROJECTS[@]})); then
  echo
  echo "Build abgeschlossen: Es gab Fehler in ${#FAILED_PROJECTS[@]} Projekt(en)."
  echo
  echo "==================== Build-Zusammenfassung ===================="
  exit 1
else
  echo
  echo "Build abgeschlossen: Alle ${#SUCCESS_PROJECTS[@]} Projekte erfolgreich."
  echo
  echo "==================== Build-Zusammenfassung ===================="
  exit 0
fi
