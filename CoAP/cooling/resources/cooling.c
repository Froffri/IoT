#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"
#include "project-conf.h"

/* Log configuration */
#include "sys/log.h"

#define LOG_MODULE "Water Cooling"
#ifdef  RES_CONF_LOG_LEVEL
#define LOG_LEVEL RES_CONF_LOG_LEVEL
#else
#define LOG_LEVEL LOG_LEVEL_DBG
#endif

static void cooling_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_cooling_module,
         "title=\"Water cooling\";rt=\"Control\"",
         NULL,
         NULL,
         cooling_put_handler,
         NULL);

static bool water_on = false;
static bool power_off = false;

static struct ctimer off_timer;

static void pow_on(void *ptr){
	leds_set(LEDS_RED);
	power_off = false;
}

static void cooling_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	size_t len = 0;
	const char *text = NULL;

	if(power_off)
		return;
		
	len = coap_get_payload(request, (const uint8_t**)&text);
	if(len <= 0)
		goto error;
	
	if(strncmp(text, "WON", len) == 0) {
		water_on = true;
		leds_set(LEDS_GREEN);
		LOG_INFO("WATER ON\n");
	} else if(strncmp(text, "WOFF", len) == 0) {
		water_on = false;
		leds_set(LEDS_RED);
		LOG_INFO("WATER OFF\n");
	} else if(strncmp(text, "POFF", len) == 0) {
		water_on = false;
		power_off = true;
		LOG_INFO("POWER OFF\n");
		ctimer_set(&off_timer, CLOCK_SECOND * 20, pow_on, NULL);
	}
	else
		goto error;

	return;
error:
	coap_set_status_code(response, BAD_REQUEST_4_00);
}