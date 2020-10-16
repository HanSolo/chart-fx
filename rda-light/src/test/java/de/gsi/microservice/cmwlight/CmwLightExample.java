package de.gsi.microservice.cmwlight;

import de.gsi.serializer.IoClassSerialiser;
import de.gsi.serializer.spi.CmwLightSerialiser;
import de.gsi.serializer.spi.FastByteBuffer;

import java.io.IOException;
import java.util.Collections;

import static java.nio.file.Paths.get;

public class CmwLightExample {
    private final static String CMW_NAMESERVER = "cmwpro00a.acc.gsi.de:5021";
    private final static String DEVICE = "GSCD001";
    private final static String PROPERTY = "SnoopTriggerEvents";
    private final static String SELECTOR = "FAIR.SELECTOR.ALL";

    public static void main(String[] args) throws CmwLightClient.RdaLightException, IOException, DirectoryLightClient.DirectoryClientException {
        subscribeSnoopFromDigitizer();
    }

    public static void subscribeSnoopFromDigitizer() throws CmwLightClient.RdaLightException, DirectoryLightClient.DirectoryClientException, IOException {
        final DirectoryLightClient directoryClient = new DirectoryLightClient(CMW_NAMESERVER);
        DirectoryLightClient.Device device = directoryClient.getDeviceInfo(Collections.singletonList(DEVICE)).get(0);
        System.out.println(device);
        final String address = device.servers.stream().findFirst().orElseThrow().get("Address:");
        System.out.println("connect client to " + address);
        final CmwLightClient client = new CmwLightClient(address);
        client.connect();
        System.out.println("Client connected");

        System.out.println("starting subscription");
        client.subscribe(DEVICE, PROPERTY, SELECTOR);

        int i = 0;
        while (i < 15) {
            final CmwLightClient.Reply result = client.receiveData();
            if (result instanceof CmwLightClient.DataReply) {
                final byte[] bytes = ((CmwLightClient.DataReply) result).dataBody.getData();
                final IoClassSerialiser classSerialiser = new IoClassSerialiser(FastByteBuffer.wrap(bytes), CmwLightSerialiser.class);
                final SnoopAcquisition snoopAcq = classSerialiser.deserialiseObject(SnoopAcquisition.class);
                System.out.println("body: " + snoopAcq);
                i++;
                client.sendHeartBeat();
            } else {
                System.out.print(".");
            }
        }

        System.out.println("unsubscribe");
        client.unsubscribe(DEVICE, PROPERTY, SELECTOR);
        while (i < 25) {
            final CmwLightClient.Reply result = client.receiveData();
            if (result instanceof CmwLightClient.DataReply) {
                final byte[] bytes = ((CmwLightClient.DataReply) result).dataBody.getData();
                final IoClassSerialiser classSerialiser = new IoClassSerialiser(FastByteBuffer.wrap(bytes), CmwLightSerialiser.class);
                final SnoopAcquisition snoopAcq = classSerialiser.deserialiseObject(SnoopAcquisition.class);
                System.out.println("body: " + snoopAcq);
            }
            client.sendHeartBeat();
            i++;
            System.out.print(".");
        }
    }

    public static void getBenchmark() throws CmwLightClient.RdaLightException {
        System.out.println("connect control socket");
        final CmwLightClient client = new CmwLightClient("tcp://SYSPC004:7777");
        client.connect();

        // test sync
        System.out.println("start benchmark");
        final int nExec = 10000;
        final long start = System.currentTimeMillis();
        for (int i = 0 ; i < nExec; i++) {
            client.get("testdevice", "testproperty", "FAIR.SELECTOR.ALL");
            while (client.receiveData() instanceof CmwLightClient.DataReply) {
                // this loop is intentionally left blank
            }
        }
        final long stop = System.currentTimeMillis();
        System.out.printf("%10d sync calls/second\n", (1000 * nExec) / (stop - start));

        //// test async
        //final long startAsync = System.currentTimeMillis();
        //for (int i = 0 ; i < nExec/32; i++) {
        //    for (int j = 0; j < 32; j++) {
        //        get("testdevice", "testproperty", "FAIR.SELECTOR.ALL");
        //    }
        //    while (controlChannel.)
        //    while (receiveData() != 2) {
        //        // this loop is intentionally left blank
        //    }
        //}
        //final long stopAsync = System.currentTimeMillis();
        //System.out.printf("%-40s: %10d async calls/second\n", (1000 * nExec) / (stopAsync - startAsync));

    }

    public static void getSnoopFromDigitizer() throws CmwLightClient.RdaLightException {
        System.out.println("connect control socket");
        final CmwLightClient client = new CmwLightClient("tcp://dal005.acc.gsi.de:13810");
        client.connect();

        client.sendHeartBeat();

        client.get("GSCD001", "SnoopTriggerEvents", "FAIR.SELECTOR.S=4");

        while (true) {
            client.receiveData();
        }
    }

    public static void getLocal() throws CmwLightClient.RdaLightException {
        System.out.println("connect control socket");
        final CmwLightClient client = new CmwLightClient("tcp://SYSPC004:7777");
        client.connect();

        client.sendHeartBeat();

        client.get("testdevice", "unknownProp", "FAIR.SELECTOR.ALL");
        // return reply to data
        while (client.receiveData() instanceof CmwLightClient.ExceptionReply) {
            // this loop is intentionally left blank
        }
        System.out.println("Received GET Exception");

        get("testdevice", "testproperty", "FAIR.SELECTOR.ALL");
        while (client.receiveData() instanceof CmwLightClient.DataReply) {
            // this loop is intentionally left blank
        }
        System.out.println("Received GET Reply");

        client.subscribe("testdevice", "testproperty", "FAIR.SELECTOR.ALL");
        // just return all data
        while (true) {
            client.receiveData();
        }
    }

    private static class SnoopAcquisition {
        public String TriggerEventName;
        public long acquisitionStamp;
        public int chainIndex;
        public long chainStartStamp;
        public int eventNumber;
        public long eventStamp;
        public int processIndex;
        public long processStartStamp;
        public int sequenceIndex;
        public long sequenceStartStamp;
        public int timingGroupID;

        @Override
        public String toString() {
            return "SnoopAcquisition{" +
                    "TriggerEventName='" + TriggerEventName + '\'' +
                    ", acquisitionStamp=" + acquisitionStamp +
                    ", chainIndex=" + chainIndex +
                    ", chainStartStamp=" + chainStartStamp +
                    ", eventNumber=" + eventNumber +
                    ", eventStamp=" + eventStamp +
                    ", processIndex=" + processIndex +
                    ", processStartStamp=" + processStartStamp +
                    ", sequenceIndex=" + sequenceIndex +
                    ", sequenceStartStamp=" + sequenceStartStamp +
                    ", timingGroupID=" + timingGroupID +
                    '}';
        }
    }
}