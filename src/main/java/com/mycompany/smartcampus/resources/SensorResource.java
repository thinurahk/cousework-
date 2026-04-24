/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.smartcampus.resources;
import com.mycompany.smartcampus.database.DataStore;
import com.mycompany.smartcampus.model.Room;
import com.mycompany.smartcampus.model.Sensor;
import com.mycompany.smartcampus.exception.LinkedResourceNotFoundException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 *
 * @author Thinura
 */

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {
private Map<String, Sensor> sensors = DataStore.getSensors();
    private Map<String, Room> rooms = DataStore.getRooms();

    @POST
    public Response addSensor(Sensor sensor) {
        if (sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Sensor ID is required\"}")
                    .build();
        }
        
        if (sensor.getRoomId() == null || sensor.getRoomId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Room ID is required to link a sensor\"}")
                    .build();
        }

        Room assignedRoom = rooms.get(sensor.getRoomId());
        if (assignedRoom == null) {
            throw new LinkedResourceNotFoundException("Cannot link Sensor. Provided roomId '" +
                    sensor.getRoomId() + "' does not exist.");
        }

        sensors.put(sensor.getId(), sensor);

        if (assignedRoom.getSensorIds() == null) {
            assignedRoom.setSensorIds(new ArrayList<>());
        }
        if (!assignedRoom.getSensorIds().contains(sensor.getId())) {
             assignedRoom.getSensorIds().add(sensor.getId());
        }

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }
    
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> allSensors = new ArrayList<>(sensors.values());
        
        if (type != null && !type.trim().isEmpty()) {
            List<Sensor> filteredSensors = allSensors.stream()
                    .filter(sensor -> type.equalsIgnoreCase(sensor.getType()))
                    .collect(Collectors.toList());
            return Response.status(Response.Status.OK).entity(filteredSensors).build();
        }
        
        return Response.status(Response.Status.OK).entity(allSensors).build();
    } 
    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }    
}
