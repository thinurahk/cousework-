/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.smartcampus.resources;
import com.mycompany.smartcampus.model.Room;
import com.mycompany.smartcampus.database.DataStore;
import com.mycompany.smartcampus.exception.RoomNotEmptyException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Thinura
 */

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorRoomResource {
    private Map<String, Room> rooms = DataStore.getRooms();
    
    @GET
    public List<Room> getAllRooms(){
        return new ArrayList<>(rooms.values());
    }
    
    @POST
    public Response addRoom(Room room){
        if(room.getId() == null || room.getId().trim().isEmpty()){
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Room ID is required\"}").build();
        }
        rooms.put(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Room not found\"}")
                    .build();
        }
        return Response.status(Response.Status.OK).entity(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Room not found\"}")
                    .build();
        }
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Cannot delete room " + roomId + 
                    ". It is currently occupied by active hardware sensors.");
        }        
        rooms.remove(roomId);
        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
