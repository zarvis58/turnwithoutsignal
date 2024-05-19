package com.safeai;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.sdp.CandidateAttribute;
import org.ice4j.ice.sdp.IceSdpUtils;
import org.opentelecoms.javax.sdp.NistSdpFactory;

import javax.sdp.*;
import org.apache.commons.lang3.StringUtils;
import org.ice4j.ice.Agent;
import org.ice4j.ice.Candidate;
import org.ice4j.ice.CandidateType;
import org.ice4j.ice.Component;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.IceProcessingState;
import org.ice4j.ice.LocalCandidate;
import org.ice4j.ice.NominationStrategy;
import org.ice4j.ice.RemoteCandidate;
import org.ice4j.ice.harvest.StunCandidateHarvester;
import org.ice4j.ice.harvest.TurnCandidateHarvester;
import org.ice4j.security.LongTermCredential;

public class StreamClient {

    private int communicationPort;

    private String clientStreamName;

    private Agent iceStreamClientAgent;

    private String localSessionDescription;

    private String externalSdp;

    private String[] turnServerAddresses = new String[] { "128.199.182.239:3478" };

    private String[] googleStunServers = new String[] { "stun.l.google.com:19302" };

    private String rootUser = "root";

    private String rootPassword = "root";

    private IceProcessingListener dataListener;

    public StreamClient(int port, String streamName) {
        this.communicationPort = port;
        this.clientStreamName = streamName;
        this.dataListener = new IceProcessingListener();
    }

    public void initialize(String peername) throws Throwable {

        iceStreamClientAgent = createAgent(communicationPort, clientStreamName);

        iceStreamClientAgent.setNominationStrategy(NominationStrategy.NOMINATE_HIGHEST_PRIO);

        iceStreamClientAgent.addStateChangeListener(dataListener);

        iceStreamClientAgent.setControlling(false);

        iceStreamClientAgent.setTa(10000);

        localSessionDescription = constructSDPDescription(iceStreamClientAgent);

        System.out.println("=================== local SDP ===================");

        System.out.println(localSessionDescription);

        saveSdpToFile(localSessionDescription, peername);

        System.out.println("======================================" + peername + "==============================");

        exchangeSdpWithPeer(peername);

    }

    @SuppressWarnings("deprecation")
    public DatagramSocket getDatagramSocket() throws Throwable {

        LocalCandidate localCandidate = iceStreamClientAgent
                .getSelectedLocalCandidate(clientStreamName);

        IceMediaStream stream = iceStreamClientAgent.getStream(clientStreamName);
        List<Component> components = stream.getComponents();
        for (Component c : components) {
            System.out.println(c.toString());
        }
        System.out.println(localCandidate.toString());
        LocalCandidate candidate = (LocalCandidate) localCandidate;
        return candidate.getDatagramSocket();

    }

    public SocketAddress obtainRemoteSocketAddress() {
        RemoteCandidate remoteCandidate = iceStreamClientAgent
                .getSelectedRemoteCandidate(clientStreamName);
        System.out.println(" transport :" + remoteCandidate.getTransportAddress());
        System.out.println(" host :" + remoteCandidate.getHostAddress());
        System.out.println(" mapped :" + remoteCandidate.getMappedAddress());
        System.out.println(" relayed :" + remoteCandidate.getRelayedAddress());
        System.out.println(" flexive :" + remoteCandidate.getReflexiveAddress());
        return remoteCandidate.getTransportAddress();
    }

    public void exchangeSdpWithPeer(String peer) throws Throwable {
        System.out.println("reading file value from file");
        String fiepath = peer + ".txt";
        externalSdp = new String(Files.readAllBytes(Paths.get(fiepath)));
        extractSdpAttributes(iceStreamClientAgent, externalSdp);
    }

    public void startConnect() throws InterruptedException {

        if (StringUtils.isBlank(externalSdp)) {
            throw new NullPointerException(
                    "Error");
        }

        iceStreamClientAgent.startConnectivityEstablishment();

        synchronized (dataListener) {
            dataListener.wait();
        }

    }

    private Agent createAgent(int rtpPort, String streamName) throws Throwable {
        return createAgent(rtpPort, streamName, false);
    }

    private Agent createAgent(int rtpPort, String streamName,
            boolean isTrickling) throws Throwable {

        System.currentTimeMillis();

        Agent agent = new Agent();

        agent.setTrickling(isTrickling);

        for (String server : googleStunServers) {
            String[] pair = server.split(":");
            agent.addCandidateHarvester(new StunCandidateHarvester(
                    new TransportAddress(pair[0], Integer.parseInt(pair[1]),
                            Transport.UDP)));
        }

        LongTermCredential longTermCredential = new LongTermCredential(rootUser,
                rootPassword);

        for (String server : turnServerAddresses) {
            String[] pair = server.split(":");
            agent.addCandidateHarvester(new TurnCandidateHarvester(
                    new TransportAddress(pair[0], Integer.parseInt(pair[1]), Transport.UDP),
                    longTermCredential));
        }

        createStream(rtpPort, streamName, agent);

        System.currentTimeMillis();
        

        

        return agent;
    }

    private IceMediaStream createStream(int rtpPort, String streamName,
            Agent agent) throws Throwable {
        System.currentTimeMillis();
        IceMediaStream stream = agent.createMediaStream(streamName);

        agent.createComponent(stream, Transport.UDP,
                rtpPort, rtpPort, rtpPort + 100);

        return stream;
    }

    public static final class IceProcessingListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent event) {
            Object state = event.getNewValue();

            if (state == IceProcessingState.COMPLETED) {
                Agent agent = (Agent) event.getSource();

                synchronized (this) {
                    this.notifyAll();
                }
            } else if (state == IceProcessingState.TERMINATED) {

            } else if (state == IceProcessingState.FAILED) {
                ((Agent) event.getSource()).free();
            }
        }
    }

    public static String constructSDPDescription(Agent agent) throws Throwable {
        SdpFactory factory = new NistSdpFactory();
        SessionDescription sdess = factory.createSessionDescription();

        IceSdpUtils.initSessionDescription(sdess, agent);

        return sdess.toString();
    }

    @SuppressWarnings("unchecked")
    public static void extractSdpAttributes(Agent localAgent, String sdp)
            throws Exception {
        SdpFactory factory = new NistSdpFactory();
        SessionDescription sdess = factory.createSessionDescription(sdp);

        for (IceMediaStream stream : localAgent.getStreams()) {
            stream.setRemotePassword(sdess.getAttribute("ice-pwd"));
            stream.setRemoteUfrag(sdess.getAttribute("ice-ufrag"));
        }

        Connection globalConn = sdess.getConnection();
        String globalConnAddr = null;
        if (globalConn != null)
            globalConnAddr = globalConn.getAddress();

        Vector<MediaDescription> mdescs = sdess.getMediaDescriptions(true);

        for (MediaDescription desc : mdescs) {
            String streamName = desc.getMedia().getMediaType();

            IceMediaStream stream = localAgent.getStream(streamName);

            if (stream == null)
                continue;

            Vector<Attribute> attributes = desc.getAttributes(true);
            for (Attribute attribute : attributes) {
                if (!attribute.getName().equals(CandidateAttribute.NAME))
                    continue;

                parseRemoteCandidate(attribute, stream);
            }

            Connection streamConn = desc.getConnection();
            String streamConnAddr = null;
            if (streamConn != null)
                streamConnAddr = streamConn.getAddress();
            else
                streamConnAddr = globalConnAddr;

            int port = desc.getMedia().getMediaPort();

            TransportAddress defaultRtpAddress = new TransportAddress(streamConnAddr, port, Transport.UDP);

            int rtcpPort = port + 1;
            String rtcpAttributeValue = desc.getAttribute("rtcp");

            if (rtcpAttributeValue != null)
                rtcpPort = Integer.parseInt(rtcpAttributeValue);

            TransportAddress defaultRtcpAddress = new TransportAddress(streamConnAddr, rtcpPort, Transport.UDP);

            Component rtpComponent = stream.getComponent(Component.RTP);
            Component rtcpComponent = stream.getComponent(Component.RTCP);

            Candidate<?> defaultRtpCandidate = rtpComponent.findRemoteCandidate(defaultRtpAddress);
            rtpComponent.setDefaultRemoteCandidate(defaultRtpCandidate);

            if (rtcpComponent != null) {
                Candidate<?> defaultRtcpCandidate = rtcpComponent.findRemoteCandidate(defaultRtcpAddress);
                rtcpComponent.setDefaultRemoteCandidate(defaultRtcpCandidate);
            }
        }
    }

    private static RemoteCandidate parseRemoteCandidate(Attribute attribute,
            IceMediaStream stream) {
        String value = null;

        try {
            value = attribute.getValue();
        } catch (Throwable t) {
        }

        StringTokenizer tokenizer = new StringTokenizer(value);

        String foundation = tokenizer.nextToken();
        int componentID = Integer.parseInt(tokenizer.nextToken());
        Transport transport = Transport.parse(tokenizer.nextToken());
        long priority = Long.parseLong(tokenizer.nextToken());
        String address = tokenizer.nextToken();
        int port = Integer.parseInt(tokenizer.nextToken());

        TransportAddress transAddr = new TransportAddress(address, port, transport);

        tokenizer.nextToken();
        CandidateType type = CandidateType.parse(tokenizer.nextToken());

        Component component = stream.getComponent(componentID);

        if (component == null)
            return null;

        RemoteCandidate relatedCandidate = null;
        if (tokenizer.countTokens() >= 4) {
            tokenizer.nextToken();
            String relatedAddr = tokenizer.nextToken();
            tokenizer.nextToken();
            int relatedPort = Integer.parseInt(tokenizer.nextToken());

            TransportAddress raddr = new TransportAddress(
                    relatedAddr, relatedPort, Transport.UDP);

            relatedCandidate = component.findRemoteCandidate(raddr);
        }

        RemoteCandidate cand = new RemoteCandidate(transAddr, component, type,
                foundation, priority, relatedCandidate);

        component.addRemoteCandidate(cand);

        return cand;
    }

    public static void saveSdpToFile(String localSdp, String string) {

        try {
            java.io.FileWriter fw = new java.io.FileWriter(string + ".txt");
            fw.write(localSdp);
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String[] getTurnServerAddresses() {
        return turnServerAddresses;
    }

    public void setTurnServerAddresses(String[] turnServers) {
        this.turnServerAddresses = turnServers;
    }

    public String[] getGoogleStunServers() {
        return googleStunServers;
    }

    public void setGoogleStunServers(String[] stunServers) {
        this.googleStunServers = stunServers;
    }

    public String getRootUser() {
        return rootUser;
    }

    public void setRootUser(String username) {
        this.rootUser = username;
    }

    public String getRootPassword() {
        return rootPassword;
    }

    public void setRootPassword(String password) {
        this.rootPassword = password;
    }
}