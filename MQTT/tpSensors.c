#include "contiki.h"
#include "sys/etimer.h"
#include "sys/ctimer.h"
#include "os/sys/log.h"
#include "project-conf.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-ds6.h"
#include "net/ipv6/uip-ds6-route.h"
#include "net/ipv6/uip-ds6-nbr.h" 
#include "net/ipv6/simple-udp.h" 
#include "button-hal.h" 
#include <stdlib.h>
#include <unistd.h>
#include "mqtt.h"

#define LOG_MODULE "Temperature & Power"
#ifdef  MQTT_CLIENT_CONF_LOG_LEVEL
#define LOG_LEVEL MQTT_CLIENT_CONF_LOG_LEVEL
#else
#define LOG_LEVEL LOG_LEVEL_DBG
#endif

/*---------------------------------------------------------------------------*/
// Area code
#define SUBZONE 1

#define NTOPICS 2

/*---------------------------------------------------------------------------*/

// TEMPERATURE CONSIGLIATE PER LA FUNZIONE CORRETTA DI UN DATACENTER [18.00, 27.00]
/*---------------------------------------------------------------------------*/
// Temperature upper and lower bounds
#define UP_TEMP   4000
#define DOWN_TEMP 1500
#define RAND_UP_TEMP 2500
#define RAND_DOWN_TEMP 1900

// static int temp = (rand() % (UP_TEMP - DOWN_TEMP + 1)) + DOWN_TEMP;
static int temp = 0;

static bool water_on = false;
/*---------------------------------------------------------------------------*/
/*---------------------------------------------------------------------------*/
// Consumed power variable
int cp = 0;

// POTENZE CONSIGLIATE COME VALORI DI CAMBIO STATO 100kW (OVERLOAD) E 40kW (UNDERUTILIZED)

// Consumed power upper and lower bounds
#define UP_POW   150000 // 150kW
#define DOWN_POW 20000  // 20kW
#define RAND_UP_POW 80000 // 80kW
#define RAND_DOWN_POW 50000 // 50kW

static struct ctimer send_after_5;

static bool overload = false;
static bool power_off = false;
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
// MQTT broker address.
#define MQTT_CLIENT_BROKER_IP_ADDR "fd00::1"

static const char *broker_ip = MQTT_CLIENT_BROKER_IP_ADDR;

// Default config values
#define DEFAULT_BROKER_PORT         1883
// #define DEFAULT_PUBLISH_INTERVAL    (30 * CLOCK_SECOND)
#define PUBLISH_INTERVAL	          (10 * CLOCK_SECOND)

// Maximum TCP segment size for outgoing segments of our socket
#define MAX_TCP_SEGMENT_SIZE    32
#define CONFIG_IP_ADDR_STR_LEN  64

static struct mqtt_message *msg_ptr = 0;

static struct mqtt_connection conn;

// Buffers for Client ID and Topics.

#define BUFFER_SIZE 64

static char client_id[BUFFER_SIZE];
static char* pub_topic[NTOPICS] = {"temperature", "consumed_power"};
static char sub_topic[BUFFER_SIZE];

// Application level buffer
#define APP_BUFFER_SIZE 1024
static char app_buffer[2][APP_BUFFER_SIZE] = {0};

// Periodic timer to check the state of the MQTT client
#define STATE_MACHINE_PERIODIC     (CLOCK_SECOND >> 1)
static struct etimer periodic_timer;

// Various states
static uint8_t state;

#define STATE_INIT    		      0	// initial state
#define STATE_NET_OK    	      1	// Network is initialized
#define STATE_CONNECTING      	2	// Connecting to MQTT broker
#define STATE_CONNECTED       	3	// Connection successful
#define STATE_SUBSCRIBED      	4	// Topics subscription done
#define STATE_DISCONNECTED    	5	// Disconnected from MQTT broker

/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
PROCESS_NAME(sensRead);
PROCESS(sensRead, "Reads the temperature and the consumed power and then sends the info to the BR.");
AUTOSTART_PROCESSES(&sensRead);
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
static bool have_connectivity(void) {
	return !(uip_ds6_get_global(ADDR_PREFERRED) == NULL || uip_ds6_defrt_choose() == NULL);
}

// Function called for handling an incoming message
static void msg_handler(const char *topic, uint16_t topic_len, const uint8_t *chunk, uint16_t chunk_len) {
	
  char areaTopic[10];
  sprintf(areaTopic, "SEN%d", SUBZONE);
  
  LOG_INFO("Message received: topic='%s' (len=%u), chunk_len=%u\n", topic, topic_len, chunk_len);

	if(strcmp(topic, areaTopic) != 0) {
		LOG_ERR("Topic not valid!\n");
		return;
	}
	
	LOG_INFO("Received Actuator command\n");
	if(strcmp((const char*) chunk, "WON") == 0) {
		LOG_INFO("Turn ON watercooling in area %d.\n", SUBZONE);
		water_on = true;
	} else if(strcmp((const char*) chunk, "WOFF") == 0)  {
		LOG_INFO("Turn OFF watercooling in area %d.\n", SUBZONE);	
		water_on = false;
	} else if(strcmp((const char*) chunk, "POFF") == 0)  {
		LOG_INFO("Turn OFF power in area %d.\n", SUBZONE);	
    power_off = true;
	} else if (strcmp((const char*) chunk, "PON") == 0) {
    LOG_INFO("Overload ended in area %d.\n", SUBZONE);
    overload = false;
  } else if (strcmp((const char*) chunk, "OL") == 0) {
    LOG_WARN("Overload in area %d.\n", SUBZONE);
    overload = true;
  }
  
}

static void mqtt_event(struct mqtt_connection *m, mqtt_event_t event, void *data) {

  switch(event) {
    case MQTT_EVENT_CONNECTED: {
      // Connection completed
      LOG_INFO("MQTT connection completed!\n");
      state = STATE_CONNECTED;
      break;
    }
    case MQTT_EVENT_DISCONNECTED: {
      // Disconnection occurred
      LOG_INFO("MQTT connection disconnected for the following reason: %u.\n", *((mqtt_event_t *) data));
      state = STATE_DISCONNECTED;
      process_poll(&sensRead);
      break;
    }
    case MQTT_EVENT_PUBLISH: {
      // Notification on a subscribed topic received
      // Data variable points to the MQTT message received
      msg_ptr = data;
      msg_handler(msg_ptr->topic, strlen(msg_ptr->topic), msg_ptr->payload_chunk, msg_ptr->payload_length); 
      break;
    }
    case MQTT_EVENT_SUBACK: {
      // Subscription successful
      #if MQTT_311
        mqtt_suback_event_t *suback_event = (mqtt_suback_event_t *)data;
        if(suback_event->success) 
          LOG_INFO("The application has subscribed to the topic!\n");
        else
          LOG_ERR("The application failed to subscribe to topic (ret code %x).\n", suback_event->return_code);
        #else
          LOG_INFO("The application has subscribed to the topic!\n");
        #endif
        break;
      break;
    }
    case MQTT_EVENT_UNSUBACK: {
      // Subscription canceled
      LOG_INFO("Subscription canceled succesfully!\n");
      break;
    }
    case MQTT_EVENT_PUBACK: {
      // Publishing completed
      LOG_INFO("Publishing completed!\n");
      break;
    }
    default: {
			LOG_INFO("The application got an unhandled MQTT event: %i\n", event);
			break;
    }
  }
}

/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
// Function that changes the temperature according to if the water cooling is active
static void changeTemp(){

  // Generates a number to represent the length of the step
  int randomIncrement = (rand() % (100 + 1) + 100); // [100 , 200]

  // Increment the temperature by a random amount
  temp += randomIncrement;

  // If water cooling system is active i have to detract more temperature
  if(water_on)
    temp -= (rand() % (150 + 1) + 150); //[150 , 300]
  else
    temp -= (rand() % (150 + 1));       //[0 , 150]

  // Assuring the temperature doesn't go out of the range [DOWN_TEMP, UP_TEMP]
  if(temp > UP_TEMP)
    temp = UP_TEMP;
  else if(temp < DOWN_TEMP)
    temp = DOWN_TEMP;
}

// Function that changes the consumed power randomly
static void changeCp(){

  int randomIncrement = (rand() % (5) - 2); // {-2, -1, 0, 1, 2}

  // Generates a number to represent the length of the step
  int randomStep = (rand() % (2000 + 1) + 1000); // [1000 , 3000]

  // Increment the power by a random amount
  cp += randomIncrement * randomStep;

  int num = rand() % 3;
  //TODO DO THE OL PLEASE
  // If the area is in overload then some processes will be moved to consume less power
  if(overload)
    cp -= (num != 0) ? num * 5000: 1000; // {1000, 5000, 10000}

  // // If water cooling system is active the system consumes more power
  // if(water_on)
  //   cp += (rand() % (2000 + 1) + 1000); //[1000 , 3000]
  // else
  //   cp += (rand() % (2000 + 1) + 500);  //[500 , 2500]

  if(cp > UP_POW)
    cp = UP_POW;
  else if(cp < DOWN_POW)
    cp = DOWN_POW;
}
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/

static void sendPow(void*ptr){
  int cpSend;

  if(water_on)
    cpSend = cp + 2000;
  else
    cpSend = cp + 1000;

  LOG_INFO("Current consumed power from area %d: %d\n", SUBZONE, cpSend);

  memset(app_buffer[1], 0, APP_BUFFER_SIZE);
  sprintf(app_buffer[1], "{\"subzone\": %d, \"consumed_power\": %d}", SUBZONE, cpSend);

  mqtt_publish(&conn, NULL, pub_topic[1], (uint8_t *)app_buffer[1],
    strlen(app_buffer[1]), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);
}

static void readnSend(){
  
  LOG_INFO("Current temperature from area %d: %d\n", SUBZONE, temp);

  memset(app_buffer[0], 0, APP_BUFFER_SIZE);
  sprintf(app_buffer[0], "{\"subzone\": %d, \"temperature\": %d}", SUBZONE, temp);

  mqtt_publish(&conn, NULL, pub_topic[0], (uint8_t *)app_buffer[0],
    strlen(app_buffer[0]), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);

  ctimer_set(&send_after_5, CLOCK_SECOND * 5, &sendPow, NULL);
}
/*---------------------------------------------------------------------------*/

PROCESS_THREAD(sensRead, ev, data) {

  PROCESS_BEGIN();
  temp = (rand() % (RAND_UP_TEMP - RAND_DOWN_TEMP + 1)) + RAND_DOWN_TEMP;
  cp = (rand() % (RAND_UP_POW - RAND_DOWN_POW + 1)) + RAND_DOWN_POW;

  static button_hal_button_t *btn;
	static mqtt_status_t status;
	static char broker_address[CONFIG_IP_ADDR_STR_LEN];
	
	btn = button_hal_get_by_index(0);
	if(btn == NULL) {
		LOG_ERR("Unable to find the default button... exit\n");
		// goto exit;
    exit(1);
	}

	LOG_INFO("Starting...\n");
	
	// Initialize the ClientID as MAC address
	snprintf(client_id, BUFFER_SIZE, "%02x%02x%02x%02x%02x%02x",
		     linkaddr_node_addr.u8[0], linkaddr_node_addr.u8[1],
		     linkaddr_node_addr.u8[2], linkaddr_node_addr.u8[5],
		     linkaddr_node_addr.u8[6], linkaddr_node_addr.u8[7]);
    
  // Broker registration					 
	mqtt_register(&conn, &sensRead, client_id, mqtt_event, MAX_TCP_SEGMENT_SIZE);
			
	state=STATE_INIT;
				    
	// Initialize periodic timer to check the status 
	etimer_set(&periodic_timer, PUBLISH_INTERVAL);

  while(1) {

    if(power_off) {
      
      etimer_stop(&periodic_timer);
      etimer_set(&periodic_timer, CLOCK_SECOND * 20);
      ctimer_stop(&send_after_5);

      mqtt_disconnect(&conn);

      temp = (rand() % (RAND_UP_TEMP - RAND_DOWN_TEMP + 1)) + RAND_DOWN_TEMP;
      cp = (rand() % (RAND_UP_POW - RAND_DOWN_POW + 1)) + RAND_DOWN_POW;

      PROCESS_WAIT_EVENT_UNTIL(ev == PROCESS_EVENT_TIMER && data == &periodic_timer);

      LOG_INFO("Starting...\n");

      state=STATE_INIT;

      etimer_set(&periodic_timer, PUBLISH_INTERVAL);

      power_off = false;
    }

    PROCESS_YIELD();

		if(!((ev == PROCESS_EVENT_TIMER && data == &periodic_timer) || ev == PROCESS_EVENT_POLL))
			continue;
		
		switch(state) {		  			  
      case STATE_INIT: {
        LOG_INFO("Initialization...\n");
        if(have_connectivity())
          state = STATE_NET_OK;
          // no break
        else
          break;
      }
      case STATE_NET_OK: {
        LOG_INFO("Connecting to MQTT server...\n"); 

        memcpy(broker_address, broker_ip, strlen(broker_ip));
      
        mqtt_connect(&conn, broker_address, DEFAULT_BROKER_PORT,
            (/*DEFAULT_*/PUBLISH_INTERVAL * 3) / CLOCK_SECOND,
            MQTT_CLEAN_SESSION_ON);

        state = STATE_CONNECTING;
        break;
      }
      case STATE_CONNECTED: {
        // Subscribe to a topic
        sprintf(sub_topic,"SEN%d", SUBZONE);
        status = mqtt_subscribe(&conn, NULL, sub_topic, MQTT_QOS_LEVEL_0);
        LOG_INFO("Subscribing to a topic...\n");
        if(status == MQTT_STATUS_OUT_QUEUE_FULL) {
          LOG_ERR("Tried to subscribe but command queue was full!\n");
          PROCESS_EXIT();
        }
        state = STATE_SUBSCRIBED;
        // no break
      }
      case STATE_SUBSCRIBED:	{
        
        // Simulating the behaviour of a sensor			
        changeTemp();

        changeCp();

        readnSend();

        break;
      } 
      case STATE_DISCONNECTED: {
        LOG_ERR("Disconnected from MQTT broker!\n");	
        state = STATE_INIT;
        break;
      }
    }
    
    etimer_set(&periodic_timer, PUBLISH_INTERVAL);
  }

	PROCESS_END();
}