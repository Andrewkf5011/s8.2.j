
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.io.*;
import java.net.*;

import graph.*;

public class WaterHeater extends JFrame implements Runnable {
    public static final long serialVersionUID = 2L;
    public static void main ( String[] args ) throws SocketException {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() { new WaterHeater(); }
        } );
    }

    HeatingSystem heatingsys = new HeatingSystem();
    GraphPanel plots = new GraphPanel(new Dimension(400,200));
    DatagramPanel receive = new DatagramPanel();

    public WaterHeater() {
        super("Water Heating");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel content = new JPanel( );
        content.setLayout( new BoxLayout( content, BoxLayout.Y_AXIS) );

        try {
            receive.setAddress(InetAddress.getLocalHost().getHostAddress(), false);
           receive.setPort(65280, false);
        }catch(UnknownHostException e){
            System.err.println(e.getMessage());
        }
        content.add(receive);

        content.add( heatingsys );

        JPanel data = new JPanel();
        data.setBorder( BorderFactory.createTitledBorder("Temperature"));
        data.add( plots );
        content.add(data);
        this.setContentPane(content);
        this.pack();
        this.setVisible(true);

        /* Add a plot with key/title, an initial value,
            a colour, and min max value scale
        */
        GraphPanel.Scale water = new GraphPanel.Scale(0,110);
        plots.addTrace("T", 20, Color.blue, water);
        plots.addRegion("green tea", 65,80, water, Color.red.darker());
        plots.addRegion("coffee", 91,96, water, Color.blue.brighter());

        plots.addTrace("H", 0, Color.red, new GraphPanel.Scale(-60,1.2));
        /* Start the threads for the
           Heating system on a 0.1s period
           Plot data on a 0.1s period
        */
        (new javax.swing.Timer(100,heatingsys)).start();
        (new javax.swing.Timer(100,plots)).start();

        /* start thread that handles comminications */
        (new Thread(this)).start();
    }

    public void run() {
        try{
        /* set up socket for reception */
        SocketAddress address = receive.getSocketAddress();
        DatagramSocket socket = new DatagramSocket(address);

        while(true) {
            try{
                /* start with fresh datagram packet */
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive( packet );
                /* extract message and pick appart into
                   lines and key:value pairs
                */
                String message = new String(packet.getData());
                String[] lines = message.trim().split("\n");
                String[] pair  = lines[0].split(":");

                switch( pair[0] ) {/*<-- Java now lets you do switches on strings :-) */
                    case "temperature":
                        if(pair[1].equals("?")) {
                            String reply = String.format("temperature:%f\n",heatingsys.getTemperature() );
                            packet.setData(reply.getBytes());/* packet has return address from the receive() above */
                            socket.send( packet );
                        }
                    break;
                    case "heating":
                        switch( pair[1] ){
                            case "on":  heatingsys.boiler.on();  break;
                            case "off": heatingsys.boiler.off(); break;
                        }
                    break;
                }
            }catch(IOException e){
                System.err.println(e.getMessage());
            }
        }
    }catch(SocketException e){System.err.println(e.getMessage());}
    }


    public class HeatingSystem extends JPanel implements ActionListener {
        public static final long serialVersionUID = 2L;
        Boiler boiler = new Boiler();
        JLabel reading = new JLabel("     \u00B0");
        double temperature;
        double element = 1.5;
        public HeatingSystem(){
            super(new FlowLayout( FlowLayout.LEFT, 5, 0));
            this.setBorder( BorderFactory.createTitledBorder("System"));
            this.add(boiler);
            this.add(reading);
        }
        double getTemperature() { return temperature; }
        public void actionPerformed(ActionEvent t){
            double DT = temperature - 10;
            double dt = 0.1;
            double  k = .1;
            temperature += -k*DT*dt ;
            if(boiler.active)temperature+=element;
            reading.setText(String.format("%8.1f\u00B0C", temperature));
            plots.plotPoint("T",temperature);
            plots.plotPoint("H",(boiler.active)?1.0:0.0);
        }
        class Boiler extends JLabel {
            public static final long serialVersionUID = 2L;
            boolean active = false;
            ImageIcon[] indicator = {
                new ImageIcon(WaterHeater.class.getResource("led-grey.png")),
                new ImageIcon(WaterHeater.class.getResource("led-green.png"))
            };
            public Boiler(){super("heating"); setIcon(indicator[0]);}
            public void on(){active=true; setIcon(indicator[1]);}
            public void off(){active=false; setIcon(indicator[0]);}
        }
        void on()  { boiler.on();  }
        void off() { boiler.off(); }
    }
}
