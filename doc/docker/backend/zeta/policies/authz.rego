package zeta.authz

# Minimal decision object compatible with callers expecting
# data.zeta.authz.decision => {"allow": bool, "ttl": {"access_token": number, "refresh_token": number}}
decision := {
  "allow": true,
  "ttl": {
    "access_token": 300,
    "refresh_token": 86400,
  },
}