package org.marketcetera.photon.internal.marketdata;



/* $License$ */

/**
 * Test {@link MarketstatKey}.
 * 
 * @author <a href="mailto:will@marketcetera.com">Will Horn</a>
 * @version $Id: KeyTest.java 10492 2009-04-14 00:11:35Z klim $
 * @since 1.5.0
 */
public class MarketstatKeyTest extends KeyTestBase {

	@Override
	Object createKey1() {
		return new MarketstatKey("IBM");
	}

	@Override
	Object createKey2() {
		return new MarketstatKey("METC");
	}

	@Override
	Object createKeyLike1ButDifferentClass() {
		return new MarketstatKey("IBM") {
		};
	}

	@Override
	void createKeyWithNullSymbol() {
		new MarketstatKey(null);
	}

}
