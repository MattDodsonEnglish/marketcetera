//
// this file is automatically generated
//
package org.marketcetera.strategy;

/* $License$ */

/**
 * Provides permissions for services.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
public enum StrategyPermissions
        implements org.springframework.security.core.GrantedAuthority
{
    CancelStrategyUploadAction,
    ClearStrategyEventsAction,
    LoadStrategyAction,
    ReadStrategyAction,
    StartStrategyAction,
    StopStrategyAction,
    UnloadStrategyAction;
    /* (non-Javadoc)
     * @see org.springframework.security.core.GrantedAuthority#getAuthority()
     */
    @Override
    public String getAuthority()
    {
        return name();
    }
}
