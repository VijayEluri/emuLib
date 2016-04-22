/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2010-2016, Peter Jakubčo
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

package emulib.plugins.memory;

import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.PluginInitializationException;

import java.util.Objects;

/**
 * This class implements some fundamental functionality that can be shared
 * by most memory implementations.
 */
public abstract class AbstractMemory implements Memory {
    /**
     * Start location of loaded program. This variable is changed by
     * compiler (mostly).
     */
    protected int programStart;

    /**
     * ID of the plug-in assigned by emuStudio
     */
    protected long pluginID;

    /**
     * Settings manipulation object
     */
    protected SettingsManager settings;

    /**
     * Sets up plug-in id.
     *
     * @param pluginID plug-in identification number
     * @throws NullPointerException if pluginID is null
     */
    public AbstractMemory(Long pluginID) {
        this.pluginID = Objects.requireNonNull(pluginID);
    }

    /**
     * This method does a semi-initialization of the memory. It loads
     * variables: pluginID and SettingsManager object.
     *
     * It is called by emuStudio in the initialization process. Should
     * be overriden.
     *
     * @param settings settings manipulation object
     * @throws NullPointerException if settings is null
     */
    @Override
    public void initialize(SettingsManager settings)  throws PluginInitializationException {
        this.settings = Objects.requireNonNull(settings);
    }

    /**
     * Get program starting address (memory location), as it was loaded by
     * the method setProgramStart().
     *
     * @return program starting address (memory location)
     */
    @Override
    public int getProgramStart() {
    	return programStart;
    }

    /**
     * Set program starting address (memory location). Mostly it is called
     * by the emuStudio after the compiler returns. When the compiler
     * compiles the source, the emuStudio gets compiled program starting
     * address (if unknown, it will be 0) and pass it here.
     *
     * @param address program starting address (memory location)
     */
    @Override
    public void setProgramStart(int address) {
        programStart = address;
    }

    @Override
    public String getTitle() {
        return getClass().getAnnotation(PluginType.class).title();
    }

    /**
     * Does nothing.
     */
    @Override
    public void reset() {

    }

}
