package graph;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import graph.*;

public class GraphPanel extends JPanel  implements  ActionListener {
    public static final long serialVersionUID = 1L;
    Scale  scale;
    double rate;
    int[] xaxis;
    ArrayList<Range> marking = new ArrayList<Range>();
    ArrayList<Trace> traces = new ArrayList<Trace>();
    Hashtable<String,Double> datatable = new Hashtable<String,Double>();

    public GraphPanel(Dimension size) {
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));

        this.setPreferredSize( size );
        this.xaxis = new int[size.width];
        for(int x=0 ; x<size.width ; x++ )  xaxis[x]=x;
        datatable = new Hashtable<String,Double>();
        datatable.put("x",0.0);
    }

    public void addTrace(String name, double init, Color color, Scale scale){
        datatable.put(name,init);
        traces.add(new Trace(name,this.getSize().width,color,scale));
    }
    public void actionPerformed(ActionEvent evt) {
        for(Trace t : traces) {
            t.addPoint( datatable.get(t.name));
        }
        this.repaint();
    }
    public void plotPoint(String name, double value) {
        datatable.put(name,value);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        for(Range r : marking) r.draw(g,this.getSize());
        for(Trace t : traces){
            int y[] = t.getPoints(this.getSize().height);
            g.setColor( t.color );
            g.drawPolyline(xaxis,y, xaxis.length);
        }
    }

    public void addRegion(String label,
                        double lower, double upper,
                        Scale scale, Color colour){
        marking.add(new Range(label,lower,upper,scale,colour));
    }


    public class Trace {
        public String name;
        public Color color;
        Scale scale;
        double[] data;

        public Trace(String name, int size, Color color, Scale scale){
            this.name = name;
            data = new double[size];
            this.color = color;
            this.scale = scale;
        }
        public void addPoint(double d) {
            for(int n=0 ; n<(data.length-1) ; n++) {
                data[n] = data[n+1];
            }
            data[data.length-1] = d;
        }
        public int[] getPoints(int height){
            int[] points = new int[data.length];
            for(int n=0 ; n<data.length ; n++ ){
                points[n] = (int)(Math.map(data[n], scale.from, scale.to, height,0));
            }
            return points;
        }
    }

    /* upper and lower bounds of vertical scale */
    static public class Scale {
        public  double from;
        public  double to;

        public Scale(double from, double to) {
            this.from = from;
            this.to = to;
        }
    }

    /* Class wrapping useful maths functions */
    static public class Math {
        static public double norm(double value, double low, double high) {
            return (value-low)/(high-low);
        }

        static public double lerp(double low, double high, double fraction) {
            return (1-fraction)*low + fraction*high;
        }

        static public double map(double value, double inlow, double inhigh, double outlow, double outhigh ) {
            return lerp( outlow, outhigh, norm(value, inlow, inhigh) );
        }

    }

    static public class Range {
        public String label;
        public double lower;
        public double upper;
        public Scale scale;
        public Color colour;

        public Range(String label,
                        double lower, double upper,
                        Scale scale, Color colour) {
            this.label = label;
            this.lower = lower;
            this.upper = upper;
            this.scale = scale;
            this.colour = new Color(colour.getRed(),colour.getGreen(),colour.getBlue(),64);
        }
        public void draw(Graphics g,Dimension size){
            Graphics gg = g.create();
            gg.setColor(this.colour);
            int l = (int)(Math.map(upper, scale.from, scale.to, size.height,0));
            int u = (int)(Math.map(lower, scale.from, scale.to, size.height,0));
            gg.fillRect(0,l, size.width, (u-l));
            gg.setColor(Color.black);
            gg.drawString(label,0,l);
        }
    }
}
