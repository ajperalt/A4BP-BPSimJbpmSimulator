package a4bp.integration;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class SimulationServerTestResource {

	
	public static String tsunami(){
		String xml = "";
		InputStream is = SimulationServerTestResource.class.getResourceAsStream("/TsunamiModel.bpmn");
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        try {
            for (int readNum; (readNum = is.read(buf)) != -1;) {
                bos.write(buf, 0, readNum);
            }
            xml = bos.toString();
        } catch (Exception ex) {
           	
        }
		
		return xml;
	}
}

