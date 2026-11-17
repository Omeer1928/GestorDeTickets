# ZAP Scanning Report

ZAP by [Checkmarx](https://checkmarx.com/).


## Summary of Alerts

| Risk Level | Number of Alerts |
| --- | --- |
| High | 0 |
| Medium | 0 |
| Low | 1 |
| Informational | 2 |




## Insights

| Level | Reason | Site | Description | Statistic |
| --- | --- | --- | --- | --- |
| Medium | Exceeded High | http://api-gateway:8080 | Percentage of responses with status code 4xx | 99 % |
| Low | Warning |  | ZAP warnings logged - see the zap.log file for details | 1    |
| Info | Informational | http://api-gateway:8080 | Percentage of responses with status code 2xx | 1 % |
| Info | Informational | http://api-gateway:8080 | Percentage of endpoints with content type application/json | 70 % |
| Info | Informational | http://api-gateway:8080 | Percentage of endpoints with method DELETE | 19 % |
| Info | Informational | http://api-gateway:8080 | Percentage of endpoints with method GET | 63 % |
| Info | Informational | http://api-gateway:8080 | Percentage of endpoints with method PATCH | 7 % |
| Info | Informational | http://api-gateway:8080 | Percentage of endpoints with method POST | 7 % |
| Info | Informational | http://api-gateway:8080 | Percentage of endpoints with method PUT | 3 % |
| Info | Informational | http://api-gateway:8080 | Count of total endpoints | 57    |
| Info | Informational | http://api-gateway:8080 | Percentage of slow responses | 1 % |







## Alerts

| Name | Risk Level | Number of Instances |
| --- | --- | --- |
| Cross-Origin-Resource-Policy Header Missing or Invalid | Low | Systemic |
| A Client Error response code was returned by the server | Informational | 61 |
| Non-Storable Content | Informational | Systemic |




## Alert Detail



### [ Cross-Origin-Resource-Policy Header Missing or Invalid ](https://www.zaproxy.org/docs/alerts/90004/)



##### Low (Medium)

### Description

Cross-Origin-Resource-Policy header is an opt-in header designed to counter side-channels attacks like Spectre. Resource should be specifically set as shareable amongst different origins.

* URL: http://api-gateway:8080/users/clients
  * Node Name: `http://api-gateway:8080/users/clients`
  * Method: `GET`
  * Parameter: `Cross-Origin-Resource-Policy`
  * Attack: ``
  * Evidence: ``
  * Other Info: ``
* URL: http://api-gateway:8080/users/specialties
  * Node Name: `http://api-gateway:8080/users/specialties`
  * Method: `GET`
  * Parameter: `Cross-Origin-Resource-Policy`
  * Attack: ``
  * Evidence: ``
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians
  * Node Name: `http://api-gateway:8080/users/technicians`
  * Method: `GET`
  * Parameter: `Cross-Origin-Resource-Policy`
  * Attack: ``
  * Evidence: ``
  * Other Info: ``
* URL: http://api-gateway:8080/users/v3/api-docs
  * Node Name: `http://api-gateway:8080/users/v3/api-docs`
  * Method: `GET`
  * Parameter: `Cross-Origin-Resource-Policy`
  * Attack: ``
  * Evidence: ``
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians
  * Node Name: `http://api-gateway:8080/users/technicians ()({firstName,lastName,email,phone,supportLevel,specialties:[],maxConcurrentTickets})`
  * Method: `POST`
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

* URL: http://api-gateway:8080/computeMetadata/v1/
  * Node Name: `http://api-gateway:8080/computeMetadata/v1/`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/latest/meta-data/
  * Node Name: `http://api-gateway:8080/latest/meta-data/`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/metadata/instance
  * Node Name: `http://api-gateway:8080/metadata/instance`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/metadata/v1
  * Node Name: `http://api-gateway:8080/metadata/v1`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/opc/v1/instance/
  * Node Name: `http://api-gateway:8080/opc/v1/instance/`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/opc/v2/instance/
  * Node Name: `http://api-gateway:8080/opc/v2/instance/`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/openstack/latest/meta_data.json
  * Node Name: `http://api-gateway:8080/openstack/latest/meta_data.json`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/users/clients/id
  * Node Name: `http://api-gateway:8080/users/clients/id`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `403`
  * Other Info: ``
* URL: http://api-gateway:8080/users/clients/id/
  * Node Name: `http://api-gateway:8080/users/clients/id/`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `403`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/id
  * Node Name: `http://api-gateway:8080/users/technicians/id`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `403`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/id/
  * Node Name: `http://api-gateway:8080/users/technicians/id/`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `403`
  * Other Info: ``
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
* URL: http://api-gateway:8080/612279213207002673
  * Node Name: `http://api-gateway:8080/612279213207002673`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/users
  * Node Name: `http://api-gateway:8080/users`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/users
  * Node Name: `http://api-gateway:8080/users`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/
  * Node Name: `http://api-gateway:8080/users/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/4175707631692639422
  * Node Name: `http://api-gateway:8080/users/4175707631692639422`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/users/clients/
  * Node Name: `http://api-gateway:8080/users/clients/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/clients/6329750246764833654
  * Node Name: `http://api-gateway:8080/users/clients/6329750246764833654`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/users/clients/actuator/health
  * Node Name: `http://api-gateway:8080/users/clients/actuator/health`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/clients/id
  * Node Name: `http://api-gateway:8080/users/clients/id`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/users/clients/id/
  * Node Name: `http://api-gateway:8080/users/clients/id/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/clients/id/7866967406904867921
  * Node Name: `http://api-gateway:8080/users/clients/id/7866967406904867921`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/users/clients/id/validate
  * Node Name: `http://api-gateway:8080/users/clients/id/validate`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/users/clients/id/validate/
  * Node Name: `http://api-gateway:8080/users/clients/id/validate/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/specialties/
  * Node Name: `http://api-gateway:8080/users/specialties/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/
  * Node Name: `http://api-gateway:8080/users/technicians/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/6019695597804276226
  * Node Name: `http://api-gateway:8080/users/technicians/6019695597804276226`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/available%3Fspecialty=http%253A%252F%252Fwww.google.com%252F&level=N1
  * Node Name: `http://api-gateway:8080/users/technicians/available (level,specialty)`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `400`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/available%3Fspecialty=HARDWARE_MANTENIMIENTO&level=http%253A%252F%252Fwww.google.com
  * Node Name: `http://api-gateway:8080/users/technicians/available (level,specialty)`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/available/
  * Node Name: `http://api-gateway:8080/users/technicians/available/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/id
  * Node Name: `http://api-gateway:8080/users/technicians/id`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/id/
  * Node Name: `http://api-gateway:8080/users/technicians/id/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/id/9074349677234920849
  * Node Name: `http://api-gateway:8080/users/technicians/id/9074349677234920849`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/id/workload
  * Node Name: `http://api-gateway:8080/users/technicians/id/workload`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/id/workload/
  * Node Name: `http://api-gateway:8080/users/technicians/id/workload/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/specialty
  * Node Name: `http://api-gateway:8080/users/technicians/specialty`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/specialty
  * Node Name: `http://api-gateway:8080/users/technicians/specialty`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/specialty/
  * Node Name: `http://api-gateway:8080/users/technicians/specialty/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/specialty/5226866960937974313
  * Node Name: `http://api-gateway:8080/users/technicians/specialty/5226866960937974313`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `400`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/specialty/HARDWARE_MANTENIMIENTO/
  * Node Name: `http://api-gateway:8080/users/technicians/specialty/HARDWARE_MANTENIMIENTO/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/v3
  * Node Name: `http://api-gateway:8080/users/v3`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/users/v3
  * Node Name: `http://api-gateway:8080/users/v3`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/v3/
  * Node Name: `http://api-gateway:8080/users/v3/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/users/v3/53156381180439235
  * Node Name: `http://api-gateway:8080/users/v3/53156381180439235`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/users/v3/api-docs/
  * Node Name: `http://api-gateway:8080/users/v3/api-docs/`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/id/status
  * Node Name: `http://api-gateway:8080/users/technicians/id/status ()({isAvailable})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `404`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/id/status/
  * Node Name: `http://api-gateway:8080/users/technicians/id/status/ ()({isAvailable})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/id/workload
  * Node Name: `http://api-gateway:8080/users/technicians/id/workload ()({delta})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `403`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/id/workload/
  * Node Name: `http://api-gateway:8080/users/technicians/id/workload/ ()({delta})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/clients
  * Node Name: `http://api-gateway:8080/users/clients ()({firstName,lastName,email,phone,companyName,ruc})`
  * Method: `POST`
  * Parameter: ``
  * Attack: ``
  * Evidence: `400`
  * Other Info: ``
* URL: http://api-gateway:8080/users/clients
  * Node Name: `http://api-gateway:8080/users/clients ()({firstName,lastName,email,phone,companyName,ruc})`
  * Method: `POST`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/clients/
  * Node Name: `http://api-gateway:8080/users/clients/ ()({firstName,lastName,email,phone,companyName,ruc})`
  * Method: `POST`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians
  * Node Name: `http://api-gateway:8080/users/technicians ()({firstName,lastName,email,phone,supportLevel,specialties:[],maxConcurrentTickets})`
  * Method: `POST`
  * Parameter: ``
  * Attack: ``
  * Evidence: `400`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians
  * Node Name: `http://api-gateway:8080/users/technicians ()({firstName,lastName,email,phone,supportLevel,specialties:[],maxConcurrentTickets})`
  * Method: `POST`
  * Parameter: ``
  * Attack: ``
  * Evidence: `409`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians
  * Node Name: `http://api-gateway:8080/users/technicians ()({firstName,lastName,email,phone,supportLevel,specialties:[],maxConcurrentTickets})`
  * Method: `POST`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/
  * Node Name: `http://api-gateway:8080/users/technicians/ ()({firstName,lastName,email,phone,supportLevel,specialties:[],maxConcurrentTickets})`
  * Method: `POST`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/clients/id
  * Node Name: `http://api-gateway:8080/users/clients/id ()({firstName,lastName,phone,companyName,ruc,status})`
  * Method: `PUT`
  * Parameter: ``
  * Attack: ``
  * Evidence: `400`
  * Other Info: ``
* URL: http://api-gateway:8080/users/clients/id
  * Node Name: `http://api-gateway:8080/users/clients/id ()({firstName,lastName,phone,companyName,ruc,status})`
  * Method: `PUT`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``
* URL: http://api-gateway:8080/users/clients/id/
  * Node Name: `http://api-gateway:8080/users/clients/id/ ()({firstName,lastName,phone,companyName,ruc,status})`
  * Method: `PUT`
  * Parameter: ``
  * Attack: ``
  * Evidence: `429`
  * Other Info: ``


Instances: 61

### Solution



### Reference



#### CWE Id: [ 388 ](https://cwe.mitre.org/data/definitions/388.html)


#### WASC Id: 20

#### Source ID: 4

### [ Non-Storable Content ](https://www.zaproxy.org/docs/alerts/10049/)



##### Informational (Medium)

### Description

The response contents are not storable by caching components such as proxy servers. If the response does not contain sensitive, personal or user-specific information, it may benefit from being stored and cached, to improve performance.

* URL: http://api-gateway:8080/users/clients/id
  * Node Name: `http://api-gateway:8080/users/clients/id`
  * Method: `DELETE`
  * Parameter: ``
  * Attack: ``
  * Evidence: `DELETE `
  * Other Info: ``
* URL: http://api-gateway:8080/users/clients/id
  * Node Name: `http://api-gateway:8080/users/clients/id`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `no-store`
  * Other Info: ``
* URL: http://api-gateway:8080/users/v3/api-docs
  * Node Name: `http://api-gateway:8080/users/v3/api-docs`
  * Method: `GET`
  * Parameter: ``
  * Attack: ``
  * Evidence: `no-store`
  * Other Info: ``
* URL: http://api-gateway:8080/users/technicians/id/workload
  * Node Name: `http://api-gateway:8080/users/technicians/id/workload ()({delta})`
  * Method: `PATCH`
  * Parameter: ``
  * Attack: ``
  * Evidence: `PATCH `
  * Other Info: ``
* URL: http://api-gateway:8080/users/clients/id
  * Node Name: `http://api-gateway:8080/users/clients/id ()({firstName,lastName,phone,companyName,ruc,status})`
  * Method: `PUT`
  * Parameter: ``
  * Attack: ``
  * Evidence: `PUT `
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


