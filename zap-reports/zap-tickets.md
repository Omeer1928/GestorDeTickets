# ZAP Scanning Report

ZAP by [Checkmarx](https://checkmarx.com/).


## Summary of Alerts

| Risk Level | Number of Alerts |
| --- | --- |
| High | 0 |
| Medium | 0 |
| Low | 2 |
| Informational | 2 |




## Insights

| Level | Reason | Site | Description | Statistic |
| --- | --- | --- | --- | --- |
| Medium | Exceeded High | http://api-gateway:8080 | Percentage of responses with status code 4xx | 99 % |
| Low | Warning |  | ZAP warnings logged - see the zap.log file for details | 1    |
| Info | Informational | http://api-gateway:8080 | Percentage of responses with status code 2xx | 1 % |
| Info | Informational | http://api-gateway:8080 | Percentage of responses with status code 5xx | 1 % |
| Info | Informational | http://api-gateway:8080 | Percentage of endpoints with content type application/json | 72 % |
| Info | Informational | http://api-gateway:8080 | Percentage of endpoints with method GET | 57 % |
| Info | Informational | http://api-gateway:8080 | Percentage of endpoints with method PATCH | 38 % |
| Info | Informational | http://api-gateway:8080 | Percentage of endpoints with method POST | 3 % |
| Info | Informational | http://api-gateway:8080 | Count of total endpoints | 54    |
| Info | Informational | http://api-gateway:8080 | Percentage of slow responses | 1 % |







## Alerts

| Name | Risk Level | Number of Instances |
| --- | --- | --- |
| A Server Error response code was returned by the server | Low | 2 |
| Cross-Origin-Resource-Policy Header Missing or Invalid | Low | Systemic |
| A Client Error response code was returned by the server | Informational | 66 |
| Non-Storable Content | Informational | Systemic |




## Alert Detail



### [ A Server Error response code was returned by the server ](https://www.zaproxy.org/docs/alerts/100000/)



##### Low (High)

### Description

A response code of 500 was returned by the server.
This may indicate that the application is failing to handle unexpected input correctly.
Raised by the 'Alert on HTTP Response Code Error' script

* URL: http://api-gateway:8080/tickets/id/assign
  * Node Name: `http://api-gateway:8080/tickets/id/assign ()({technicianId})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `500`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets
  * Node Name: `http://api-gateway:8080/tickets ()({userId,title,description,category})`
  * Method: `POST`
  * Parameter: ``
  * Attack: ``
  * Evidence: `500`
  * Other Info: ``


Instances: 2

### Solution



### Reference



#### CWE Id: [ 388 ](https://cwe.mitre.org/data/definitions/388.html)


#### WASC Id: 20

#### Source ID: 4

### [ Cross-Origin-Resource-Policy Header Missing or Invalid ](https://www.zaproxy.org/docs/alerts/90004/)



##### Low (Medium)

### Description

Cross-Origin-Resource-Policy header is an opt-in header designed to counter side-channels attacks like Spectre. Resource should be specifically set as shareable amongst different origins.

* URL: http://api-gateway:8080/tickets
  * Node Name: `http://api-gateway:8080/tickets`
  * Method: `GET`
  * Parameter: `Cross-Origin-Resource-Policy`
  * Attack: ``
  * Evidence: ``
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/client/clientId
  * Node Name: `http://api-gateway:8080/tickets/client/clientId`
  * Method: `GET`
  * Parameter: `Cross-Origin-Resource-Policy`
  * Attack: ``
  * Evidence: ``
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/status/REGISTRADO
  * Node Name: `http://api-gateway:8080/tickets/status/REGISTRADO`
  * Method: `GET`
  * Parameter: `Cross-Origin-Resource-Policy`
  * Attack: ``
  * Evidence: ``
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/technician/technicianId
  * Node Name: `http://api-gateway:8080/tickets/technician/technicianId`
  * Method: `GET`
  * Parameter: `Cross-Origin-Resource-Policy`
  * Attack: ``
  * Evidence: ``
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/v3/api-docs
  * Node Name: `http://api-gateway:8080/tickets/v3/api-docs`
  * Method: `GET`
  * Parameter: `Cross-Origin-Resource-Policy`
  * Attack: ``
  * Evidence: ``
  * Other Info: ``

Instances: Systemic


### Solution

Ensure that the application/web server sets the Cross-Origin-Resource-Policy header appropriately, and that it sets the Cross-Origin-Resource-Policy header to 'same-origin' for all web pages.
'same-site' is considered as less secured and should be avoided.
If resources must be shared, set the header to 'cross-origin'.
If possible, ensure that the end user uses a standards-compliant and modern web browser that supports the Cross-Origin-Resource-Policy header (https://caniuse.com/mdn-http_headers_cross-origin-resource-policy).

### Reference


* [ https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Cross-Origin-Embedder-Policy ](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Cross-Origin-Embedder-Policy)


#### CWE Id: [ 693 ](https://cwe.mitre.org/data/definitions/693.html)


#### WASC Id: 14

#### Source ID: 3

### [ A Client Error response code was returned by the server ](https://www.zaproxy.org/docs/alerts/100000/)



##### Informational (High)

### Description

A response code of 404 was returned by the server.
This may indicate that the application is failing to handle unexpected input correctly.
Raised by the 'Alert on HTTP Response Code Error' script

* URL: http://api-gateway:8080
  * Node Name: `http://api-gateway:8080`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/
  * Node Name: `http://api-gateway:8080/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/8818322850117397012
  * Node Name: `http://api-gateway:8080/8818322850117397012`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/
  * Node Name: `http://api-gateway:8080/tickets/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/5122342063513767297
  * Node Name: `http://api-gateway:8080/tickets/5122342063513767297`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/client
  * Node Name: `http://api-gateway:8080/tickets/client`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/client
  * Node Name: `http://api-gateway:8080/tickets/client`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/client/
  * Node Name: `http://api-gateway:8080/tickets/client/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/client/clientId/
  * Node Name: `http://api-gateway:8080/tickets/client/clientId/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id
  * Node Name: `http://api-gateway:8080/tickets/id`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/
  * Node Name: `http://api-gateway:8080/tickets/id/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/5614290076097004062
  * Node Name: `http://api-gateway:8080/tickets/id/5614290076097004062`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/actuator/health
  * Node Name: `http://api-gateway:8080/tickets/id/actuator/health`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/events
  * Node Name: `http://api-gateway:8080/tickets/id/events`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/events/
  * Node Name: `http://api-gateway:8080/tickets/id/events/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/queue/
  * Node Name: `http://api-gateway:8080/tickets/queue/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/status
  * Node Name: `http://api-gateway:8080/tickets/status`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/status
  * Node Name: `http://api-gateway:8080/tickets/status`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/status/
  * Node Name: `http://api-gateway:8080/tickets/status/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/status/652898428749714335
  * Node Name: `http://api-gateway:8080/tickets/status/652898428749714335`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `400`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/status/REGISTRADO/
  * Node Name: `http://api-gateway:8080/tickets/status/REGISTRADO/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/technician
  * Node Name: `http://api-gateway:8080/tickets/technician`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/technician
  * Node Name: `http://api-gateway:8080/tickets/technician`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/technician/
  * Node Name: `http://api-gateway:8080/tickets/technician/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/technician/technicianId/
  * Node Name: `http://api-gateway:8080/tickets/technician/technicianId/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/v3
  * Node Name: `http://api-gateway:8080/tickets/v3`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/v3
  * Node Name: `http://api-gateway:8080/tickets/v3`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/v3/
  * Node Name: `http://api-gateway:8080/tickets/v3/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/v3/3648143272460783472
  * Node Name: `http://api-gateway:8080/tickets/v3/3648143272460783472`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/v3/api-docs/
  * Node Name: `http://api-gateway:8080/tickets/v3/api-docs/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/computeMetadata/v1/
  * Node Name: `http://api-gateway:8080/computeMetadata/v1/ ()({technicianId})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/latest/meta-data/
  * Node Name: `http://api-gateway:8080/latest/meta-data/ ()({technicianId})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/metadata/instance
  * Node Name: `http://api-gateway:8080/metadata/instance ()({technicianId})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/metadata/v1
  * Node Name: `http://api-gateway:8080/metadata/v1 ()({technicianId})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/opc/v1/instance/
  * Node Name: `http://api-gateway:8080/opc/v1/instance/ ()({technicianId})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/opc/v2/instance/
  * Node Name: `http://api-gateway:8080/opc/v2/instance/ ()({technicianId})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/openstack/latest/meta_data.json
  * Node Name: `http://api-gateway:8080/openstack/latest/meta_data.json ()({technicianId})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/assign
  * Node Name: `http://api-gateway:8080/tickets/id/assign ()({technicianId})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `400`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/assign
  * Node Name: `http://api-gateway:8080/tickets/id/assign ()({technicianId})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/assign
  * Node Name: `http://api-gateway:8080/tickets/id/assign ()({technicianId})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/assign/
  * Node Name: `http://api-gateway:8080/tickets/id/assign/ ()({technicianId})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/close
  * Node Name: `http://api-gateway:8080/tickets/id/close ()({comment})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/close
  * Node Name: `http://api-gateway:8080/tickets/id/close ()({comment})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/close/
  * Node Name: `http://api-gateway:8080/tickets/id/close/ ()({comment})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/escalate
  * Node Name: `http://api-gateway:8080/tickets/id/escalate ()({reason,newCategory,newTechId})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `400`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/escalate
  * Node Name: `http://api-gateway:8080/tickets/id/escalate ()({reason,newCategory,newTechId})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/escalate
  * Node Name: `http://api-gateway:8080/tickets/id/escalate ()({reason,newCategory,newTechId})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/escalate/
  * Node Name: `http://api-gateway:8080/tickets/id/escalate/ ()({reason,newCategory,newTechId})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/priority
  * Node Name: `http://api-gateway:8080/tickets/id/priority ()({priority,justification})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `400`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/priority
  * Node Name: `http://api-gateway:8080/tickets/id/priority ()({priority,justification})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/priority
  * Node Name: `http://api-gateway:8080/tickets/id/priority ()({priority,justification})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/priority/
  * Node Name: `http://api-gateway:8080/tickets/id/priority/ ()({priority,justification})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/reassign
  * Node Name: `http://api-gateway:8080/tickets/id/reassign ()({newTechnicianId,reason})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `400`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/reassign
  * Node Name: `http://api-gateway:8080/tickets/id/reassign ()({newTechnicianId,reason})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/reassign
  * Node Name: `http://api-gateway:8080/tickets/id/reassign ()({newTechnicianId,reason})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/reassign/
  * Node Name: `http://api-gateway:8080/tickets/id/reassign/ ()({newTechnicianId,reason})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/resolve
  * Node Name: `http://api-gateway:8080/tickets/id/resolve ()({resolutionLog,resolutionCode})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `400`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/resolve
  * Node Name: `http://api-gateway:8080/tickets/id/resolve ()({resolutionLog,resolutionCode})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/resolve
  * Node Name: `http://api-gateway:8080/tickets/id/resolve ()({resolutionLog,resolutionCode})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/resolve/
  * Node Name: `http://api-gateway:8080/tickets/id/resolve/ ()({resolutionLog,resolutionCode})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/start
  * Node Name: `http://api-gateway:8080/tickets/id/start`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/start/
  * Node Name: `http://api-gateway:8080/tickets/id/start/`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets
  * Node Name: `http://api-gateway:8080/tickets ()({userId,title,description,category})`
  * Method: `POST`
  * Parameter: ``
  * Attack: ``
  * Evidence: `400`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets
  * Node Name: `http://api-gateway:8080/tickets ()({userId,title,description,category})`
  * Method: `POST`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets
  * Node Name: `http://api-gateway:8080/tickets ()({userId,title,description,category})`
  * Method: `POST`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/
  * Node Name: `http://api-gateway:8080/tickets/ ()({userId,title,description,category})`
  * Method: `POST`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``


Instances: 66

### Solution



### Reference



#### CWE Id: [ 388 ](https://cwe.mitre.org/data/definitions/388.html)


#### WASC Id: 20

#### Source ID: 4

### [ Non-Storable Content ](https://www.zaproxy.org/docs/alerts/10049/)



##### Informational (Medium)

### Description

The response contents are not storable by caching components such as proxy servers. If the response does not contain sensitive, personal or user-specific information, it may benefit from being stored and cached, to improve performance.

* URL: http://api-gateway:8080/tickets
  * Node Name: `http://api-gateway:8080/tickets`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `no-store`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/v3/api-docs
  * Node Name: `http://api-gateway:8080/tickets/v3/api-docs`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `no-store`
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/escalate
  * Node Name: `http://api-gateway:8080/tickets/id/escalate ()({reason,newCategory,newTechId})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `PATCH `
  * Other Info: ``
* URL: http://api-gateway:8080/tickets/id/priority
  * Node Name: `http://api-gateway:8080/tickets/id/priority ()({priority,justification})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `PATCH `
  * Other Info: ``
* URL: http://api-gateway:8080/tickets
  * Node Name: `http://api-gateway:8080/tickets ()({userId,title,description,category})`
  * Method: `POST`
  * Parameter: ``
  * Attack: ``
  * Evidence: `no-store`
  * Other Info: ``

Instances: Systemic


### Solution

The content may be marked as storable by ensuring that the following conditions are satisfied:
The request method must be understood by the cache and defined as being cacheable ("GET", "HEAD", and "POST" are currently defined as cacheable)
The response status code must be understood by the cache (one of the 1XX, 2XX, 3XX, 4XX, or 5XX response classes are generally understood)
The "no-store" cache directive must not appear in the request or response header fields
For caching by "shared" caches such as "proxy" caches, the "private" response directive must not appear in the response
For caching by "shared" caches such as "proxy" caches, the "Authorization" header field must not appear in the request, unless the response explicitly allows it (using one of the "must-revalidate", "public", or "s-maxage" Cache-Control response directives)
In addition to the conditions above, at least one of the following conditions must also be satisfied by the response:
It must contain an "Expires" header field
It must contain a "max-age" response directive
For "shared" caches such as "proxy" caches, it must contain a "s-maxage" response directive
It must contain a "Cache Control Extension" that allows it to be cached
It must have a status code that is defined as cacheable by default (200, 203, 204, 206, 300, 301, 404, 405, 410, 414, 501).

### Reference


* [ https://datatracker.ietf.org/doc/html/rfc7234 ](https://datatracker.ietf.org/doc/html/rfc7234)
* [ https://datatracker.ietf.org/doc/html/rfc7231 ](https://datatracker.ietf.org/doc/html/rfc7231)
* [ https://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html ](https://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html)


#### CWE Id: [ 524 ](https://cwe.mitre.org/data/definitions/524.html)


#### WASC Id: 13

#### Source ID: 3


