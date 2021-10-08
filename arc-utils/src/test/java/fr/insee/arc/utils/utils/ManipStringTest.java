package fr.insee.arc.utils.utils;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

public class ManipStringTest {

	@Test
	public void isStringNull() {
		// yes null
		assertEquals(true, ManipString.isStringNull(null));
		// yes empty
		assertEquals(true, ManipString.isStringNull(""));
		// false not empty
		assertEquals(false, ManipString.isStringNull(" "));
	}

	@Test
	public void substringBeforeFirstNullInput() {
		// null input string -> return null
		assertEquals(null, ManipString.substringBeforeFirst(null, "-"));
	}

	@Test
	public void substringBeforeFirstNullSep() {
	// null separator -> return all string
	assertEquals("abc", ManipString.substringBeforeFirst("abc", null));
	}
	
	@Test
	public void substringBeforeFirstSepNotFound() {
	// separator not found -> return all string
	assertEquals("abc", ManipString.substringBeforeFirst("abc", "-"));
	}
	
	@Test
	public void substringBeforeFirstNormal() {
		// standard use case
		assertEquals("az", ManipString.substringBeforeFirst("az-b-c", "-"));
	}
	

	// substringBeforeLast
	@Test
	public void substringBeforeLast() {
		// null input string -> return null
		assertEquals(null, ManipString.substringBeforeLast(null, "-"));
		// null separator -> return all string
		assertEquals("abc", ManipString.substringBeforeLast("abc", null));
		// separator not found -> return all string
		assertEquals("abc", ManipString.substringBeforeLast("abc", "-"));
		// standard use case
		assertEquals("a-b", ManipString.substringBeforeLast("a-b-c", "-"));
	}

	// substringAfterFirst
	@Test
	public void substringAfterFirst() {
		assertEquals(null, ManipString.substringAfterFirst(null, "-"));
		assertEquals("abc", ManipString.substringAfterFirst("abc", null));
		assertEquals("abc", ManipString.substringAfterFirst("abc", "-"));
		assertEquals("b-c", ManipString.substringAfterFirst("a-b-c", "-"));
	}


	// substringAfterLast
	@Test
	public void substringAfterLastNullString() {
		assertEquals(null, ManipString.substringAfterLast(null, "-"));
		assertEquals("abc", ManipString.substringAfterLast("abc", null));
		assertEquals("abc", ManipString.substringAfterLast("abc", "-"));
		assertEquals("c", ManipString.substringAfterLast("a-b-c", "-"));
	}


	@Test
	public void compress() {
		String testValue1="arêtes de poÿss▒n";
		assertEquals(testValue1,ManipString.decompress(ManipString.compress(testValue1)));
	}
	
	@Test
	public void parseInteger() {
		assertEquals((Integer)null,ManipString.parseInteger("1 am not a number"));
		// positive number with + sign
		assertEquals((Integer)10,ManipString.parseInteger("+10"));
		// decimal number returns null
		assertEquals(null,ManipString.parseInteger("10.8"));
	}

	@Test
	public void redoEntryName() {
		// rename an archive entry to a temporary name
		assertEquals("a.tar.gz§depot§c.xml",ManipString.redoEntryName("a.tar.gz/depot\\c.xml"));
	}
	
	
	@Test
    public void stringToListSeparator() {
		assertEquals(Arrays.asList("a","b","cde"),ManipString.stringToList("a,b,cde",","));
    }
	
	@Test(expected = NullPointerException.class)
	 public void stringToListSeparatorNull() {
		ManipString.stringToList(null,",");
    }
	
	@Test
    public void stringToListVariadic() {
		assertEquals(Arrays.asList("a","b","cde"),ManipString.stringToList("a","b","cde"));
    }
	
}
