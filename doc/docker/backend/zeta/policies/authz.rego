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
	# 1.2.276.0.76.4.50 = "Betriebsstätte Arzt" - reale professionOID der SMC-B-Testkarte
	allowed_profession_oid_set := {"1.2.276.0.76.4.50"}
	input.user_info.professionOID in allowed_profession_oid_set
}

audience_is_allowed if {
	# reale Audience des VSDM-Fachdienstes (vsdm-zeta-ingress)
	allowed_audience_set := {"https://vsdm-zeta-ingress/"}
	requested_audience_set := {a | a := input.authorization_request.audience[_]}
	count(requested_audience_set) > 0
	requested_audience_set - allowed_audience_set == set()
}

product_id_is_allowed if {
	# reale product_id des VSDM-Testclients (vsdm-client-simservice-java)
	allowed_product_id_set := {"demo-client"}
	input.client_registration_data.product_id in allowed_product_id_set
}

product_version_is_allowed if {
	# reale product_version des VSDM-Testclients (vsdm-client-simservice-java)
	allowed_product_version_set := {"0.2.0"}
	input.client_registration_data.product_version in allowed_product_version_set
}

scopes_are_allowed if {
	allowed_scope_set := {"vsdservice"}
	requested_scope_set := {s | s := input.authorization_request.scopes[_]}
	count(requested_scope_set) > 0
	requested_scope_set - allowed_scope_set == set()
}
