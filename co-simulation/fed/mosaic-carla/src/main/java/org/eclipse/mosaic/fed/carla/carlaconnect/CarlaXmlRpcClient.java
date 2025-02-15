/*
*Copyright (C) 2023 LEIDOS.
*
*Licensed under the Apache License, Version 2.0 (the "License"); you may not
*use this file except in compliance with the License. You may obtain a copy of
*the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing, software
*distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
*WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
*License for the specific language governing permissions and limitations under
*the License.
*/
package org.eclipse.mosaic.fed.carla.carlaconnect;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.eclipse.mosaic.interactions.detector.DetectorRegistration;
import org.eclipse.mosaic.lib.objects.detector.DetectedObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * This is a class that uses xmlrpc to connect with CARLA CDASim Adapter. It includes calls 
 * to dynamically create sensors and get detected objects from created sensors.
 */
public class CarlaXmlRpcClient{

    private static final String CREATE_SENSOR = "create_simulated_semantic_lidar_sensor";
    private static final String GET_DETECTED_OBJECTS = "get_detected_objects";
    private static final String CONNECT ="connect";

    private XmlRpcClient client;
    private final Logger log = LoggerFactory.getLogger(this.getClass());


    public CarlaXmlRpcClient(URL xmlRpcServerUrl) {
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();   
        config.setServerURL(xmlRpcServerUrl);
        // Set reply and connection timeout (both in ms)
        config.setReplyTimeout(6000);
        config.setConnectionTimeout(10000);
        client = new XmlRpcClient();
        client.setConfig(config);
    }


    public void connect(int retryAttempts) throws XmlRpcException, InterruptedException{
        boolean connected = false;
        int currentAttempt = 1;
        while( !connected && retryAttempts >= currentAttempt ) {
            try {
                log.info("Attempting to connect to CARLA CDA Sim Adapter ... ");
                Object[] params = new Object[]{};
                client.execute(CONNECT, params);
                connected = true;
            }
            catch(XmlRpcException e) {
                log.error("Connection attempt {} to connect to CARLA CDA Sim Adapter failed!", currentAttempt, e);
                // Sleep for 1 second betweeen attempts
                Thread.sleep(1000);
                currentAttempt++;
            }
        } 
        if (!connected) {
            throw new XmlRpcException("Failed to connect to XML RPC Server with config " + client.getConfig() + " !");
        }
        log.info("Connected successfully to CARLA CDA Sim Adapter!");    
    }
    /**
     * Calls CARLA CDA Sim Adapter create_sensor XMLRPC method and logs sensor ID of created sensor.
     * @param registration DetectorRegistration interaction used to create sensor.
     * @throws XmlRpcException if XMLRPC call fails or connection is lost.
     */
    public void createSensor(DetectorRegistration registration) throws XmlRpcException{
        List<Double> location = Arrays.asList(registration.getDetector().getLocation().getX(), registration.getDetector().getLocation().getY(), registration.getDetector().getLocation().getZ());
        List<Double> orientation = Arrays.asList(registration.getDetector().getOrientation().getPitch(), registration.getDetector().getOrientation().getRoll(), registration.getDetector().getOrientation().getYaw());
        Object[] params = new Object[]{registration.getInfrastructureId(), registration.getDetector().getSensorId(), location, orientation};
        Object result = client.execute(CREATE_SENSOR, params);
        log.info((String)result);
      
    }
    /**
     * Calls CARLA CDA Sim Adapter get_detected_objects XMLRPC method and returns an array of DetectedObject.
     * @param infrastructureId String infrastructure ID of sensor to get detections from.
     * @param sensorId String sensor ID of sensor to get detections from
     * @return DetectedObject[] from given sensor.
     * @throws XmlRpcException if XMLRPC call fails or connection is lost.
     */
    public DetectedObject[] getDetectedObjects(String infrastructureId ,String sensorId) throws XmlRpcException{
        Object[] params = new Object[]{infrastructureId, sensorId};
        Object result = client.execute(GET_DETECTED_OBJECTS, params);
        log.debug("Detections from infrastructure {} sensor {} : {}", infrastructureId, sensorId, result);
        String jsonResult = (String)result;
        Gson gson = new Gson();
        return gson.fromJson(jsonResult,DetectedObject[].class);
       
    }
}
