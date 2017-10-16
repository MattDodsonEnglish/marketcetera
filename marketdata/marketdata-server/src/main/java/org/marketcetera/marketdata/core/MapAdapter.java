package org.marketcetera.marketdata.core;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.marketcetera.util.misc.ClassVersion;

/* $License$ */

/**
 * Provides an adapter to translate Map values to and from XML.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id: MapAdapter.java 17251 2016-09-08 23:18:29Z colin $
 * @since 2.4.0
 */
@ClassVersion("$Id: MapAdapter.java 17251 2016-09-08 23:18:29Z colin $")
public class MapAdapter
        extends XmlAdapter<MapElements[],Map<String,String>>
{
    /* (non-Javadoc)
     * @see javax.xml.bind.annotation.adapters.XmlAdapter#unmarshal(java.lang.Object)
     */
    @Override
    public Map<String,String> unmarshal(MapElements[] inValue)
            throws Exception
    {
        Map<String,String> value = new HashMap<String,String>();
        for(MapElements mapElement : inValue) {
            value.put(mapElement.getKey(),
                      mapElement.getValue());
        }
        return value;
    }
    /* (non-Javadoc)
     * @see javax.xml.bind.annotation.adapters.XmlAdapter#marshal(java.lang.Object)
     */
    @Override
    public MapElements[] marshal(Map<String,String> inValue)
            throws Exception
    {
        MapElements[] mapElements = new MapElements[inValue.size()];
        int i = 0;
        for (Map.Entry<String,String> entry : inValue.entrySet()) {
            mapElements[i++] = new MapElements(entry.getKey(),
                                                    entry.getValue());
        }
        return mapElements;
    }
}