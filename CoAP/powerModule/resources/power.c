#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"
#include "project-conf.h"
#include "sys/ctimer.h"

/* Log configuration */
#include "sys/log.h"

#define LOG_MODULE "Power module"
#ifdef  RES_CONF_LOG_LEVEL
#define LOG_LEVEL RES_CONF_LOG_LEVEL
#else
#define LOG_LEVEL LOG_LEVEL_DBG
#endif

static void power_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_power_module,
         "title=\"Power module\";rt=\"Control\"",
         NULL,
         NULL,
         power_put_handler,
         NULL);

bool power_off = false;

// Global variable to start and stop the blinking
bool start = false;

static struct ctimer blink_timer;
static struct ctimer off_timer;

static void blink(void *ptr) {
	if(!start)
		return;
	
	ctimer_reset(&blink_timer);

	leds_toggle(LEDS_GREEN);
}

static void pow_on(void *ptr) {
	power_off = false;
}

static void power_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	size_t len = 0;
	const char *text = NULL;
		
	if(power_off)
		return;

	len = coap_get_payload(request, (const uint8_t**)&text);
	if(len <= 0)
		goto error;
	
	leds_off(LEDS_RED);

	if(strncmp(text, "POFF", len) == 0) {
		power_off = true;
		start = false;
		leds_set(LEDS_RED);
		LOG_INFO("POWER OFF\n");
		ctimer_set(&off_timer, CLOCK_SECOND * 20, pow_on, NULL);
	} else if(strncmp(text, "PON", len) == 0) {
		power_off = false;
		start = false;
		leds_set(LEDS_GREEN);
		LOG_INFO("POWER ON\n");
	} else if(strncmp(text, "OL", len) == 0) {
		power_off = false;
		start = true;
		ctimer_set(&blink_timer, CLOCK_SECOND * 0.5, blink, NULL);
		LOG_WARN("OVERLOAD\n");
	}
	else
		goto error;

	return;
error:
	coap_set_status_code(response, BAD_REQUEST_4_00);
}