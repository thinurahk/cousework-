/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.smartcampus.database;
import com.mycompany.smartcampus.model.Room;
import com.mycompany.smartcampus.model.Sensor;
import com.mycompany.smartcampus.model.SensorReading;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Thinura
 */
public class DataStore {
    private static Map<String, Room> rooms = new HashMap<>();   
    private static Map<String, Sensor> sensors = new HashMap<>();
    private static Map<String, List<SensorReading>> sensorReadings = new HashMap<>();

    static{
        Room r1 = new Room("LIB-110","Libary Study Space",50);
        rooms.put(r1.getId(), r1);
        
        Room r2 = new Room("LIB-101","Computer Lab",30);
        rooms.put(r2.getId(), r2);        
        
    }
    public static Map<String, Room> getRooms() {
        return rooms;
    }

    public static Map<String, Sensor> getSensors() {
        return sensors;
    }

    public static Map<String, List<SensorReading>> getSensorReadings() {
        return sensorReadings;
    }   
}
