/*
 * ContextPoolTest.java
 *
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emulib.runtime;

import emulib.annotations.ContextType;
import emulib.emustudio.APITest;
import emulib.plugins.Context;
import emulib.plugins.compiler.CompilerContext;
import emulib.plugins.cpu.CPUContext;
import emulib.plugins.device.DeviceContext;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.exceptions.AlreadyRegisteredException;
import emulib.runtime.exceptions.ContextNotFoundException;
import emulib.runtime.exceptions.InvalidContextException;
import emulib.runtime.exceptions.InvalidPasswordException;
import emulib.runtime.interfaces.PluginConnections;
import emulib.runtime.stubs.CPUContextStub;
import emulib.runtime.stubs.CompilerContextStub;
import emulib.runtime.stubs.DeviceContextStub;
import emulib.runtime.stubs.MemoryContextStub;
import emulib.runtime.stubs.UnannotatedContextStub;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ContextPoolTest {
    private CPUContextStub cpuContextMock;
    private MemoryContextStub memContextMock;
    private CompilerContextStub compilerContextMock;
    private DeviceContextStub devContextMock;
    private ContextPool contextPool;
    private final PluginConnections defaultComputer = new ComputerStub(true);

    private class ComputerStub implements PluginConnections {
        private final boolean connected;

        ComputerStub(boolean connected) {
            this.connected = connected;
        }

        @Override
        public boolean isConnected(long pluginID, long toPluginID) {
            return pluginID != toPluginID && connected;
        }
    }

    @ContextType
    private interface DifferentCPUContextStubWithEqualHash extends CPUContextStub {

    }

    @ContextType
    private interface DifferentCompilerContextStubWithEqualHash extends CompilerContextStub {

    }

    @ContextType
    private interface DifferentMemoryContextStubWithEqualHash extends MemoryContextStub {

    }

    @ContextType
    private interface DifferentDeviceContextStubWithEqualHash extends DeviceContextStub {

    }

    @ContextType
    private interface FirstEmptyContextStub extends Context {

    }

    @ContextType
    private interface SecondEmptyContextStub extends Context {

    }

    @Before
    public void setUp() throws InvalidPasswordException {
        cpuContextMock = EasyMock.createNiceMock(CPUContextStub.class);
        memContextMock = EasyMock.createNiceMock(MemoryContextStub.class);
        compilerContextMock = EasyMock.createNiceMock(CompilerContextStub.class);
        devContextMock = EasyMock.createNiceMock(DeviceContextStub.class);
        replay(cpuContextMock, memContextMock, compilerContextMock, devContextMock);

        contextPool = new ContextPool();
        APITest.assignEmuStudioPassword();
        assertTrue(contextPool.setComputer(APITest.getEmuStudioPassword(), defaultComputer));
    }

    @After
    public void tearDown() throws InvalidPasswordException {
        contextPool.clearAll(APITest.getEmuStudioPassword());
        verify(cpuContextMock, memContextMock, compilerContextMock, devContextMock);
        contextPool = null;
    }

    @Test
    public void testRegisterCPU() throws Exception {
        contextPool.register(0, cpuContextMock, CPUContext.class);
        assertEquals(cpuContextMock, contextPool.getCPUContext(1, CPUContext.class));
        assertTrue(contextPool.unregister(0, CPUContext.class));
    }

    @Test(expected = ContextNotFoundException.class)
    public void testRegisterTwoTimesCPUAndAccessThemByIndex() throws Exception {
        CPUContextStub anotherCpuContextMock = EasyMock.createNiceMock(CPUContextStub.class);
        replay(anotherCpuContextMock);
        contextPool.register(0, cpuContextMock, CPUContext.class);
        contextPool.register(0, anotherCpuContextMock, CPUContext.class);

        // Access both contexts
        assertEquals(cpuContextMock, contextPool.getCPUContext(0, CPUContext.class, 0));
        assertEquals(anotherCpuContextMock, contextPool.getCPUContext(0, CPUContext.class, 1));

        // single unregister should remove all contexts for the owner
        assertTrue(contextPool.unregister(0, CPUContext.class));

        contextPool.getCPUContext(0, CPUContext.class);
    }

    @Test
    public void testRegisterCPUAccessibleByTwoInterfacesWithEqualHash() throws Exception {
        contextPool.register(0, cpuContextMock, CPUContextStub.class);
        assertEquals(cpuContextMock, contextPool.getCPUContext(1, CPUContextStub.class));
        assertEquals(cpuContextMock, contextPool.getCPUContext(1, DifferentCPUContextStubWithEqualHash.class));
        assertTrue(contextPool.unregister(0, CPUContextStub.class));
    }

    @Test
    public void testUnregisterCanOnlyOwner() throws Exception {
        contextPool.register(0, cpuContextMock, CPUContextStub.class);
        assertFalse(contextPool.unregister(1, CPUContextStub.class));
    }

    @Test(expected = ContextNotFoundException.class)
    public void testContextCannotBeRetrievedFromOwnerIfItIsNotRecursivelyConnected() throws Exception {
        contextPool.register(1, compilerContextMock, CompilerContext.class);
        contextPool.getCompilerContext(1, CompilerContext.class);
    }

    @Test
    public void testRegisterCompiler() throws Exception {
        contextPool.register(1, compilerContextMock, CompilerContext.class);
        assertEquals(compilerContextMock, contextPool.getCompilerContext(2, CompilerContext.class));
        assertTrue(contextPool.unregister(1, CompilerContext.class));
    }

    @Test
    public void testRegisterCompilerAccessibleByTwoInterfacesWithEqualHash() throws Exception {
        contextPool.register(1, compilerContextMock, CompilerContextStub.class);
        assertEquals(compilerContextMock, contextPool.getCompilerContext(2, CompilerContextStub.class));
        assertEquals(compilerContextMock, contextPool.getCompilerContext(2, DifferentCompilerContextStubWithEqualHash.class));
        assertTrue(contextPool.unregister(1, CompilerContextStub.class));
    }

    @Test(expected = ContextNotFoundException.class)
    public void testRegisterTwoTimesCompilerAndAccessThemByIndex() throws Exception {
        CompilerContextStub anotherCompilerContextMock = EasyMock.createNiceMock(CompilerContextStub.class);
        replay(anotherCompilerContextMock);
        contextPool.register(1, compilerContextMock, CompilerContext.class);
        contextPool.register(1, anotherCompilerContextMock, CompilerContext.class);

        // Access both contexts
        assertEquals(compilerContextMock, contextPool.getCompilerContext(1, CompilerContext.class, 0));
        assertEquals(anotherCompilerContextMock, contextPool.getCompilerContext(1, CompilerContext.class, 1));

        // single unregister should remove all contexts for the owner
        assertTrue(contextPool.unregister(1, CompilerContext.class));

        contextPool.getCompilerContext(1, CompilerContext.class);
    }

    @Test
    public void testRegisterMemory() throws Exception {
        contextPool.register(2, memContextMock, MemoryContext.class);
        assertEquals(memContextMock, contextPool.getMemoryContext(3, MemoryContext.class));
        assertTrue(contextPool.unregister(2, MemoryContext.class));
    }

    @Test
    public void testRegisterMemoryAccessibleByTwoInterfacesWithEqualHash() throws Exception {
        contextPool.register(2, memContextMock, MemoryContextStub.class);
        assertEquals(memContextMock, contextPool.getMemoryContext(3, MemoryContextStub.class));
        assertEquals(memContextMock, contextPool.getMemoryContext(3, DifferentMemoryContextStubWithEqualHash.class));
        assertTrue(contextPool.unregister(2, MemoryContextStub.class));
    }

    @Test(expected = ContextNotFoundException.class)
    public void testRegisterTwoTimesMemoryAndAccessThemByIndex() throws Exception {
        MemoryContextStub anotherMemoryContextMock = EasyMock.createNiceMock(MemoryContextStub.class);
        replay(anotherMemoryContextMock);
        contextPool.register(2, memContextMock, MemoryContext.class);
        contextPool.register(2, anotherMemoryContextMock, MemoryContext.class);

        // Access both contexts
        assertEquals(memContextMock, contextPool.getMemoryContext(2, MemoryContext.class, 0));
        assertEquals(anotherMemoryContextMock, contextPool.getMemoryContext(2, MemoryContext.class, 1));

        // single unregister should remove all contexts for the owner
        assertTrue(contextPool.unregister(2, MemoryContext.class));

        contextPool.getMemoryContext(2, MemoryContext.class);
    }

    @Test
    public void testRegisterDevice() throws Exception {
        contextPool.register(3, devContextMock, DeviceContext.class);
        assertEquals(devContextMock, contextPool.getDeviceContext(4, DeviceContext.class));
        assertTrue(contextPool.unregister(3, DeviceContext.class));
    }

    @Test
    public void testRegisterDeviceAccessibleByTwoInterfacesWithEqualHash() throws Exception {
        contextPool.register(3, devContextMock, DeviceContextStub.class);
        assertEquals(devContextMock, contextPool.getDeviceContext(4, DeviceContextStub.class));
        assertEquals(devContextMock, contextPool.getDeviceContext(4, DifferentDeviceContextStubWithEqualHash.class));
        assertTrue(contextPool.unregister(3, DeviceContextStub.class));
    }

    @Test
    public void testRegisterTwoContextsWithEqualInterfaces() throws Exception {
        FirstEmptyContextStub firstEmpty = EasyMock.createNiceMock(FirstEmptyContextStub.class);
        SecondEmptyContextStub secondEmpty = EasyMock.createNiceMock(SecondEmptyContextStub.class);
        replay(firstEmpty, secondEmpty);

        contextPool.register(0, firstEmpty, FirstEmptyContextStub.class);
        contextPool.register(0, secondEmpty, SecondEmptyContextStub.class);
    }

    @Test(expected = ContextNotFoundException.class)
    public void testRegisterTwoTimesDeviceAndAccessThemByIndex() throws Exception {
        DeviceContextStub anotherDeviceContextMock = EasyMock.createNiceMock(DeviceContextStub.class);
        replay(anotherDeviceContextMock);
        contextPool.register(3, devContextMock, DeviceContext.class);
        contextPool.register(3, anotherDeviceContextMock, DeviceContext.class);

        // Access both contexts
        assertEquals(devContextMock, contextPool.getDeviceContext(3, DeviceContext.class, 0));
        assertEquals(anotherDeviceContextMock, contextPool.getDeviceContext(3, DeviceContext.class, 1));

        // single unregister should remove all contexts for the owner
        assertTrue(contextPool.unregister(3, DeviceContext.class));

        contextPool.getDeviceContext(3, DeviceContext.class);
    }

    @Test(expected = InvalidContextException.class)
    public void testUnexpectedContextInterface() throws Exception {
        contextPool.register(1, memContextMock, CPUContext.class);
    }

    @Test(expected = InvalidContextException.class)
    public void testUnannotatedContextInterface() throws Exception {
        Context unannotatedContext = EasyMock.createNiceMock(UnannotatedContextStub.class);
        contextPool.register(0, unannotatedContext, UnannotatedContextStub.class);
    }

    @Test(expected = AlreadyRegisteredException.class)
    public void testCPUContextAlreadyRegisteredDifferentOwner() throws Exception {
        contextPool.register(0, cpuContextMock, CPUContext.class);
        contextPool.register(1, cpuContextMock, CPUContext.class);
    }

    @Test(expected = AlreadyRegisteredException.class)
    public void testCPUContextAlreadyRegisteredSameOwner() throws Exception {
        contextPool.register(0, cpuContextMock, CPUContext.class);
        contextPool.register(0, cpuContextMock, CPUContext.class);
    }

    @Test(expected = AlreadyRegisteredException.class)
    public void testMemoryContextAlreadyRegisteredDifferentOwner() throws Exception {
        contextPool.register(2, memContextMock, MemoryContext.class);
        contextPool.register(3, memContextMock, MemoryContext.class);
    }

    @Test(expected = AlreadyRegisteredException.class)
    public void testMemoryContextAlreadyRegisteredSameOwner() throws Exception {
        contextPool.register(2, memContextMock, MemoryContext.class);
        contextPool.register(2, memContextMock, MemoryContext.class);
    }

    @Test(expected = AlreadyRegisteredException.class)
    public void testCompilerContextAlreadyRegisteredDifferentOwner() throws Exception {
        contextPool.register(1, compilerContextMock, CompilerContext.class);
        contextPool.register(2, compilerContextMock, CompilerContext.class);
    }

    @Test(expected = AlreadyRegisteredException.class)
    public void testCompilerContextAlreadyRegisteredSameOwner() throws Exception {
        contextPool.register(1, compilerContextMock, CompilerContext.class);
        contextPool.register(1, compilerContextMock, CompilerContext.class);
    }

    @Test(expected = AlreadyRegisteredException.class)
    public void testDeviceContextAlreadyRegisteredDifferentOwner() throws Exception {
        contextPool.register(3, devContextMock, DeviceContext.class);
        contextPool.register(2, devContextMock, DeviceContext.class);
    }

    @Test(expected = AlreadyRegisteredException.class)
    public void testDeviceContextAlreadyRegisteredSameOwner() throws Exception {
        contextPool.register(3, devContextMock, DeviceContext.class);
        contextPool.register(3, devContextMock, DeviceContext.class);
    }

    @Test
    public void testCallUnregisterWithoutRegister() throws Exception {
        assertFalse(contextPool.unregister(0, CPUContextStub.class));
    }

    @Test(expected = ContextNotFoundException.class)
    public void testGetContextWhenNoComputerIsSet() throws Exception {
        assertTrue(contextPool.setComputer(APITest.getEmuStudioPassword(), null));

        contextPool.getCPUContext(0, CPUContextStub.class);
    }

    @Test(expected = InvalidContextException.class)
    public void testGetNullCPUContext() throws Exception {
        contextPool.getCPUContext(0, null);
    }

    @Test(expected = InvalidContextException.class)
    public void testGetNullCompilerContext() throws Exception {
        contextPool.getCompilerContext(1, null);
    }

    @Test(expected = InvalidContextException.class)
    public void testGetNullMemoryContext() throws Exception {
        contextPool.getMemoryContext(2, null);
    }

    @Test(expected = InvalidContextException.class)
    public void testGetNullDeviceContext() throws Exception {
        contextPool.getDeviceContext(3, null);
    }

    @Test(expected = InvalidContextException.class)
    public void testGetCPUContextWhichIsNotInterface() throws Exception {
        contextPool.getCPUContext(0, cpuContextMock.getClass());
    }

    @Test(expected = InvalidContextException.class)
    public void testGetCompilerContextWhichIsNotInterface() throws Exception {
        contextPool.getCompilerContext(0, compilerContextMock.getClass());
    }

    @Test(expected = InvalidContextException.class)
    public void testGetMemoryContextWhichIsNotInterface() throws Exception {
        contextPool.getMemoryContext(0, memContextMock.getClass());
    }

    @Test(expected = InvalidContextException.class)
    public void testGetDeviceContextWhichIsNotInterface() throws Exception {
        contextPool.getDeviceContext(0, devContextMock.getClass());
    }

    @Test(expected = ContextNotFoundException.class)
    public void testComputerIsNotSetGetCPU() throws Exception {
        assertTrue(contextPool.setComputer(APITest.getEmuStudioPassword(), null));
        contextPool.register(0, cpuContextMock, CPUContext.class);

        contextPool.getCPUContext(0, CPUContext.class);
    }

    @Test
    public void testUnregisterInvalidContext() throws Exception {
        contextPool.register(0, cpuContextMock, CPUContext.class);
        try {
            assertEquals(cpuContextMock, contextPool.getCPUContext(1, CPUContext.class));
            assertFalse(contextPool.unregister(0, MemoryContext.class));
        } finally {
            assertTrue(contextPool.unregister(0, CPUContext.class));
        }
    }

    @Test
    public void testGetContextWithEmustudioPassword() throws Exception {
        assertTrue(
                contextPool.setComputer(
                        APITest.getEmuStudioPassword(),
                        new ComputerStub(false)
                )
        );
        contextPool.register(0, cpuContextMock, CPUContext.class);
        assertNotNull(
                contextPool.getCPUContext(
                        APITest.getEmuStudioPassword().hashCode(),
                        CPUContext.class
                )
        );
    }

}
