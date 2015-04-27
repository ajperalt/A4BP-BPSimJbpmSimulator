/**
 * 
 */
package a4bp.integration;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author peralta
 *
 */
public class SimulationServerTest {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link a4bp.integration.SimulationServer#runSimulation(java.lang.String)}.
	 */
	@Test
	public void testRunSimulationString() {
		SimulationServer ss = new SimulationServer();
		try {
			String xml = SimulationServerTestResource.tsunami();
			ss.runSimulation(xml);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
