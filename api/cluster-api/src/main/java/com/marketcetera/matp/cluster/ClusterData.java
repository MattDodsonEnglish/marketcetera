package com.marketcetera.matp.cluster;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/* $License$ */

/**
 * Describes this cluster instance.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ClusterData
        implements Serializable,Comparable<ClusterData>
{
    /**
     * Create a new ClusterData instance.
     */
    public ClusterData() {}
    
    /**
     * Create a new ClusterData instance.
     *
     * @param inTotalInstances an <code>int</code> value
     * @param inHostId a <code>String</code> value
     * @param inHostNumber an <code>int</code> value
     * @param inInstanceNumber an <code>int</code> value
     * @param inUuid a <code>String</code> value
     */
    public ClusterData(int inTotalInstances,
                       String inHostId,
                       int inHostNumber,
                       int inInstanceNumber,
                       String inUuid)
    {
        instanceNumber = inInstanceNumber;
        hostNumber = inHostNumber;
        hostId = inHostId;
        totalInstances = inTotalInstances;
        uuid = inUuid;
    }
    /**
     * Get the instanceNumber value.
     *
     * @return an <code>int</code> value
     */
    public int getInstanceNumber()
    {
        return instanceNumber;
    }
    /**
     * Sets the instanceNumber value.
     *
     * @param inInstanceNumber an <code>int</code> value
     */
    public void setInstanceNumber(int inInstanceNumber)
    {
        instanceNumber = inInstanceNumber;
    }
    /**
     * Get the hostNumber value.
     *
     * @return an <code>int</code> value
     */
    public int getHostNumber()
    {
        return hostNumber;
    }
    /**
     * Sets the hostNumber value.
     *
     * @param inHostNumber an <code>int</code> value
     */
    public void setHostNumber(int inHostNumber)
    {
        hostNumber = inHostNumber;
    }
    /**
     * Get the hostId value.
     *
     * @return a <code>String</code> value
     */
    public String getHostId()
    {
        return hostId;
    }
    /**
     * Sets the hostId value.
     *
     * @param inHostId a <code>String</code> value
     */
    public void setHostId(String inHostId)
    {
        hostId = inHostId;
    }
    /**
     * Get the totalInstances value.
     *
     * @return an <code>int</code> value
     */
    public int getTotalInstances()
    {
        return totalInstances;
    }
    /**
     * Sets the totalInstances value.
     *
     * @param inTotalInstances an <code>int</code> value
     */
    public void setTotalInstances(int inTotalInstances)
    {
        totalInstances = inTotalInstances;
    }
    /**
     * Get the uuid value.
     *
     * @return a <code>String</code> value
     */
    public String getUuid()
    {
        return uuid;
    }
    /**
     * Sets the uuid value.
     *
     * @param inUuid a <code>String</code> value
     */
    public void setUuid(String inUuid)
    {
        uuid = inUuid;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return new StringBuilder().append("host").append(hostNumber).append("-").append(instanceNumber).toString();
    }
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(hostId).append(instanceNumber).toHashCode();
    }
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ClusterData)) {
            return false;
        }
        ClusterData other = (ClusterData) obj;
        return new EqualsBuilder().append(hostId,other.hostId).append(instanceNumber,other.instanceNumber).isEquals();
    }
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(ClusterData inO)
    {
        return new CompareToBuilder().append(hostNumber,inO.getHostNumber()).append(instanceNumber,inO.getInstanceNumber()).toComparison();
    }
    /**
     * instance number value
     */
    private int instanceNumber;
    /**
     * host number value
     */
    private int hostNumber;
    /**
     * host id value
     */
    private String hostId;
    /**
     * total instances value
     */
    private int totalInstances;
    /**
     * uuid value
     */
    private String uuid;
    private static final long serialVersionUID = -6037562825938330054L;
}
