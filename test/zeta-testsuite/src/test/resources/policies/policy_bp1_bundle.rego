# Policy BP1 (Bundle-Variante von P1): Erlaubt nur professionOid "1.2.276.0.76.4.49" (Arzt)
# PS1 (mit professionOid 1.2.276.0.76.4.49) wird akzeptiert
# PS11 (mit professionOid 1.2.276.0.76.4.50) wird abgelehnt
#
# Inhaltlich identisch zu policy_p1.rego, aber im isolierten Test-Paket "zeta.bundletest.authz"
# statt "zeta.authz" (siehe PolicyBundlePollingSteps), damit das per Bundle-Polling geladene
# Test-Bundle nicht mit der produktiven "zeta.authz"-Policy (REST-API-Hot-Reload-Szenario in
# PolicyUpdateabilitySteps) kollidiert.
package zeta.bundletest.authz

import rego.v1

default decision := {
  "allow": false,
  "ttl": {
    "access_token": 300,
    "refresh_token": 86400,
  },
}

decision := result if {
  input.user_info.professionOID == "1.2.276.0.76.4.49"
  result := {
    "allow": true,
    "ttl": {
      "access_token": 300,
      "refresh_token": 86400,
    },
  }
}
