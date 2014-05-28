package koncept.io;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

public class LineByLineStreamTest {

	@Test
	public void smallBufferSingleMatch() throws IOException {
		//use an odd sized buffer
		LineStreamer lines = new LineStreamer(simpleStream(), 7, new byte[]{32});
		byte[] read = lines.bytesToToken();
		System.out.println(read.length);
		//first 32 character = size 16
		assertThat(read.length, is(32)); //0 to 31
		assertThat(read[0], is((byte)0));
		assertThat(read[31], is((byte)31));
		
		
	}
	
	@Test
	public void largeBufferSingleMatch() throws IOException {
		LineStreamer lines = new LineStreamer(simpleStream(), 1024, new byte[]{0}); 
		byte[] read = lines.bytesToToken();
		int readTimes = 0;
		while(read != null) {
			readTimes++;
			switch(readTimes) {
			case 1:
				assertThat("read number " + readTimes + " failed", read.length, is(0)); //simple lengths for now
				break;
			default:
				assertThat("read number " + readTimes + " failed", read.length, is(126)); //simple lengths for now
			}
			
				
			read = lines.bytesToToken();
		}
		assertThat(readTimes, is(11));
	}
	
	private InputStream simpleStream() {
		byte[] b = new byte[Byte.MAX_VALUE * 10];
		for(int i = 0; i < b.length; i++) b[i] = (byte)(i % Byte.MAX_VALUE);
		return new ByteArrayInputStream(b);
	}
}
