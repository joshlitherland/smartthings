/**
 *  Raspberry Pi
 *
 *  Copyright 2015 Nicholas Wilde
 *
 *  Monitor your Raspberry Pi using SmartThings and BerryIO SmartThings <https://github.com/nicholaswilde/berryio-smartthings>
 *
 *  Contributors:
 *  Thanks to NewHorizons for BerryIO
 *  Thanks to Ledridge for the SmartThings addition to BerryIO
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
import groovy.json.JsonSlurper
import com.google.common.base.Splitter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


preferences {
        input("ip", "string", title:"IP Address", defaultValue: "192.168.5.190" ,required: true, displayDuringSetup: true)
        input("port", "string", title:"Port", defaultValue: "80" , required: true, displayDuringSetup: true)
        input("username", "string", title:"Username", defaultValue: "pi" , required: true, displayDuringSetup: true)
        input("password", "password", title:"Password", defaultValue: "raspberry" , required: true, displayDuringSetup: true)
}

metadata {
	definition (name: "Raspberry Pi", namespace: "nicholaswilde", author: "Nicholas Wilde") {
		capability "Polling"
		capability "Refresh"
		capability "Temperature Measurement"
        capability "Switch"
        capability "Sensor"
        capability "Actuator"
        capability "Contact Sensor"
        
        attribute "cpuPercentage", "string"
        attribute "memory", "string"
        attribute "diskUsage", "string"
        
        command "restart"
        command "rgb_blinktest"
	}

	simulator {
		// TODO: define status and reply messages here
	}
    
    tiles {
		valueTile("temperature", "device.temperature", width: 1, height: 1) {
            state "temperature", label:'${currentValue}° CPU', unit: "F",
            backgroundColors:[
                [value: 25, color: "#153591"],
                [value: 35, color: "#1e9cbb"],
                [value: 47, color: "#90d2a7"],
                [value: 59, color: "#44b621"],
                [value: 67, color: "#f1d801"],
                [value: 76, color: "#d04e00"],
                [value: 77, color: "#bc2323"]
            ]
        }
        standardTile("button", "device.switch", width: 1, height: 1, canChangeIcon: true) {
			state "off", label: 'Off', icon: "st.Electronics.electronics18", backgroundColor: "#ffffff", nextState: "on"
			state "on", label: 'On', icon: "st.Electronics.electronics18", backgroundColor: "#79b821", nextState: "off"
		}
        valueTile("cpuPercentage", "device.cpuPercentage", inactiveLabel: false) {
        	state "default", label:'${currentValue}% CPU', unit:"Percentage",
            backgroundColors:[
                [value: 31, color: "#153591"],
                [value: 44, color: "#1e9cbb"],
                [value: 59, color: "#90d2a7"],
                [value: 74, color: "#44b621"],
                [value: 84, color: "#f1d801"],
                [value: 95, color: "#d04e00"],
                [value: 96, color: "#bc2323"]
            ]
        }
        valueTile("memory", "device.memory", width: 1, height: 1) {
        	state "default", label:'${currentValue} MB', unit:"MB",
            backgroundColors:[
                [value: 353, color: "#153591"],
                [value: 287, color: "#1e9cbb"],
                [value: 210, color: "#90d2a7"],
                [value: 133, color: "#44b621"],
                [value: 82, color: "#f1d801"],
                [value: 26, color: "#d04e00"],
                [value: 20, color: "#bc2323"]
            ]
        }
        valueTile("diskUsage", "device.diskUsage", width: 1, height: 1) {
        	state "default", label:'${currentValue}% Disk', unit:"Percent",
            backgroundColors:[
                [value: 31, color: "#153591"],
                [value: 44, color: "#1e9cbb"],
                [value: 59, color: "#90d2a7"],
                [value: 74, color: "#44b621"],
                [value: 84, color: "#f1d801"],
                [value: 95, color: "#d04e00"],
                [value: 96, color: "#bc2323"]
            ]
        }
        standardTile("contact", "device.contact", width: 1, height: 1) {
			state("closed", label:'${name}', icon:"st.contact.contact.closed", backgroundColor:"#79b821", action: "open")
			state("open", label:'${name}', icon:"st.contact.contact.open", backgroundColor:"#ffa81e", action: "close")
		}
        standardTile("restart", "device.restart", inactiveLabel: false, decoration: "flat") {
        	state "default", action:"restart", label: "Restart", displayName: "Restart"
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
        	state "default", action:"refresh.refresh", icon: "st.secondary.refresh"
        }
        standardTile("rgbTest", "device.rgb_blinktest", inactiveLabel: false, decoration: "flat") {
        	state "default", action:"rgb_blinktest", icon: "st.Entertainment.entertainment10"
        }
        main "button"
        details(["button", "temperature", "cpuPercentage", "memory" , "diskUsage", "contact", "restart", "refresh", "rgbTest"])
    }
}

// ------------------------------------------------------------------

// parse events into attributes
def parse(String description) {
	log.warn("Parse parse parse!!!")
    def map = [:]
    def descMap = parseDescriptionAsMap(description)
    log.debug "descMap: ${descMap}"
    
    def body = new String(descMap["body"].decodeBase64())
    log.debug "body: ${body}"
    
    def slurper = new JsonSlurper()
    def result = slurper.parseText(body)
    
    log.debug "result: ${result}"

	if (result){
    	log.debug "Computer is up"
   		sendEvent(name: "switch", value: "on")
    }
    
    log.debug "check temp..."
    if (result.containsKey("cpu_temp")) {
    	log.debug "temp: ${result.cpu_temp.toDouble().round()}"
        log.debug "temp: ${celsiusToFahrenheit(result.cpu_temp.toDouble().round())} F"
    	sendEvent(name: "temperature", value: celsiusToFahrenheit(result.cpu_temp.toDouble().round()))
    }
    
    if (result.containsKey("cpu_perc")) {
    	log.debug "cpu_perc: ${result.cpu_perc}"
        sendEvent(name: "cpuPercentage", value: result.cpu_perc)
    }
    
    if (result.containsKey("mem_avail")) {
    	log.debug "mem_avail: ${result.mem_avail.toDouble().round()}"
        sendEvent(name: "memory", value: result.mem_avail.toDouble().round())
    }
    if (result.containsKey("disk_usage")) {
    	log.debug "disk_usage: ${result.disk_usage.toDouble().round()}"
        sendEvent(name: "diskUsage", value: result.disk_usage.toDouble().round())
    }
  	if (result.containsKey("gpio_value_17")) {
    	log.debug "gpio_value_17: ${result.gpio_value_17.toDouble().round()}"
        if (result.gpio_value_17.contains("0")){
        	log.debug "gpio_value_17: open"
            sendEvent(name: "contact", value: "open")
        } else {
        	log.debug "gpio_value_17: closed"
            sendEvent(name: "contact", value: "closed")
        }
    }
  	
}

// handle commands
def poll() {
    log.info "Executing 'poll'"
    sendEvent(name: "switch", value: "off")
    def result = getRPiData();
    log.debug("Poll: Got response back: ${result}");
    return result;
}

def refresh() {
	log.info "Executing 'refresh'"
	sendEvent(name: "switch", value: "off")
    def result = getRPiData();
    log.debug("Refresh: Got response back: ${result}");
    return result;
}

def restart(){
	log.debug "Restart was pressed"
    sendEvent(name: "switch", value: "off")
    def uri = "/api_command/reboot"
    return postAction(uri);
}

def rgb_blinktest() {
	log.debug "Testing RGB Hat"
    def uri = "/api_command/rgb_blinktest"
    return postAction(uri);
}

// Get CPU percentage reading
def getRPiData() {
	def uri = "/api_command/smartthings"
    def result = postAction(uri);
    log.debug("GetRPiData: Got response back: ${result}");
    return result;
}

// ------------------------------------------------------------------

def postAction(uri){
	//setDeviceNetworkId("192.168.5.190","80")
	//setDeviceNetworkId(settings.ip, settings.port)
  
	def userpass = encodeCredentials(settings.username, settings.password)
    //log.debug("userpass: " + userpass) 

	def headers = getHeader(userpass)
	//log.debug("headders: " + headers) 
    def request = [:]
    request.put("method","POST");
    request.put("path",uri);
    request.put("headers",headers);
    def dni="c0a805be:0050";
	def result = new physicalgraph.device.HubAction(request,dni)
    log.debug("Executing hubAction on " + getHostAddress())
    return result;
}

// ------------------------------------------------------------------
// Helper methods
// ------------------------------------------------------------------

def parseDescriptionAsMap(description) {
	description.split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
}


def toAscii(s){
        StringBuilder sb = new StringBuilder();
        String ascString = null;
        long asciiInt;
                for (int i = 0; i < s.length(); i++){
                    sb.append((int)s.charAt(i));
                    sb.append("|");
                    char c = s.charAt(i);
                }
                ascString = sb.toString();
                asciiInt = Long.parseLong(ascString);
                return asciiInt;
    }

def encodeCredentials(username, password){
	log.debug "Encoding credentials"
	//def userpassascii = "${username}:${password}"
    def userpassascii = "pi:raspberry"
    def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    log.debug "ASCII credentials are ${userpassascii}"
    log.debug "Credentials are ${userpass}"
    return userpass
}

def getHeader(userpass){
	log.debug "Getting headers"
    def headers = [:]
    headers.put("HOST", getHostAddress())
    headers.put("Authorization", userpass)
    //log.debug "Headers are ${headers}"
    return headers
}

def delayAction(long time) {
	new physicalgraph.device.HubAction("delay $time")
}

def setDeviceNetworkId(ip,port){
  	def iphex = convertIPtoHex(ip)
  	def porthex = convertPortToHex(port)
  	//device.deviceNetworkId = "$iphex:$porthex"
    def dni="c0a805be:0050";
    device.deviceNetworkId = dni;
  	log.info "Device Network Id set to ${iphex}:${porthex}"
}

def getHostAddress() {
	//return "${ip}:${port}"
    return "192.168.5.190:80"
}

def String convertIPtoHex(ipAddress) { 
	log.debug "convertIPtoHex(${ipAddress})"
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
    return hex

}

def String convertPortToHex(port) {
	log.debug "convertPorttoHex(${port})"
	String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport
}