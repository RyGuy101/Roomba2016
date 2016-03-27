package org.jointheleague.iaroc;

/**************************************************************************
 * AndroidStudio WhiteLiner 2014
 * version 140906A by Vic added beeps
 * version 150122A AndroidStudio version
 * version 150225B AndroidStudio version
 * version 150613A Erik lessons
 **************************************************************************/

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

import org.jointheleague.erik.irobot.IRobotCreateAdapter;
import org.jointheleague.erik.irobot.IRobotCreateInterface;
import org.jointheleague.iaroc.Dashboard;
import org.jointheleague.iaroc.sensors.UltraSonicSensors;

import java.util.Random;

/**
 * A Lada is an implementation of the IRobotCreateInterface, inspired by Vic's
 * awesome API. It is entirely event driven.
 *
 * @author From the Erik Simplified version 140512A
 */
public class Brain extends IRobotCreateAdapter {
    private final Dashboard dashboard;
    Random r = new Random();
    public UltraSonicSensors sonar;
    private boolean firstPass = true;
    ;
    private int commandAzimuth;
    private int leftSignal;
    private int rightSignal;
    private int leftFrontSignal;
    private int rightFrontSignal;
    private int wheelSpeed = 50;
    private int relativeHeading = 0;
    private int irSensorThreshhold = 2000;
    public int turnSpan;
    private int[] beep1 = {72, 15};
    private int[] beep2 = {80, 15};
    private int[] beep3 = {65, 15};

    public Brain(IOIO ioio, IRobotCreateInterface create, Dashboard dashboard)
            throws ConnectionLostException {
        super(create);
        sonar = new UltraSonicSensors(ioio, dashboard);
        this.dashboard = dashboard;
    }

    public void initialize() throws ConnectionLostException {
        dashboard.log("WhiteLiner2014 version 140903B");
        driveDirect(wheelSpeed, wheelSpeed);
    }

    public void loop() throws ConnectionLostException {
        driveDirect(wheelSpeed, wheelSpeed);
        readSensors(SENSORS_GROUP_ID6); // Reads all sensors
        leftFrontSignal = getCliffFrontLeftSignal();
        rightFrontSignal = getCliffFrontRightSignal();
        leftSignal = getCliffLeftSignal();
        rightSignal = getCliffRightSignal();
        dashboard.log(leftFrontSignal + "");//for testing only to calibrate threshhold

        /***************************************************************************************
         * Handling left IR sensors.
         ***************************************************************************************/
        if (leftFrontSignal > irSensorThreshhold) // Seeing left front IR
        // sensor. Too far right.
        {
            song(1, beep1);
            playSong(1);
            turnAngle(5); // Turn left 5 degrees.
        }
        if (leftSignal > irSensorThreshhold) // Seeing left front IR sensor. Too
        // far right.
        {
            song(2, beep2);
            playSong(2);
            turnAngle(20); // Turn left 20 degrees.
        }

        /***************************************************************************************
         * Handling right IR sensors.
         ***************************************************************************************/
        if (rightFrontSignal > irSensorThreshhold) // Seeing right front IR
        // sensor. Too far left.
        {
            song(3, beep3);
            playSong(3);
            turnAngle(-5);// Turn right 5 degrees.
        }
        if (rightSignal > irSensorThreshhold) // Seeing right front IR sensor.
        // Too far left...turn right.
        {
            song(1, beep1);
            playSong(1);
            turnAngle(-20); // Turn right 20 degrees.
        }

        /***************************************************************************************
         * Checking for bumps. Turns right on left bump. Turns left on
         * right bump. Back up and turn right for head-on.
         ***************************************************************************************/
        boolean bumpRightSignal = isBumpRight();
        boolean bumpLeftSignal = isBumpLeft();

        if (bumpRightSignal) {
            song(1, beep1);
            playSong(1);
            driveDirect(wheelSpeed, 0);// turn left
        }

        if (bumpLeftSignal && bumpRightSignal) // Front bump.
        {
            song(1, beep1);
            playSong(1);
            driveDirect(-wheelSpeed, -wheelSpeed / 2); // Back up.
            turnAngle(-30); // Turn right 30 degrees.
            driveDirect(wheelSpeed, wheelSpeed); // Continue forward.
        }

    }

    public void turnAngle(int angleToTurn) throws ConnectionLostException // >0 means left, <0 means right.
    {
        if (angleToTurn > 0) {
            driveDirect(wheelSpeed, 0); // turn left
            relativeHeading = 0;
            while (relativeHeading < angleToTurn) {
                readSensors(SENSORS_GROUP_ID6);
                relativeHeading += getAngle();
            }
        }

        if (angleToTurn < 0) {
            driveDirect(0, wheelSpeed);// turn right
            relativeHeading = 0;
            while (relativeHeading > angleToTurn) {
                readSensors(SENSORS_GROUP_ID6);
                relativeHeading += getAngle();
            }
        }

        driveDirect(wheelSpeed, wheelSpeed); // Go straight.
    }

    public void turn(int commandAngle) throws ConnectionLostException // Doesn't
    // work
    // for
    // turns
    // through
    // 360
    {
        int startAzimuth = 0;
        if (firstPass) {
            startAzimuth += readCompass();
            commandAzimuth = (startAzimuth + commandAngle) % 360;
            dashboard.log("commandaz = " + commandAzimuth + " startaz = "
                    + startAzimuth);
            firstPass = false;
        }
        int currentAzimuth = readCompass();
        dashboard.log("now = " + currentAzimuth);
        if (currentAzimuth >= commandAzimuth) {
            driveDirect(0, 0);
            firstPass = true;
            dashboard.log("finalaz = " + readCompass());
        }
    }

    public int readCompass() {
        return (int) (dashboard.getAzimuth() + 360) % 360;
    }
}