/*
 * Copyright (C) 2009, Google Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.eclipse.jgit.dircache;

import static java.time.Instant.EPOCH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Test;

public class DirCacheEntryTest {
	@Test
	public void testIsValidPath() {
		assertTrue(isValidPath("a"));
		assertTrue(isValidPath("a/b"));
		assertTrue(isValidPath("ab/cd/ef"));

		assertFalse(isValidPath(""));
		assertFalse(isValidPath("/a"));
		assertFalse(isValidPath("a//b"));
		assertFalse(isValidPath("ab/cd//ef"));
		assertFalse(isValidPath("a/"));
		assertFalse(isValidPath("ab/cd/ef/"));
		assertFalse(isValidPath("a\u0000b"));
	}

	@SuppressWarnings("unused")
	private static boolean isValidPath(String path) {
		try {
			new DirCacheEntry(path);
			return true;
		} catch (InvalidPathException e) {
			return false;
		}
	}

	@SuppressWarnings("unused")
	@Test
	public void testCreate_ByStringPath() {
		assertEquals("a", new DirCacheEntry("a").getPathString());
		assertEquals("a/b", new DirCacheEntry("a/b").getPathString());

		try {
			new DirCacheEntry("/a");
			fail("Incorrectly created DirCacheEntry");
		} catch (IllegalArgumentException err) {
			assertEquals("Invalid path: /a", err.getMessage());
		}
	}

	@SuppressWarnings("unused")
	@Test
	public void testCreate_ByStringPathAndStage() {
		DirCacheEntry e;

		e = new DirCacheEntry("a", 0);
		assertEquals("a", e.getPathString());
		assertEquals(0, e.getStage());

		e = new DirCacheEntry("a/b", 1);
		assertEquals("a/b", e.getPathString());
		assertEquals(1, e.getStage());

		e = new DirCacheEntry("a/c", 2);
		assertEquals("a/c", e.getPathString());
		assertEquals(2, e.getStage());

		e = new DirCacheEntry("a/d", 3);
		assertEquals("a/d", e.getPathString());
		assertEquals(3, e.getStage());

		try {
			new DirCacheEntry("/a", 1);
			fail("Incorrectly created DirCacheEntry");
		} catch (IllegalArgumentException err) {
			assertEquals("Invalid path: /a", err.getMessage());
		}

		try {
			new DirCacheEntry("a", -11);
			fail("Incorrectly created DirCacheEntry");
		} catch (IllegalArgumentException err) {
			assertEquals("Invalid stage -11 for path a", err.getMessage());
		}

		try {
			new DirCacheEntry("a", 4);
			fail("Incorrectly created DirCacheEntry");
		} catch (IllegalArgumentException err) {
			assertEquals("Invalid stage 4 for path a", err.getMessage());
		}
	}

	@Test
	public void testSetFileMode() {
		final DirCacheEntry e = new DirCacheEntry("a");

		assertEquals(0, e.getRawMode());

		e.setFileMode(FileMode.REGULAR_FILE);
		assertSame(FileMode.REGULAR_FILE, e.getFileMode());
		assertEquals(FileMode.REGULAR_FILE.getBits(), e.getRawMode());

		e.setFileMode(FileMode.EXECUTABLE_FILE);
		assertSame(FileMode.EXECUTABLE_FILE, e.getFileMode());
		assertEquals(FileMode.EXECUTABLE_FILE.getBits(), e.getRawMode());

		e.setFileMode(FileMode.SYMLINK);
		assertSame(FileMode.SYMLINK, e.getFileMode());
		assertEquals(FileMode.SYMLINK.getBits(), e.getRawMode());

		e.setFileMode(FileMode.GITLINK);
		assertSame(FileMode.GITLINK, e.getFileMode());
		assertEquals(FileMode.GITLINK.getBits(), e.getRawMode());

		try {
			e.setFileMode(FileMode.MISSING);
			fail("incorrectly accepted FileMode.MISSING");
		} catch (IllegalArgumentException err) {
			assertEquals("Invalid mode 0 for path a", err.getMessage());
		}

		try {
			e.setFileMode(FileMode.TREE);
			fail("incorrectly accepted FileMode.TREE");
		} catch (IllegalArgumentException err) {
			assertEquals("Invalid mode 40000 for path a", err.getMessage());
		}
	}

	@Test
	public void testCopyMetaDataWithStage() {
		copyMetaDataHelper(false);
	}

	@Test
	public void testCopyMetaDataWithoutStage() {
		copyMetaDataHelper(true);
	}

	private static void copyMetaDataHelper(boolean keepStage) {
		DirCacheEntry e = new DirCacheEntry("some/path", DirCacheEntry.STAGE_2, FileMode.EXECUTABLE_FILE);
		e.setAssumeValid(false);
		e.setCreationTime(2L);
		e.setLastModified(EPOCH.plusMillis(3L));
		e.setLength(100L);
		e.setObjectId(ObjectId
				.fromString("0123456789012345678901234567890123456789"));
		e.setUpdateNeeded(true);

		DirCacheEntry f = new DirCacheEntry("someother/path",
				DirCacheEntry.STAGE_1, FileMode.SYMLINK);
		f.setAssumeValid(true);
		f.setCreationTime(10L);
		f.setLastModified(EPOCH.plusMillis(20L));
		f.setLength(100000000L);
		f.setObjectId(ObjectId
				.fromString("1234567890123456789012345678901234567890"));
		f.setUpdateNeeded(true);

		e.copyMetaData(f, keepStage);
		assertTrue(e.isAssumeValid());
		assertEquals(10L, e.getCreationTime());
		assertEquals(
				ObjectId.fromString("1234567890123456789012345678901234567890"),
				e.getObjectId());
		assertEquals(FileMode.SYMLINK, e.getFileMode());
		assertEquals(EPOCH.plusMillis(20L), e.getLastModifiedInstant());
		assertEquals(100000000L, e.getLength());
		if (keepStage)
			assertEquals(DirCacheEntry.STAGE_2, e.getStage());
		else
			assertEquals(DirCacheEntry.STAGE_1, e.getStage());
		assertTrue(e.isUpdateNeeded());
		assertEquals("some/path", e.getPathString());
	}
}
