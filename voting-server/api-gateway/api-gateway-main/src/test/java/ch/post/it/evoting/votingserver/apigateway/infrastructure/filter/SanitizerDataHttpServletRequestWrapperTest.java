package ch.post.it.evoting.votingserver.apigateway.infrastructure.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import org.junit.Test;

public class SanitizerDataHttpServletRequestWrapperTest {

	@Test
	public void checkSanitizedCSV() {
		try {
			SanitizerDataHttpServletRequestWrapper.checkIfCSVIsSanitized("a ,b ,c,d,e,f,g,h,i");
			SanitizerDataHttpServletRequestWrapper.checkIfCSVIsSanitized("a;b;c;d ; e;f");
			SanitizerDataHttpServletRequestWrapper.checkIfCSVIsSanitized("baba;[\"{\\\"publicKey\\\":{\\\"zpSubgroup\\\":{\\\"g\\\":\\\"Aw==\\\"");
		} catch (Exception e) {
			fail("Error occurred");
		}
	}

	@Test
	public void checkNotSanitizedCSV() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> SanitizerDataHttpServletRequestWrapper.checkIfCSVIsSanitized("a, <script src='toto.js> , c"));
		assertEquals("Data is not valid at CSV element", exception.getMessage());
	}
}
