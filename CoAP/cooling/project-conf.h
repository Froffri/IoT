#ifndef PROJECT_CONF_H_
#define PROJECT_CONF_H_

// Log level settings
// #define COAP_CLIENT_CONF_LOG_LEVEL LOG_LEVEL_ERROR   // Log level of the COAP client
// #define RES_CONF_LOG_LEVEL LOG_LEVEL_ERROR           // Log level of the COAP client

// Set the maximum number of CoAP concurrent transactions:
#undef COAP_MAX_OPEN_TRANSACTIONS
#define COAP_MAX_OPEN_TRANSACTIONS 4

/* Save some memory for the sky platform. */
#undef NBR_TABLE_CONF_MAX_NEIGHBORS
#define NBR_TABLE_CONF_MAX_NEIGHBORS 10

#undef UIP_CONF_MAX_ROUTES
#define UIP_CONF_MAX_ROUTES 10

#undef UIP_CONF_BUFFER_SIZE
#define UIP_CONF_BUFFER_SIZE 240

#endif /* PROJECT_CONF_H_ */