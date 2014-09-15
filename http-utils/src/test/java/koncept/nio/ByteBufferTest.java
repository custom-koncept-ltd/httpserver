package koncept.nio;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;

import org.junit.Test;

public class ByteBufferTest {
	
	private ByteBuffer buff = ByteBuffer.allocate(10);
	
	/**
	 * including a cheap println or suppress method<br>
	 * Because it can help with visualisation
	 * @param s
	 */
	private void println(String s) {
		System.out.println(s);
	}
	
	@Test
	public void initialConditions() {
		println("initialConditions " + buff);
		assertThat(buff.position(), is(0));
		assertThat(buff.capacity(), is(10));
		assertThat(buff.limit(), is(10));
	}
	
	@Test
	public void writeSomeData() {
		buff.put((byte) 1);
		buff.put((byte) 2);
		println("writeSomeData " + buff);
		assertThat(buff.position(), is(2));
		assertThat(buff.limit(), is((10)));
	}
	
	@Test
	public void flippingABuffer() {
		buff.put((byte) 1);
		buff.put((byte) 2);
		buff.flip();
		println("flippingABuffer " + buff);
		assertThat(buff.position(), is(0));
		assertThat(buff.limit(), is((2)));
	}
	
	@Test
	public void compact() {
		buff.put((byte) 1);
		buff.put((byte) 2);
		buff.flip();
		byte read = buff.get();
		assertThat(read, is((byte)1));
		println("(pre)compact " + buff);
		buff.compact();
		println("compact " + buff);
		read = buff.get();
		assertThat(read, is((byte)2));
	}
	
	@Test
	public void compactAndFlip() {
		buff.put((byte) 1);
		buff.put((byte) 2);
		buff.flip();
		byte read = buff.get();
		assertThat(read, is((byte)1));
		println("(pre)compactAndFlip " + buff);
		buff.compact();
		println("(compact)compactAndFlip " + buff);
		buff.flip();
		println("compactAndFlip " + buff);
		read = buff.get();
		assertThat(read, is((byte)2));
	}
	
	@Test
	public void compactAndFlipEmptyBuffer() {
		assertThat(buff.remaining(), is(10));
		println("(initial)compactAndFlipEmptyBuffer " + buff);
		buff.compact();
		println("(compact)compactAndFlipEmptyBuffer " + buff);
		assertThat(buff.remaining(), is(0));
		buff.flip();
		println("(flip)compactAndFlipEmptyBuffer " + buff);
		assertThat(buff.remaining(), is(10));
	}
}
