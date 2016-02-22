/**
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
package org.holodeckb2b.common.util;

/**
 * Is a simple and write once read many key value pair.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @param <K> Key type
 * @param <V> Value type
 */
public class KeyValuePair<K, V> {
    
    private K   key;
    private V   value;
    
    /**
     * Creates a new <code>KeyValuePair</code>
     * 
     * @param key       An object of class K that is the key of the new pair
     * @param value     An object of class V that is the value of the new pair
     */
    public KeyValuePair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Gets the key of this key value pair
     * 
     * @return  The key of this pair
     */
    public K getKey() {
        return key;
    }
    
    /**
     * Gets the value of this key value pair
     * 
     * @return The value of this pair
     */
    public V getValue() {
        return value;
    }
}
