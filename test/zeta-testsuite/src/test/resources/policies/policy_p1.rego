# Policy P1: Erlaubt nur professionOid "1.2.276.0.76.4.49" (Arzt)
# PS1 (mit professionOid 1.2.276.0.76.4.49) wird akzeptiert
# PS11 (mit professionOid 1.2.276.0.76.4.50) wird abgelehnt
package zeta.authz

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

