/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.smartcampus.resources;
import com.mycompany.smartcampus.database.DataStore;
import com.mycompany.smartcampus.model.Sensor;
import com.mycompany.smartcampus.model.SensorReading;
import com.mycompany.smartcampus.exception.SensorUnavailableException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author Thinura
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {
    
    private String sensorId;
    private Map<String, Sensor> sensors = DataStore.getSensors();
    private Map<String, List<SensorReading>> sensorReadings = DataStore.getSensorReadings();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }
    @GET
    public Response getReadings() {
        Sensor parentSensor = sensors.get(sensorId);
        if (parentSensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\":\"Sensor not found\"}")
                           .build();
        }

        List<SensorReading> readings = sensorReadings.getOrDefault(sensorId, new ArrayList<>());
        return Response.status(Response.Status.OK).entity(readings).build();
    }

    @POST
    public Response addReading(SensorReading reading) {
        Sensor parentSensor = sensors.get(sensorId);
        if (parentSensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("{\"error\":\"Sensor not found\"}")
                           .build();
        }
        if ("MAINTENANCE".equalsIgnoreCase(parentSensor.getStatus())) {
            throw new SensorUnavailableException("Sensor " + sensorId + " is currently in MAINTENANCE mode and "
                    + "cannot accept readings.");
        }
        if (reading.getId() == null || reading.getId().trim().isEmpty()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() <= 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        sensorReadings.putIfAbsent(sensorId, new ArrayList<>());
        sensorReadings.get(sensorId).add(reading);

        parentSensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
