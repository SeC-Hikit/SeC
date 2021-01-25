package org.sc.common.rest.response

import org.sc.common.rest.MaintenanceDto
import org.sc.common.rest.PoiDto
import org.sc.common.rest.Status

data class PoiResponse (val status: Status,
                        val messages: Set<String>,
                        val poiList: List<PoiDto>) : RESTResponse(status, messages)