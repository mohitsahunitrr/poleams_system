package com.precisionhawk.poleams.processors.poleinspection.ppl;

import com.precisionhawk.ams.bean.Point;
import java.util.LinkedList;
import java.util.List;
import org.papernapkin.liana.xml.sax.AbstractDocumentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 *
 * @author pchapman
 */
public class PoleXMLDocumentHandler extends AbstractDocumentHandler {
    
    private static final String TAG_FILE_NAME = "filename";
    private static final String TAG_OBJECT = "object";
    private static final String TAG_NAME = "name";
    private static final String TAG_XMAX = "xmax";
    private static final String TAG_XMIN = "xmin";
    private static final String TAG_YMAX = "ymax";
    private static final String TAG_YMIN = "ymin";
    
    private String fileName;
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    private Observation observation;

    private List<Observation> observations = new LinkedList();
    public List<Observation> getObservations() {
        return observations;
    }
    public void setObservations(List<Observation> observations) {
        this.observations = observations;
    }
    
    @Override
    protected void _endElement(String uri, String localName, String qName) throws SAXException {
        switch (localName) {
            case TAG_FILE_NAME:
                fileName = super.textbuffer.toString().trim();
                break;
            case TAG_NAME:
                observation.setName(super.textbuffer.toString().trim());
                break;
            case TAG_OBJECT:
                validate(observation);
                observations.add(observation);
                observation = null;
                break;
            case TAG_XMAX:
                observation.setXmax(intFromText());
                break;
            case TAG_XMIN:
                observation.setXmin(intFromText());
                break;
            case TAG_YMAX:
                observation.setYmax(intFromText());
                break;
            case TAG_YMIN:
                observation.setYmin(intFromText());
                break;
        }
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch (localName) {
            case TAG_OBJECT:
                observation = new Observation();
                break;
        }
    }
    
    private int intFromText() throws SAXException {
        String s = super.textbuffer.toString().trim();
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException ex) {
            throw new SAXException(String.format("Invalid integer value %s", s), ex);
        }
    }
    
    private void validate(Observation o) throws SAXException {
        if (o.getXmax() == null) {
            throw new SAXException("Missing xmax");
        } else if (o.getXmin() == null) {
            throw new SAXException("Missing xmin");
        } else if (o.getYmax() == null) {
            throw new SAXException("Missing ymax");
        } else if (o.getYmin() == null) {
            throw new SAXException("Missing ymin");
        }
    }
}

class Observation {
    
    private String name;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    private Integer xmax;
    public Integer getXmax() {
        return xmax;
    }
    public void setXmax(Integer xmax) {
        this.xmax = xmax;
    }

    private Integer xmin;
    public Integer getXmin() {
        return xmin;
    }
    public void setXmin(Integer xmin) {
        this.xmin = xmin;
    }

    private Integer ymax;
    public Integer getYmax() {
        return ymax;
    }
    public void setYmax(Integer ymax) {
        this.ymax = ymax;
    }

    private Integer ymin;
    public Integer getYmin() {
        return ymin;
    }
    public void setYmin(Integer ymin) {
        this.ymin = ymin;
    }
    
    List<Point> points() {
        List<Point> points = new LinkedList();
        points.add(new Point(xmin.doubleValue(), ymax.doubleValue()));
        points.add(new Point(xmin.doubleValue(), ymin.doubleValue()));
        points.add(new Point(xmax.doubleValue(), ymin.doubleValue()));
        points.add(new Point(xmax.doubleValue(), ymax.doubleValue()));
//        points.add(new Point(Double.valueOf(xmin), Double.valueOf(ymax)));
        return points;
    }
}