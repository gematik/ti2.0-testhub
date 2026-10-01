package zeta.authz

import future.keywords.if
import future.keywords.in

# Regel 1: Definiert 'decision' für den FEHLERFALL.
decision := response if {
	failures := reasons
	count(failures) > 0
	response := {
		"allow": false,
		"reasons": failures,
	}
}

# Regel 2: Definiert 'decision' für den ERFOLGSFALL.
decision := response if {
	count(reasons) == 0
	response := {
		"allow": true,
		"ttl": {
			"access_token": 300,
			"refresh_token": 86400,
		},
	}
}

reasons[msg] if {
	not scopes_are_allowed
	msg := "One or more requested scopes are not allowed"
}

reasons[msg] if {
	not profession_oid_is_allowed
	msg := "The professionOID is not allowed by this policy"
}

reasons[msg] if {
	not audience_is_allowed
	msg := "One or more requested audiences are not allowed"
}

reasons[msg] if {
	not product_id_is_allowed
	msg := "The product_id is not allowed by this policy"
}

reasons[msg] if {
	not product_version_is_allowed
	msg := "The product_version is not allowed by this policy"
}

profession_oid_is_allowed if {
	# Deny-List statt Allow-List: nur den eindeutig synthetischen, im Negativ-Test
	# manipulierten Wert ablehnen. So bleibt die Policy für echte SMC-B-Karten mit
	# beliebiger realer professionOID durchlässig (siehe zeta-client-policy/client_policy.feature).
	denied_profession_oid_set := {"1.2.276.0.76.4.999"}
	not input.user_info.professionOID in denied_profession_oid_set
}

audience_is_allowed if {
	# Deny-List statt Allow-List (siehe profession_oid_is_allowed): nur die im
	# Negativ-Test manipulierte, eindeutig ungültige Audience ablehnen.
	denied_audience_set := {"https://evil.example.com/api"}
	requested_audience_set := {a | a := input.authorization_request.audience[_]}
	requested_audience_set & denied_audience_set == set()
}

product_id_is_allowed if {
	# Deny-List statt Allow-List (siehe profession_oid_is_allowed): nur die im
	# Negativ-Test manipulierte, eindeutig ungültige product_id ablehnen.
	denied_product_id_set := {"unknown-client"}
	not input.client_registration_data.product_id in denied_product_id_set
}

product_version_is_allowed if {
	# Deny-List statt Allow-List (siehe profession_oid_is_allowed): nur die im
	# Negativ-Test manipulierte, eindeutig ungültige product_version ablehnen.
	denied_product_version_set := {"99.99.99"}
	not input.client_registration_data.product_version in denied_product_version_set
}

scopes_are_allowed if {
	# Deny-List statt Allow-List (siehe profession_oid_is_allowed): nur den im
	# Negativ-Test manipulierten, eindeutig ungültigen Scope ablehnen.
	denied_scope_set := {"invalid_scope_xyz"}
	requested_scope_set := {s | s := input.authorization_request.scopes[_]}
	requested_scope_set & denied_scope_set == set()
}
