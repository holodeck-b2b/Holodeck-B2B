/*
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.core.pmode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.UUID;

import org.holodeckb2b.common.pmode.Agreement;
import org.holodeckb2b.common.pmode.InMemoryPModeSet;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.core.config.InternalConfiguration;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.pmode.IPModeSetListener;
import org.holodeckb2b.interfaces.pmode.PModeSetEvent;
import org.holodeckb2b.interfaces.pmode.PModeSetException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.ArgumentCaptor;

/**
 * Created at 17:28 09.09.16
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class PModeManagerTest {

	private static InternalConfiguration config;
	private static ClassLoader dcl;
	private static ClassLoader ecl;


	@BeforeAll
	static void setUpClass() throws Exception {
		config = new InternalConfiguration(TestUtils.getTestClassBasePath());
		dcl = Thread.currentThread().getContextClassLoader();
		ecl = new URLClassLoader(new URL[] { TestUtils.getTestClassBasePath().toUri().toURL() }, dcl);
	}

	@BeforeEach
	void setupTest() {
		config.setAcceptNonValidablePMode(true);
		TestPModeStorage.failOnInit = false;
	}

	@AfterEach
	void resetClassLoader() {
		Thread.currentThread().setContextClassLoader(dcl);
	}

	@Test
	public void testDefaultConfig() {
		try {
			PModeManager manager = new PModeManager(config);

			Field pmodeStorage = PModeManager.class.getDeclaredField("deployedPModes");
			pmodeStorage.setAccessible(true);
			assertTrue(pmodeStorage.get(manager) instanceof InMemoryPModeSet);

		} catch (Throwable t) {
			t.printStackTrace();
			fail();
		}
	}

	@Test
	void testStorageLoading() {
		Thread.currentThread().setContextClassLoader(ecl);

		assertDoesNotThrow(() -> new PModeManager(config));

		assertTrue(TestPModeStorage.isLoaded());
	}

	@Test
	void testStorageLoadingFailure() {
		Thread.currentThread().setContextClassLoader(ecl);

		TestPModeStorage.failOnInit = true;
		assertThrows(PModeSetException.class, () -> new PModeManager(config));
	}

	@Test
	void testValidatorLoading() {
		Thread.currentThread().setContextClassLoader(ecl);

		assertDoesNotThrow(() -> new PModeManager(config));

		assertTrue(TestValidatorAllGood.isLoaded());
		assertTrue(TestValidatorRejectPull.isLoaded());
	}

	@Test
	void testInvalidConfig() {
		config.setAcceptNonValidablePMode(false);
		assertThrows(PModeSetException.class, () -> new PModeManager(config));
	}

	@Test
	void testBasicAdd() {
		PModeManager manager = assertDoesNotThrow(() -> new PModeManager(config));

		PMode pmode = new PMode();
		pmode.setId(UUID.randomUUID().toString());
		pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);

		assertDoesNotThrow(() -> manager.add(pmode));

		assertTrue(manager.containsId(pmode.getId()));
	}

	@Test
	void testRejectAdd() {
		Thread.currentThread().setContextClassLoader(ecl);
		PModeManager manager = assertDoesNotThrow(() -> new PModeManager(config));

		PMode pmode = new PMode();
		pmode.setId(UUID.randomUUID().toString());
		pmode.setMepBinding(EbMSConstants.ONE_WAY_PULL);

		assertThrows(PModeSetException.class, () -> manager.add(pmode));

		assertFalse(manager.containsId(pmode.getId()));
	}

	@Test
	void testBasicReplace() {
		PModeManager manager = assertDoesNotThrow(() -> new PModeManager(config));

		PMode pmode = new PMode();
		pmode.setId(UUID.randomUUID().toString());
		pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);

		assertDoesNotThrow(() -> manager.add(pmode));

		assertTrue(manager.containsId(pmode.getId()));

		PMode update = new PMode(pmode);
		update.setAgreement(new Agreement("newAgreement"));

		assertDoesNotThrow(() -> manager.replace(update));

		assertTrue(manager.containsId(update.getId()));
		assertEquals("newAgreement", manager.get(update.getId()).getAgreement().getName());
	}

	@Test
	void testRejectReplace() {
		Thread.currentThread().setContextClassLoader(ecl);
		PModeManager manager = assertDoesNotThrow(() -> new PModeManager(config));

		PMode pmode = new PMode();
		pmode.setId(UUID.randomUUID().toString());
		pmode.setMepBinding(EbMSConstants.ONE_WAY_PUSH);

		assertDoesNotThrow(() -> manager.add(pmode));

		assertTrue(manager.containsId(pmode.getId()));

		PMode update = new PMode(pmode);
		update.setMepBinding(EbMSConstants.ONE_WAY_PULL);

		assertThrows(PModeSetException.class, () -> manager.replace(update));

		assertTrue(manager.containsId(update.getId()));
		assertEquals(EbMSConstants.ONE_WAY_PUSH, manager.get(update.getId()).getMepBinding());
	}

	@Test
	void testBasicRemove() throws PModeSetException {
		PModeManager manager = assertDoesNotThrow(() -> new PModeManager(config));

		PMode pmode = new PMode();
		pmode.setId(UUID.randomUUID().toString());

		assertDoesNotThrow(() -> manager.add(pmode));
		assertTrue(manager.containsId(pmode.getId()));

		assertDoesNotThrow(() -> manager.remove(pmode.getId()));

		assertFalse(manager.containsId(pmode.getId()));
	}

	@Test
	void testAllEventsRegistration() {
		PModeManager manager = assertDoesNotThrow(() -> new PModeManager(config));

		IPModeSetListener listener = mock(IPModeSetListener.class);

		manager.registerEventListener(listener);

		ArgumentCaptor<PModeSetEvent> arg = ArgumentCaptor.forClass(PModeSetEvent.class);

		PMode pmode = new PMode();
		pmode.setId(UUID.randomUUID().toString());
		assertDoesNotThrow(() -> manager.add(pmode));

		verify(listener, times(1)).handleEvent(arg.capture());
		assertEquals(PModeSetEvent.PModeSetAction.ADD, arg.getValue().getEventType());

		pmode.setAgreement(new Agreement("newAgreement"));
		assertDoesNotThrow(() -> manager.replace(pmode));

		verify(listener, times(2)).handleEvent(arg.capture());
		assertEquals(PModeSetEvent.PModeSetAction.UPDATE, arg.getValue().getEventType());

		assertDoesNotThrow(() -> manager.remove(pmode.getId()));

		verify(listener, times(3)).handleEvent(arg.capture());
		assertEquals(PModeSetEvent.PModeSetAction.REMOVE, arg.getValue().getEventType());
	}

	@ParameterizedTest
	@EnumSource(mode = Mode.MATCH_ALL, value = PModeSetEvent.PModeSetAction.class)
	void testSingleEventRegistration(PModeSetEvent.PModeSetAction action) {
		PModeManager manager = assertDoesNotThrow(() -> new PModeManager(config));

		ArgumentCaptor<PModeSetEvent> arg = ArgumentCaptor.forClass(PModeSetEvent.class);
		IPModeSetListener listener = mock(IPModeSetListener.class);

		manager.registerEventListener(listener, action);

		PMode pmode = new PMode();
		pmode.setId(UUID.randomUUID().toString());
		assertDoesNotThrow(() -> manager.add(pmode));
		pmode.setAgreement(new Agreement("newAgreement"));
		assertDoesNotThrow(() -> manager.replace(pmode));
		assertDoesNotThrow(() -> manager.remove(pmode.getId()));

		verify(listener, atMostOnce()).handleEvent(arg.capture());
		assertEquals(action, arg.getValue().getEventType());
	}

	@Test
	void testRemoveListenerCompletely() {
		PModeManager manager = assertDoesNotThrow(() -> new PModeManager(config));

		IPModeSetListener listener = mock(IPModeSetListener.class);

		manager.registerEventListener(listener);

		manager.unregisterEventListener(listener);

		PMode pmode = new PMode();
		pmode.setId(UUID.randomUUID().toString());
		assertDoesNotThrow(() -> manager.add(pmode));
		pmode.setAgreement(new Agreement("newAgreement"));
		assertDoesNotThrow(() -> manager.replace(pmode));
		assertDoesNotThrow(() -> manager.remove(pmode.getId()));

		verify(listener, never()).handleEvent(any(PModeSetEvent.class));
	}

	@ParameterizedTest
	@EnumSource(mode = Mode.MATCH_ALL, value = PModeSetEvent.PModeSetAction.class)
	void testRemoveListenerPartially(PModeSetEvent.PModeSetAction action) {
		PModeManager manager = assertDoesNotThrow(() -> new PModeManager(config));

		IPModeSetListener listener = mock(IPModeSetListener.class);

		manager.registerEventListener(listener);

		manager.unregisterEventListener(listener, action);

		PMode pmode = new PMode();
		pmode.setId(UUID.randomUUID().toString());
		assertDoesNotThrow(() -> manager.add(pmode));
		pmode.setAgreement(new Agreement("newAgreement"));
		assertDoesNotThrow(() -> manager.replace(pmode));
		assertDoesNotThrow(() -> manager.remove(pmode.getId()));

		verify(listener, atMost(2)).handleEvent(any(PModeSetEvent.class));
		verify(listener, never()).handleEvent(argThat(ev -> ev.getEventType() == action));
	}
}