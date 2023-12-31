#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"
#include "coap-blocking-api.h"
#include "os/sys/log.h"
#include "project-conf.h"
#include "os/dev/button-hal.h"

#include "node-id.h"
#include "net/ipv6/simple-udp.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-ds6.h"
#include "net/ipv6/uip-debug.h"
#include "routing/routing.h"

#define LOG_MODULE "Power"
#ifdef  COAP_CLIENT_CONF_LOG_LEVEL
#define LOG_LEVEL COAP_CLIENT_CONF_LOG_LEVEL
#else
#define LOG_LEVEL LOG_LEVEL_DBG
#endif

#define SERVER_EP "coap://[fd00::1]:5683"
#define CONNECTION_TRY_INTERVAL 1
#define REGISTRATION_TRY_INTERVAL 1
#define SIMULATION_INTERVAL 1

#define INTERVAL_BETWEEN_CONNECTION_TESTS 1

extern coap_resource_t res_power_module;

static struct etimer connectivity_timer;

/*---------------------------------------------------------------------------*/
// Area code
int subzone = 1;
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
PROCESS_NAME(powerModule);
PROCESS(powerModule, "Deactivates the interested subzone.");
AUTOSTART_PROCESSES(&powerModule);
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
static bool is_connected() {
	if(NETSTACK_ROUTING.node_is_reachable()) {
		LOG_INFO("The Border Router is reachable.\n");
		return true;
  	}

	return false;
}

/*---------------------------------------------------------------------------*/


PROCESS_THREAD(powerModule, ev, data){

    PROCESS_BEGIN();

    static button_hal_button_t *btn;

    btn = button_hal_get_by_index(0);
	if(btn == NULL) {
		LOG_ERR("Unable to find the default button... exit\n");
        exit(1);
	}

    PROCESS_WAIT_EVENT_UNTIL(ev == button_hal_press_event);

    leds_set(LEDS_RED);

    LOG_INFO("Inizialization power module...\n");
    coap_activate_resource(&res_power_module, "power_module"); 

    // Trying to connect to the border router
    etimer_set(&connectivity_timer, CLOCK_SECOND * INTERVAL_BETWEEN_CONNECTION_TESTS);
    PROCESS_WAIT_UNTIL(etimer_expired(&connectivity_timer));
    LOG_INFO("Waiting for connection with the Border Router...\n");

    while(!is_connected()) {
        etimer_reset(&connectivity_timer);
        PROCESS_WAIT_UNTIL(etimer_expired(&connectivity_timer));
    }
    leds_set(LEDS_BLUE);

    PROCESS_END();
}