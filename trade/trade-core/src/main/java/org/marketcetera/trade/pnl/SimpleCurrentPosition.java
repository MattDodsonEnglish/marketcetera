//
// this file is automatically generated
//
package org.marketcetera.trade.pnl;

/* $License$ */

/**
 * Describes the current position of a given instrument owned by a given user.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
public class SimpleCurrentPosition
        implements CurrentPosition
{
    /**
     * Create a new SimpleCurrentPosition instance.
     */
    public SimpleCurrentPosition() {}
    /**
     * Create a new SimpleCurrentPosition instance.
     *
     * @param inCurrentPosition a <code>CurrentPosition</code> value
     */
    public SimpleCurrentPosition(CurrentPosition inCurrentPosition)
    {
        setInstrument(inCurrentPosition.getInstrument());
        setUser(inCurrentPosition.getUser());
        setPosition(inCurrentPosition.getPosition());
        setWeightedAverageCost(inCurrentPosition.getWeightedAverageCost());
        setRealizedGain(inCurrentPosition.getRealizedGain());
        setUnrealizedGain(inCurrentPosition.getUnrealizedGain());
    }
    /**
     * Get the instrument value.
     *
     * @return a <code>org.marketcetera.trade.Instrument</code> value
     */
    @Override
    public org.marketcetera.trade.Instrument getInstrument()
    {
        return instrument;
    }
    /**
     * Set the instrument value.
     *
     * @param inInstrument a <code>org.marketcetera.trade.Instrument</code> value
     */
    @Override
    public void setInstrument(org.marketcetera.trade.Instrument inInstrument)
    {
        instrument = inInstrument;
    }
    /**
     * Get the user value.
     *
     * @return a <code>org.marketcetera.admin.User</code> value
     */
    @Override
    public org.marketcetera.admin.User getUser()
    {
        return user;
    }
    /**
     * Set the user value.
     *
     * @param inUser a <code>org.marketcetera.admin.User</code> value
     */
    @Override
    public void setUser(org.marketcetera.admin.User inUser)
    {
        user = inUser;
    }
    /**
     * Get the position value.
     *
     * @return a <code>java.math.BigDecimal</code> value
     */
    @Override
    public java.math.BigDecimal getPosition()
    {
        return position;
    }
    /**
     * Set the position value.
     *
     * @param inPosition a <code>java.math.BigDecimal</code> value
     */
    @Override
    public void setPosition(java.math.BigDecimal inPosition)
    {
        position = inPosition == null ? java.math.BigDecimal.ZERO : inPosition;
    }
    /**
     * Get the weightedAverageCost value.
     *
     * @return a <code>java.math.BigDecimal</code> value
     */
    @Override
    public java.math.BigDecimal getWeightedAverageCost()
    {
        return weightedAverageCost;
    }
    /**
     * Set the weightedAverageCost value.
     *
     * @param inWeightedAverageCost a <code>java.math.BigDecimal</code> value
     */
    @Override
    public void setWeightedAverageCost(java.math.BigDecimal inWeightedAverageCost)
    {
        weightedAverageCost = inWeightedAverageCost == null ? java.math.BigDecimal.ZERO : inWeightedAverageCost;
    }
    /**
     * Get the realizedGain value.
     *
     * @return a <code>java.math.BigDecimal</code> value
     */
    @Override
    public java.math.BigDecimal getRealizedGain()
    {
        return realizedGain;
    }
    /**
     * Set the realizedGain value.
     *
     * @param inRealizedGain a <code>java.math.BigDecimal</code> value
     */
    @Override
    public void setRealizedGain(java.math.BigDecimal inRealizedGain)
    {
        realizedGain = inRealizedGain == null ? java.math.BigDecimal.ZERO : inRealizedGain;
    }
    /**
     * Get the unrealizedGain value.
     *
     * @return a <code>java.math.BigDecimal</code> value
     */
    @Override
    public java.math.BigDecimal getUnrealizedGain()
    {
        return unrealizedGain;
    }
    /**
     * Set the unrealizedGain value.
     *
     * @param inUnrealizedGain a <code>java.math.BigDecimal</code> value
     */
    @Override
    public void setUnrealizedGain(java.math.BigDecimal inUnrealizedGain)
    {
        unrealizedGain = inUnrealizedGain == null ? java.math.BigDecimal.ZERO : inUnrealizedGain;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("CurrentPosition [")
            .append("instrument=").append(instrument)
            .append(", user=").append(user)
            .append(", position=").append(org.marketcetera.core.BigDecimalUtil.render(position))
            .append(", weightedAverageCost=").append(org.marketcetera.core.BigDecimalUtil.renderCurrency(weightedAverageCost))
            .append(", realizedGain=").append(org.marketcetera.core.BigDecimalUtil.renderCurrency(realizedGain))
            .append(", unrealizedGain=").append(org.marketcetera.core.BigDecimalUtil.renderCurrency(unrealizedGain)).append("]");
        return builder.toString();
    }
    /**
     * position instrument value
     */
    private org.marketcetera.trade.Instrument instrument;
    /**
     * user which owns lot
     */
    private org.marketcetera.admin.User user;
    /**
     * position value
     */
    private java.math.BigDecimal position = java.math.BigDecimal.ZERO;
    /**
     * weighted average cost to attain this position
     */
    private java.math.BigDecimal weightedAverageCost = java.math.BigDecimal.ZERO;
    /**
     * realized gain value
     */
    private java.math.BigDecimal realizedGain = java.math.BigDecimal.ZERO;
    /**
     * unrealized gain value
     */
    private java.math.BigDecimal unrealizedGain = java.math.BigDecimal.ZERO;
}